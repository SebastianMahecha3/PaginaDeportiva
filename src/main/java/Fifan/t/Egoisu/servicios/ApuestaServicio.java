package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.config.ConfiguracionApuestas;
import Fifan.t.Egoisu.dto.LineaBoletoDto;
import Fifan.t.Egoisu.dto.ResumenBoletoDto;
import Fifan.t.Egoisu.dto.SeleccionDto;
import Fifan.t.Egoisu.entidades.*;
import Fifan.t.Egoisu.entidades.enums.*;
import Fifan.t.Egoisu.exception.InvalidBetException;
import Fifan.t.Egoisu.exception.ResourceNotFoundException;
import Fifan.t.Egoisu.exception.UnauthorizedActionException;
import Fifan.t.Egoisu.repositorios.ApuestaRepositorio;
import Fifan.t.Egoisu.repositorios.JugadorRepositorio;
import Fifan.t.Egoisu.repositorios.PartidoRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo de apuestas: usuario -> selección -> cuota actual (pedida a ServicioCuotas) -> crear apuesta -> congelar cuota.
 * Reglas: nada del cliente se acepta como cuota, ganancia, estado o rol; todo se valida y recalcula aquí.
 * La liquidación posterior la hace LiquidacionServicio.
 */
@Service
@RequiredArgsConstructor
public class ApuestaServicio {

    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("9999999999.99");

    private final ApuestaRepositorio apuestaRepositorio;
    private final PartidoRepositorio partidoRepositorio;
    private final JugadorRepositorio jugadorRepositorio;
    private final UsuarioServicio usuarioServicio;
    private final ServicioCuotas servicioCuotas;
    private final CatalogoMercadosServicio catalogo;
    private final CalculadoraApuesta calculadora;
    private final ConfiguracionApuestas config;

    /** Selección ya validada por el servidor, con su cuota vigente. */
    @Getter @AllArgsConstructor
    static class SeleccionResuelta {
        private final Partido partido;
        private final Jugador jugador;
        private final SeleccionDto seleccion;
        private final BigDecimal cuota;
        private final String descripcion;
    }

    // ---------- boleto ----------

    /** Valida una selección nueva contra el boleto actual y la devuelve normalizada (sin campos que no aplican). */
    @Transactional(readOnly = true)
    public SeleccionDto validarNuevaSeleccion(List<SeleccionDto> actuales, SeleccionDto nueva) {
        SeleccionResuelta resuelta = resolver(nueva);
        if (actuales.size() >= config.getParleyMaximoSelecciones()) {
            throw new InvalidBetException("El boleto admite como máximo " + config.getParleyMaximoSelecciones() + " selecciones.");
        }
        for (SeleccionDto existente : actuales) {
            if (Objects.equals(existente.getPartidoId(), resuelta.getSeleccion().getPartidoId())) {
                throw new InvalidBetException("Ya tienes una selección de este partido en el boleto. "
                        + "Quítala si quieres elegir otra (un parley admite una selección por partido).");
            }
        }
        return resuelta.getSeleccion();
    }

    /** Calcula el resumen del boleto (y, si se indica monto, comisión y ganancia potencial). No guarda nada. */
    @Transactional(readOnly = true)
    public ResumenBoletoDto resumir(List<SeleccionDto> selecciones, BigDecimal monto) {
        List<LineaBoletoDto> lineas = new ArrayList<>();
        List<BigDecimal> cuotas = new ArrayList<>();
        boolean valido = !selecciones.isEmpty();
        for (SeleccionDto s : selecciones) {
            try {
                SeleccionResuelta r = resolver(s);
                lineas.add(new LineaBoletoDto(s, r.getPartido().getDescripcion(), r.getSeleccion().getMercado().getEtiqueta(),
                        r.getDescripcion(), r.getCuota(), null));
                cuotas.add(r.getCuota());
            } catch (InvalidBetException e) {
                valido = false;
                lineas.add(new LineaBoletoDto(s, "Partido #" + s.getPartidoId(),
                        s.getMercado() == null ? "-" : s.getMercado().getEtiqueta(), "-", null, e.getMessage()));
            }
        }
        if (!valido) {
            return new ResumenBoletoDto(lineas, false, null, null, null, null, null, null, null, null);
        }
        TipoApuesta tipo = cuotas.size() == 1 ? TipoApuesta.SIMPLE : TipoApuesta.PARLEY;
        BigDecimal base = calculadora.cuotaCombinada(cuotas);
        BigDecimal bonus = calculadora.bonusParley(cuotas.size());
        BigDecimal total = calculadora.aplicarBonus(base, bonus);
        if (monto != null && monto.signum() > 0) {
            return new ResumenBoletoDto(lineas, true, tipo, base, bonus, total, monto,
                    calculadora.comision(monto), calculadora.montoNeto(monto), calculadora.gananciaPotencial(monto, total));
        }
        return new ResumenBoletoDto(lineas, true, tipo, base, bonus, total, null, null, null, null);
    }

