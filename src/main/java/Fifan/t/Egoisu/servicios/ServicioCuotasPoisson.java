package Fifan.t.Egoisu.servicios;

import static Fifan.t.Egoisu.servicios.MatematicaProbabilidad.MC;

import Fifan.t.Egoisu.config.ConfiguracionCuotas;
import Fifan.t.Egoisu.dto.RendimientoDto;
import Fifan.t.Egoisu.dto.ResumenEquipoDto;
import Fifan.t.Egoisu.dto.SeleccionDto;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.Gol;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.enums.EstadoJugador;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.entidades.enums.LadoEquipo;
import Fifan.t.Egoisu.entidades.enums.OpcionSeleccion;
import Fifan.t.Egoisu.repositorios.JugadorRepositorio;
import Fifan.t.Egoisu.repositorios.PartidoRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de cuotas (fórmula aprobada). Todo se calcula con BigDecimal.
 *
 * 1) Goles esperados de cada equipo (Poisson). El ataque de un equipo combina sus GOLES y sus TIROS de partidos
 *    anteriores; la debilidad defensiva del rival combina los goles y tiros que ha RECIBIDO:
 *        λ_local     = media_goles_de_local     × ataque_local     × debilidad_visitante
 *        λ_visitante = media_goles_de_visitante × ataque_visitante × debilidad_local
 * 2) Con historial: 50% rendimiento general + 50% como local/visitante, suavizado hacia el promedio de la liga con
 *    peso n/(n+k). Sin partidos (n = 0): solo el NIVEL (valoración 1-10) del equipo:
 *        ataque = 0.5 + valoración/10      debilidad = 1.5 - valoración/10
 * 3) Mercados: 1X2 (matriz de Poisson), más/menos goles (Poisson de λL+λV), tiros y corners (Poisson por equipo) y
 *    goleador: P = 1 - exp(-λ_equipo × parte_del_jugador), donde la parte depende de su POSICIÓN, su MEDIA (1-99)
 *    y sus goles anteriores.
 * 4) Cuota final = 1 / (p × (1 + margen)) con margen 5% aplicado una sola vez (la comisión de la apuesta NO entra aquí).
 *    Antes de convertir, p se acota por abajo a 1 / (cuotaMaxima × (1 + margen)) para que una probabilidad diminuta
 *    no dispare la cuota. Después se acota entre cuotaMinima (1.10) y cuotaMaxima (10.00), con 2 decimales (HALF_UP).
 */
@Service
@RequiredArgsConstructor
public class ServicioCuotasPoisson implements ServicioCuotas {

    private static final BigDecimal DOS = BigDecimal.valueOf(2);
    private static final BigDecimal MITAD = new BigDecimal("0.5");
    private static final BigDecimal FACTOR_MIN = new BigDecimal("0.25");
    private static final BigDecimal FACTOR_MAX = new BigDecimal("3");
    private static final BigDecimal LAMBDA_MIN = new BigDecimal("0.05");
    private static final BigDecimal MEDIA_REFERENCIA = BigDecimal.valueOf(70);

    private final PartidoRepositorio partidoRepositorio;
    private final JugadorRepositorio jugadorRepositorio;
    private final EstadisticaServicio estadisticaServicio;
    private final ConfiguracionCuotas config;

    private final Map<Long, Entrada> cache = new ConcurrentHashMap<>();

    private enum Metrica { GOLES, TIROS, CORNERS }

    private record Entrada(Instant creado, Modelo modelo) {}

    /** Resultado del cálculo para un partido; se reutiliza para todas sus opciones de apuesta. */
    static final class Modelo {
        BigDecimal lambdaLocal;
        BigDecimal lambdaVisitante;
        BigDecimal tirosLocal;
        BigDecimal tirosVisitante;
        BigDecimal cornersLocal;
        BigDecimal cornersVisitante;
        BigDecimal[] resultado;
        final Map<Long, BigDecimal> probGolJugador = new HashMap<>();
    }

