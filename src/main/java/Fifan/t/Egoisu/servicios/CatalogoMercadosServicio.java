package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.config.ConfiguracionApuestas;
import Fifan.t.Egoisu.dto.*;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.enums.*;
import Fifan.t.Egoisu.repositorios.JugadorRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sabe QUÉ mercados y opciones se ofrecen para un partido (resultado, goles, goleador, tiros, corners)
 * y cómo se describen en texto. Pide cada cuota a ServicioCuotas; no contiene fórmulas.
 * Para añadir un mercado nuevo se amplía aquí (y en TipoMercado / LiquidacionServicio).
 */
@Service
@RequiredArgsConstructor
public class CatalogoMercadosServicio {

    private final ServicioCuotas servicioCuotas;
    private final JugadorRepositorio jugadorRepositorio;
    private final ConfiguracionApuestas config;

    /** Solo el mercado 1X2 (tarjetas de la página principal). */
    @Transactional(readOnly = true)
    public PartidoApuestaDto construirResumen(Partido p) {
        return construir(p, true);
    }

    /** Todos los mercados del partido. */
    @Transactional(readOnly = true)
    public PartidoApuestaDto construirCompleto(Partido p) {
        return construir(p, false);
    }

    /** Líneas permitidas para los mercados que usan línea (goles, tiros, corners). */
    public boolean lineaPermitida(TipoMercado mercado, BigDecimal linea) {
        if (linea == null) return false;
        List<BigDecimal> permitidas = switch (mercado) {
            case GOLES -> config.getLineasGoles();
            case TIROS -> config.getLineasTiros();
            case CORNERS -> config.getLineasCorners();
            default -> List.of();
        };
        return permitidas.stream().anyMatch(l -> l.compareTo(linea) == 0);
    }

    /** Texto legible de una selección (se guarda como snapshot en la apuesta). */
    public String describir(Partido p, SeleccionDto s, Jugador jugador) {
        return switch (s.getMercado()) {
            case RESULTADO -> switch (s.getOpcion()) {
                case LOCAL -> "Gana " + p.getLocal().getNombre();
                case VISITANTE -> "Gana " + p.getVisitante().getNombre();
                default -> "Empate";
            };
            case GOLES -> masMenos(s) + fmt(s.getLinea()) + " goles";
            case GOLEADOR -> jugador.getNombre() + " marca (" + jugador.getEquipo().getNombre() + ")";
            case TIROS -> "Tiros de " + nombreEquipo(p, s.getLado()) + ": " + masMenos(s) + fmt(s.getLinea());
            case CORNERS -> "Tiros de esquina de " + nombreEquipo(p, s.getLado()) + ": " + masMenos(s) + fmt(s.getLinea());
        };
    }

    // ---------- construcción de grupos ----------

    private PartidoApuestaDto construir(Partido p, boolean soloResultado) {
        List<GrupoMercadoDto> grupos = new ArrayList<>();
        if (!p.admiteApuestas(LocalDateTime.now())) {
            return new PartidoApuestaDto(p, false, grupos);
        }
        grupos.add(new GrupoMercadoDto(TipoMercado.RESULTADO.getEtiqueta(), List.of(
                opcion(p, "Local: " + p.getLocal().getNombre(), sel(p, TipoMercado.RESULTADO, OpcionSeleccion.LOCAL, null, null, null)),
                opcion(p, "Empate", sel(p, TipoMercado.RESULTADO, OpcionSeleccion.EMPATE, null, null, null)),
                opcion(p, "Visitante: " + p.getVisitante().getNombre(), sel(p, TipoMercado.RESULTADO, OpcionSeleccion.VISITANTE, null, null, null)))));
        if (soloResultado) {
            return new PartidoApuestaDto(p, true, grupos);
        }

        List<OpcionMercadoDto> goles = new ArrayList<>();
        for (BigDecimal linea : config.getLineasGoles()) {
            goles.add(opcion(p, "Más de " + fmt(linea), sel(p, TipoMercado.GOLES, OpcionSeleccion.MAS, linea, null, null)));
            goles.add(opcion(p, "Menos de " + fmt(linea), sel(p, TipoMercado.GOLES, OpcionSeleccion.MENOS, linea, null, null)));
        }
        grupos.add(new GrupoMercadoDto(TipoMercado.GOLES.getEtiqueta(), goles));

        List<OpcionMercadoDto> goleadores = new ArrayList<>();
        for (Equipo_ e : equipos(p)) {
            for (Jugador j : jugadorRepositorio.findByEquipoIdAndEstadoOrderByDorsalAsc(e.id(), EstadoJugador.ACTIVO)) {
                goleadores.add(opcion(p, "#" + j.getDorsal() + " " + j.getNombre() + " (" + e.nombre() + ", "
                        + j.posicionEfectiva().getEtiqueta() + " " + j.mediaEfectiva() + ")",
                        sel(p, TipoMercado.GOLEADOR, OpcionSeleccion.ANOTA, null, null, j.getId())));
            }
        }
        if (!goleadores.isEmpty()) {
            grupos.add(new GrupoMercadoDto("Goleador (marca al menos un gol)", goleadores));
        }
        agregarLineasPorEquipo(p, TipoMercado.TIROS, config.getLineasTiros(), grupos);
        agregarLineasPorEquipo(p, TipoMercado.CORNERS, config.getLineasCorners(), grupos);
        return new PartidoApuestaDto(p, true, grupos);
    }

    private void agregarLineasPorEquipo(Partido p, TipoMercado mercado, List<BigDecimal> lineas, List<GrupoMercadoDto> grupos) {
        for (LadoEquipo lado : LadoEquipo.values()) {
            List<OpcionMercadoDto> opciones = new ArrayList<>();
            for (BigDecimal linea : lineas) {
                opciones.add(opcion(p, "Más de " + fmt(linea), sel(p, mercado, OpcionSeleccion.MAS, linea, lado, null)));
                opciones.add(opcion(p, "Menos de " + fmt(linea), sel(p, mercado, OpcionSeleccion.MENOS, linea, lado, null)));
            }
            grupos.add(new GrupoMercadoDto(mercado.getEtiqueta() + " de " + nombreEquipo(p, lado), opciones));
        }
    }

    private OpcionMercadoDto opcion(Partido p, String etiqueta, SeleccionDto seleccion) {
        BigDecimal cuota = servicioCuotas.calcularCuota(p, seleccion).orElse(null);
        return new OpcionMercadoDto(etiqueta, cuota, seleccion);
    }

    private SeleccionDto sel(Partido p, TipoMercado m, OpcionSeleccion o, BigDecimal linea, LadoEquipo lado, Long jugadorId) {
        return new SeleccionDto(p.getId(), m, o, linea, lado, jugadorId);
    }

    // ---------- utilidades ----------

    private record Equipo_(Long id, String nombre) { }

    private List<Equipo_> equipos(Partido p) {
        return List.of(new Equipo_(p.getLocal().getId(), p.getLocal().getNombre()),
                new Equipo_(p.getVisitante().getId(), p.getVisitante().getNombre()));
    }

    private String nombreEquipo(Partido p, LadoEquipo lado) {
        return lado == LadoEquipo.LOCAL ? p.getLocal().getNombre() : p.getVisitante().getNombre();
    }

    private String masMenos(SeleccionDto s) {
        return s.getOpcion() == OpcionSeleccion.MAS ? "Más de " : "Menos de ";
    }

    private String fmt(BigDecimal linea) {
        return linea.stripTrailingZeros().toPlainString();
    }
}