    // ---------- confirmación ----------

    /**
     * Crea la apuesta. Recalcula TODO en el servidor y guarda un snapshot (cuotas, textos, monto, comisión).
     * cuotaMostrada (opcional) solo sirve para avisar si la cuota cambió entre la vista previa y la confirmación.
     */
    @Transactional
    public Apuesta confirmar(String username, List<SeleccionDto> selecciones, BigDecimal monto, BigDecimal cuotaMostrada) {
        Usuario usuario = usuarioServicio.obtenerPorUsername(username);
        if (usuario.getRol() == Rol.ROLE_ADMIN) {
            throw new InvalidBetException("El administrador no puede realizar apuestas.");
        }
        BigDecimal montoValido = validarMonto(monto);
        if (selecciones == null || selecciones.isEmpty()) {
            throw new InvalidBetException("Tu boleto está vacío: elige al menos una selección.");
        }
        if (selecciones.size() > config.getParleyMaximoSelecciones()) {
            throw new InvalidBetException("El boleto admite como máximo " + config.getParleyMaximoSelecciones() + " selecciones.");
        }
        List<SeleccionResuelta> resueltas = new ArrayList<>();
        Set<Long> partidos = new HashSet<>();
        for (SeleccionDto s : selecciones) {
            SeleccionResuelta r = resolver(s);
            if (!partidos.add(r.getPartido().getId())) {
                throw new InvalidBetException("Un parley admite una sola selección por partido.");
            }
            resueltas.add(r);
        }
        List<BigDecimal> cuotas = resueltas.stream().map(SeleccionResuelta::getCuota).toList();
        BigDecimal base = calculadora.cuotaCombinada(cuotas);
        BigDecimal bonus = calculadora.bonusParley(cuotas.size());
        BigDecimal total = calculadora.aplicarBonus(base, bonus);
        if (cuotaMostrada != null && cuotaMostrada.compareTo(total) != 0) {
            throw new InvalidBetException("Las cuotas cambiaron mientras confirmabas. Revisa el boleto y confirma de nuevo.");
        }

        Apuesta apuesta = new Apuesta();
        apuesta.setUsuario(usuario);
        apuesta.setFecha(LocalDateTime.now());
        apuesta.setTipo(resueltas.size() == 1 ? TipoApuesta.SIMPLE : TipoApuesta.PARLEY);
        apuesta.setMontoApostado(montoValido);
        apuesta.setPorcentajeComision(config.getComision());
        apuesta.setCuotaBase(base);
        apuesta.setBonusParley(bonus);
        apuesta.setCuotaTotal(total);
        apuesta.setGananciaPotencial(calculadora.gananciaPotencial(montoValido, total));
        apuesta.setEstado(EstadoApuesta.PENDIENTE);
        for (SeleccionResuelta r : resueltas) {
            SeleccionDto d = r.getSeleccion();
            SeleccionApuesta sa = new SeleccionApuesta();
            sa.setApuesta(apuesta);
            sa.setPartido(r.getPartido());
            sa.setPartidoDescripcion(r.getPartido().getDescripcion());
            sa.setMercado(d.getMercado());
            sa.setOpcion(d.getOpcion());
            sa.setLinea(d.getLinea());
            sa.setLado(d.getLado());
            sa.setJugador(r.getJugador());
            sa.setDescripcion(r.getDescripcion());
            sa.setCuota(r.getCuota());
            sa.setEstado(EstadoSeleccion.PENDIENTE);
            apuesta.getSelecciones().add(sa);
        }
        return apuestaRepositorio.save(apuesta);
    }

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public List<Apuesta> historial(String username) {
        Usuario usuario = usuarioServicio.obtenerPorUsername(username);
        return apuestaRepositorio.findByUsuarioIdOrderByFechaDesc(usuario.getId());
    }