    // ------------------------------------------------------------------ API pública

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> calcularCuota(Partido partido, SeleccionDto s) {
        if (partido == null || s == null || s.getMercado() == null || s.getOpcion() == null) {
            return Optional.empty();
        }
        Modelo m = modelo(partido);
        BigDecimal p = switch (s.getMercado()) {
            case RESULTADO -> switch (s.getOpcion()) {
                case LOCAL -> m.resultado[0];
                case EMPATE -> m.resultado[1];
                case VISITANTE -> m.resultado[2];
                default -> null;
            };
            case GOLES -> masOMenos(s, m.lambdaLocal.add(m.lambdaVisitante, MC));
            case TIROS -> s.getLado() == null ? null
                    : masOMenos(s, s.getLado() == LadoEquipo.LOCAL ? m.tirosLocal : m.tirosVisitante);
            case CORNERS -> s.getLado() == null ? null
                    : masOMenos(s, s.getLado() == LadoEquipo.LOCAL ? m.cornersLocal : m.cornersVisitante);
            case GOLEADOR -> s.getOpcion() == OpcionSeleccion.ANOTA && s.getJugadorId() != null
                    ? m.probGolJugador.get(s.getJugadorId()) : null;
        };
        return p == null ? Optional.empty() : Optional.of(aCuota(p));
    }

    /** Fuerza un recálculo en la próxima consulta (p. ej. tras registrar un resultado). */
    public void limpiarCache() {
        cache.clear();
    }

    // ------------------------------------------------------------------ probabilidad -> cuota

    private BigDecimal masOMenos(SeleccionDto s, BigDecimal lambda) {
        if (s.getLinea() == null) {
            return null;
        }
        if (s.getOpcion() == OpcionSeleccion.MAS) {
            return MatematicaProbabilidad.masQue(s.getLinea(), lambda);
        }
        if (s.getOpcion() == OpcionSeleccion.MENOS) {
            return MatematicaProbabilidad.menosQue(s.getLinea(), lambda);
        }
        return null;
    }