    /** Devuelve la apuesta solo si pertenece al usuario que la pide. */
    @Transactional(readOnly = true)
    public Apuesta obtenerDeUsuario(Long id, String username) {
        Apuesta apuesta = apuestaRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apuesta no encontrada."));
        if (!apuesta.getUsuario().getUsername().equalsIgnoreCase(username)) {
            throw new UnauthorizedActionException("No puedes ver una apuesta de otro usuario.");
        }
        return apuesta;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Apuesta> listarTodas() {
        return apuestaRepositorio.findAllByOrderByFechaDesc();
    }

    // ---------- validación de una selección ----------

    /** Comprueba partido abierto, mercado coherente y cuota disponible. Lanza InvalidBetException si algo falla. */
    SeleccionResuelta resolver(SeleccionDto s) {
        if (s == null || s.getPartidoId() == null || s.getMercado() == null) {
            throw new InvalidBetException("La selección está incompleta.");
        }
        Partido partido = partidoRepositorio.findById(s.getPartidoId())
                .orElseThrow(() -> new InvalidBetException("El partido seleccionado no existe."));
        if (partido.getEstado() == EstadoPartido.CANCELADO) {
            throw new InvalidBetException("El partido " + partido.getDescripcion() + " fue cancelado.");
        }
        if (!partido.admiteApuestas(LocalDateTime.now())) {
            throw new InvalidBetException("Las apuestas para " + partido.getDescripcion() + " ya están cerradas.");
        }

        SeleccionDto n = new SeleccionDto(partido.getId(), s.getMercado(), s.getOpcion(), null, null, null);
        Jugador jugador = null;
        switch (s.getMercado()) {
            case RESULTADO -> {
                if (s.getOpcion() != OpcionSeleccion.LOCAL && s.getOpcion() != OpcionSeleccion.EMPATE
                        && s.getOpcion() != OpcionSeleccion.VISITANTE) {
                    throw new InvalidBetException("Opción inválida para el resultado del partido.");
                }
            }
            case GOLES -> {
                exigirMasMenos(s);
                exigirLinea(s);
                n.setLinea(s.getLinea());
            }
            case GOLEADOR -> {
                if (s.getJugadorId() == null) {
                    throw new InvalidBetException("Elige un jugador.");
                }
                jugador = jugadorRepositorio.findById(s.getJugadorId())
                        .orElseThrow(() -> new InvalidBetException("El jugador seleccionado no existe."));
                Long equipoId = jugador.getEquipo().getId();
                if (jugador.getEstado() != EstadoJugador.ACTIVO
                        || (!equipoId.equals(partido.getLocal().getId()) && !equipoId.equals(partido.getVisitante().getId()))) {
                    throw new InvalidBetException("Ese jugador no está disponible para este partido.");
                }
                n.setOpcion(OpcionSeleccion.ANOTA);
                n.setJugadorId(jugador.getId());
            }
            case TIROS, CORNERS -> {
                exigirMasMenos(s);
                exigirLinea(s);
                if (s.getLado() == null) {
                    throw new InvalidBetException("Indica a qué equipo se refiere la apuesta.");
                }
                n.setLinea(s.getLinea());
                n.setLado(s.getLado());
            }
        }
        BigDecimal cuota = servicioCuotas.calcularCuota(partido, n)
                .orElseThrow(() -> new InvalidBetException("Las cuotas de este mercado todavía no están disponibles."));
        if (cuota.signum() <= 0) {
            throw new InvalidBetException("La cuota calculada no es válida.");
        }
        return new SeleccionResuelta(partido, jugador, n, cuota.setScale(2, RoundingMode.HALF_UP),
                catalogo.describir(partido, n, jugador));
    }

    private void exigirMasMenos(SeleccionDto s) {
        if (s.getOpcion() != OpcionSeleccion.MAS && s.getOpcion() != OpcionSeleccion.MENOS) {
            throw new InvalidBetException("Opción inválida para este mercado.");
        }
    }

    private void exigirLinea(SeleccionDto s) {
        if (!catalogo.lineaPermitida(s.getMercado(), s.getLinea())) {
            throw new InvalidBetException("La línea elegida no existe en este mercado.");
        }
    }

    private BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new InvalidBetException("La cantidad apostada debe ser mayor que 0.");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new InvalidBetException("La cantidad apostada admite como máximo 2 decimales.");
        }
        if (monto.compareTo(MONTO_MAXIMO) > 0) {
            throw new InvalidBetException("La cantidad apostada es demasiado grande.");
        }
        return monto.setScale(2, RoundingMode.HALF_UP);
    }
}