    /**
     * ÚNICO punto donde una probabilidad se convierte en cuota (lo usan todos los mercados):
     *   1) p se acota a [pMin, 1], con pMin = 1 / (cuotaMaxima × (1 + margen)) (≈ 9.52% con margen 5% y tope 10.00).
     *   2) cuota = 1 / (p × (1 + margen))      (margen aplicado una sola vez; sin comisión)
     *   3) cuota = max(cuotaMinima, min(cuota, cuotaMaxima)), redondeada a 2 decimales.
     */
    BigDecimal aCuota(BigDecimal p) {
        BigDecimal factorMargen = BigDecimal.ONE.add(config.getMargen());
        BigDecimal probabilidadMinima = BigDecimal.ONE.divide(config.getCuotaMaxima().multiply(factorMargen, MC), MC);
        BigDecimal probabilidad = p.max(probabilidadMinima).min(BigDecimal.ONE);
        BigDecimal cuota = BigDecimal.ONE.divide(probabilidad.multiply(factorMargen, MC), MC);
        cuota = cuota.max(config.getCuotaMinima()).min(config.getCuotaMaxima());
        return cuota.setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ modelo del partido

    private Modelo modelo(Partido partido) {
        Long id = partido.getId();
        if (id != null) {
            Entrada e = cache.get(id);
            if (e != null && e.creado().plusSeconds(config.getCacheSegundos()).isAfter(Instant.now())) {
                return e.modelo();
            }
        }
        Modelo m = construir(partido);
        if (id != null) {
            cache.put(id, new Entrada(Instant.now(), m));
        }
        return m;
    }

    private Modelo construir(Partido partido) {
        Equipo eLocal = partido.getLocal();
        Equipo eVisitante = partido.getVisitante();
        Liga liga = construirLiga();

        List<Partido> historialLocal = historial(eLocal, partido);
        List<Partido> historialVisitante = historial(eVisitante, partido);
        Perfil local = new Perfil(eLocal, true, historialLocal, liga);
        Perfil visitante = new Perfil(eVisitante, false, historialVisitante, liga);

        Modelo m = new Modelo();
        // Goles: el ataque mezcla goles y tiros; la debilidad del rival mezcla goles y tiros recibidos.
        BigDecimal ataqueLocal = mitad(local.favor(Metrica.GOLES), local.favor(Metrica.TIROS));
        BigDecimal ataqueVisitante = mitad(visitante.favor(Metrica.GOLES), visitante.favor(Metrica.TIROS));
        BigDecimal debilidadLocal = mitad(local.contra(Metrica.GOLES), local.contra(Metrica.TIROS));
        BigDecimal debilidadVisitante = mitad(visitante.contra(Metrica.GOLES), visitante.contra(Metrica.TIROS));
        m.lambdaLocal = minimo(liga.local(Metrica.GOLES).multiply(ataqueLocal, MC).multiply(debilidadVisitante, MC), LAMBDA_MIN);
        m.lambdaVisitante = minimo(liga.visitante(Metrica.GOLES).multiply(ataqueVisitante, MC).multiply(debilidadLocal, MC), LAMBDA_MIN);
        m.resultado = MatematicaProbabilidad.resultado1X2(m.lambdaLocal, m.lambdaVisitante, config.getMaxGolesMatriz());

        // Tiros y corners: media de la liga × lo que el equipo produce × lo que el rival concede.
        m.tirosLocal = minimo(liga.local(Metrica.TIROS).multiply(local.favor(Metrica.TIROS), MC).multiply(visitante.contra(Metrica.TIROS), MC), LAMBDA_MIN);
        m.tirosVisitante = minimo(liga.visitante(Metrica.TIROS).multiply(visitante.favor(Metrica.TIROS), MC).multiply(local.contra(Metrica.TIROS), MC), LAMBDA_MIN);
        m.cornersLocal = minimo(liga.local(Metrica.CORNERS).multiply(local.favor(Metrica.CORNERS), MC).multiply(visitante.contra(Metrica.CORNERS), MC), LAMBDA_MIN);
        m.cornersVisitante = minimo(liga.visitante(Metrica.CORNERS).multiply(visitante.favor(Metrica.CORNERS), MC).multiply(local.contra(Metrica.CORNERS), MC), LAMBDA_MIN);

        // Goleador: reparto de los goles esperados del equipo entre sus jugadores.
        repartirGoles(eLocal, m.lambdaLocal, historialLocal, m.probGolJugador);
        repartirGoles(eVisitante, m.lambdaVisitante, historialVisitante, m.probGolJugador);
        return m;
    }

    private List<Partido> historial(Equipo equipo, Partido actual) {
        return partidoRepositorio.findFinalizadosDeEquipo(equipo.getId(), EstadoPartido.FINALIZADO).stream()
                .filter(p -> actual.getId() == null || !actual.getId().equals(p.getId()))
                .toList();
    }

    // ------------------------------------------------------------------ goleador (posición + media + historial)

    /**
     * peso(jugador) = pesoPosición × (media/70)^3 × (1 + goles_del_jugador / partidos_del_equipo)
     * λ_jugador = λ_equipo × peso / suma_de_pesos_del_equipo      P(marca) = 1 - exp(-λ_jugador)
     * Un delantero de media alta obtiene una probabilidad mayor; un portero, casi nula.
     */
    private void repartirGoles(Equipo equipo, BigDecimal lambdaEquipo, List<Partido> partidosEquipo, Map<Long, BigDecimal> destino) {
        List<Jugador> jugadores = jugadorRepositorio.findByEquipoIdAndEstadoOrderByDorsalAsc(equipo.getId(), EstadoJugador.ACTIVO);
        if (jugadores.isEmpty()) {
            return;
        }
        Map<Long, Integer> golesPrevios = new HashMap<>();
        for (Partido p : partidosEquipo) {
            for (Gol g : p.getGoles()) {
                golesPrevios.merge(g.getJugador().getId(), 1, Integer::sum);
            }
        }
        BigDecimal partidos = BigDecimal.valueOf(Math.max(1, partidosEquipo.size()));
        Map<Long, BigDecimal> pesos = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Jugador j : jugadores) {
            BigDecimal factorMedia = BigDecimal.valueOf(j.mediaEfectiva()).divide(MEDIA_REFERENCIA, MC).pow(3, MC);
            BigDecimal factorHistorial = BigDecimal.ONE.add(
                    BigDecimal.valueOf(golesPrevios.getOrDefault(j.getId(), 0)).divide(partidos, MC), MC);
            BigDecimal peso = j.posicionEfectiva().getPesoGol().multiply(factorMedia, MC).multiply(factorHistorial, MC);
            pesos.put(j.getId(), peso);
            total = total.add(peso, MC);
        }
        for (Jugador j : jugadores) {
            BigDecimal lambdaJugador = lambdaEquipo.multiply(pesos.get(j.getId()), MC).divide(total, MC);
            BigDecimal p = BigDecimal.ONE.subtract(MatematicaProbabilidad.exp(lambdaJugador.negate()), MC);
            destino.put(j.getId(), MatematicaProbabilidad.limitar(p));
        }
    }

    // ------------------------------------------------------------------ promedios de la liga

    /** Promedios de la liga por métrica: lo que producen los locales y lo que producen los visitantes. */
    private final class Liga {
        private final BigDecimal[] local = new BigDecimal[Metrica.values().length];
        private final BigDecimal[] visitante = new BigDecimal[Metrica.values().length];

        BigDecimal local(Metrica m) { return local[m.ordinal()]; }

        BigDecimal visitante(Metrica m) { return visitante[m.ordinal()]; }

        BigDecimal general(Metrica m) { return local(m).add(visitante(m), MC).divide(DOS, MC); }
    }

    /**
     * Promedio = (valorPorDefecto × k + suma real) / (k + partidos). Con 0 partidos usa el valor por defecto;
     * a medida que hay datos, los datos reales pesan cada vez más.
     */
    private Liga construirLiga() {
        List<Partido> todos = partidoRepositorio.findFinalizadosConEstadisticas(EstadoPartido.FINALIZADO);
        BigDecimal[] sumaLocal = new BigDecimal[Metrica.values().length];
        BigDecimal[] sumaVisitante = new BigDecimal[Metrica.values().length];
        Arrays.fill(sumaLocal, BigDecimal.ZERO);
        Arrays.fill(sumaVisitante, BigDecimal.ZERO);
        for (Partido p : todos) {
            sumaLocal[Metrica.GOLES.ordinal()] = sumaLocal[Metrica.GOLES.ordinal()].add(BigDecimal.valueOf(valor(p.getGolesLocal())));
            sumaVisitante[Metrica.GOLES.ordinal()] = sumaVisitante[Metrica.GOLES.ordinal()].add(BigDecimal.valueOf(valor(p.getGolesVisitante())));
            if (p.getEstadistica() != null) {
                sumaLocal[Metrica.TIROS.ordinal()] = sumaLocal[Metrica.TIROS.ordinal()].add(BigDecimal.valueOf(p.getEstadistica().getTirosLocal()));
                sumaVisitante[Metrica.TIROS.ordinal()] = sumaVisitante[Metrica.TIROS.ordinal()].add(BigDecimal.valueOf(p.getEstadistica().getTirosVisitante()));
                sumaLocal[Metrica.CORNERS.ordinal()] = sumaLocal[Metrica.CORNERS.ordinal()].add(BigDecimal.valueOf(p.getEstadistica().getCornersLocal()));
                sumaVisitante[Metrica.CORNERS.ordinal()] = sumaVisitante[Metrica.CORNERS.ordinal()].add(BigDecimal.valueOf(p.getEstadistica().getCornersVisitante()));
            }
        }
        BigDecimal k = BigDecimal.valueOf(config.getSuavizadoK());
        BigDecimal divisor = k.add(BigDecimal.valueOf(todos.size()));
        Liga liga = new Liga();
        BigDecimal[] defectoLocal = {config.getGolesLocalLiga(), config.getTirosLiga(), config.getCornersLiga()};
        BigDecimal[] defectoVisitante = {config.getGolesVisitanteLiga(), config.getTirosLiga(), config.getCornersLiga()};
        for (Metrica m : Metrica.values()) {
            int i = m.ordinal();
            liga.local[i] = defectoLocal[i].multiply(k, MC).add(sumaLocal[i], MC).divide(divisor, MC);
            liga.visitante[i] = defectoVisitante[i].multiply(k, MC).add(sumaVisitante[i], MC).divide(divisor, MC);
        }
        return liga;
    }

    // ------------------------------------------------------------------ perfil de un equipo en este partido

    /**
     * Fortalezas RELATIVAS de un equipo (1.00 = igual al promedio de la liga).
     *  - favor(m): cuánto produce (goles, tiros, corners).   - contra(m): cuánto concede.
     * Sin partidos jugados se usa solo la valoración (nivel) del equipo.
     */
    private final class Perfil {
        private final Equipo equipo;
        private final boolean juegaDeLocal;
        private final int n;
        private final RendimientoDto general;
        private final RendimientoDto contexto;
        private final Liga liga;

        Perfil(Equipo equipo, boolean juegaDeLocal, List<Partido> historial, Liga liga) {
            ResumenEquipoDto resumen = estadisticaServicio.construir(equipo, historial);
            this.equipo = equipo;
            this.juegaDeLocal = juegaDeLocal;
            this.liga = liga;
            this.general = resumen.getGeneral();
            this.contexto = juegaDeLocal ? resumen.getComoLocal() : resumen.getComoVisitante();
            this.n = general.getPartidos();
        }

        BigDecimal favor(Metrica m) {
            if (n == 0) {
                return ataquePorNivel();
            }
            BigDecimal relGeneral = dividir(promFavor(general, m), liga.general(m));
            BigDecimal referenciaContexto = juegaDeLocal ? liga.local(m) : liga.visitante(m);
            return suavizar(relGeneral, contexto.getPartidos() > 0 ? dividir(promFavor(contexto, m), referenciaContexto) : null);
        }

        BigDecimal contra(Metrica m) {
            if (n == 0) {
                return debilidadPorNivel();
            }
            BigDecimal relGeneral = dividir(promContra(general, m), liga.general(m));
            // Lo que un equipo concede en casa es lo que producen los visitantes, y viceversa.
            BigDecimal referenciaContexto = juegaDeLocal ? liga.visitante(m) : liga.local(m);
            return suavizar(relGeneral, contexto.getPartidos() > 0 ? dividir(promContra(contexto, m), referenciaContexto) : null);
        }

        /** 50% general + 50% local/visitante (si hay), y luego mezcla con 1.00 (liga) con peso n/(n+k). */
        private BigDecimal suavizar(BigDecimal relGeneral, BigDecimal relContexto) {
            BigDecimal rel = relContexto == null ? relGeneral : relGeneral.add(relContexto, MC).multiply(MITAD, MC);
            BigDecimal peso = BigDecimal.valueOf(n).divide(BigDecimal.valueOf(n + config.getSuavizadoK()), MC);
            BigDecimal mezcla = peso.multiply(rel, MC).add(BigDecimal.ONE.subtract(peso, MC), MC);
            return mezcla.max(FACTOR_MIN).min(FACTOR_MAX);
        }

        /** Ataque por nivel: 0.5 + valoración/10 (valoración 1-10). */
        private BigDecimal ataquePorNivel() {
            return MITAD.add(BigDecimal.valueOf(equipo.getValoracionInicial()).divide(BigDecimal.TEN, MC), MC);
        }

        /** Debilidad defensiva por nivel: 1.5 - valoración/10. */
        private BigDecimal debilidadPorNivel() {
            return new BigDecimal("1.5").subtract(BigDecimal.valueOf(equipo.getValoracionInicial()).divide(BigDecimal.TEN, MC), MC);
        }
    }

    private static BigDecimal promFavor(RendimientoDto r, Metrica m) {
        return switch (m) {
            case GOLES -> r.getPromGolesFavor();
            case TIROS -> r.getPromTiros();
            case CORNERS -> r.getPromCorners();
        };
    }

    private static BigDecimal promContra(RendimientoDto r, Metrica m) {
        return switch (m) {
            case GOLES -> r.getPromGolesContra();
            case TIROS -> r.getPromTirosContra();
            case CORNERS -> r.getPromCornersContra();
        };
    }

    // ------------------------------------------------------------------ utilidades

    private static BigDecimal dividir(BigDecimal a, BigDecimal b) {
        return b.signum() == 0 ? BigDecimal.ONE : a.divide(b, MC);
    }

    private static BigDecimal mitad(BigDecimal a, BigDecimal b) {
        return a.add(b, MC).multiply(MITAD, MC);
    }

    private static BigDecimal minimo(BigDecimal valor, BigDecimal piso) {
        return valor.max(piso);
    }

    private static int valor(Integer i) {
        return i == null ? 0 : i;
    }
}