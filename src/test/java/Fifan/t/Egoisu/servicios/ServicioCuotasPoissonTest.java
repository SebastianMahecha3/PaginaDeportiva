package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import Fifan.t.Egoisu.config.ConfiguracionCuotas;
import Fifan.t.Egoisu.dto.SeleccionDto;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.enums.EstadoJugador;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.entidades.enums.LadoEquipo;
import Fifan.t.Egoisu.entidades.enums.OpcionSeleccion;
import Fifan.t.Egoisu.entidades.enums.PosicionJugador;
import Fifan.t.Egoisu.entidades.enums.TipoMercado;
import Fifan.t.Egoisu.repositorios.JugadorRepositorio;
import Fifan.t.Egoisu.repositorios.PartidoRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioCuotasPoissonTest {

    private PartidoRepositorio partidoRepo;
    private JugadorRepositorio jugadorRepo;
    private ServicioCuotasPoisson servicio;

    private Equipo local;
    private Equipo visitante;
    private Partido partido;

    @BeforeEach
    void preparar() {
        partidoRepo = mock(PartidoRepositorio.class);
        jugadorRepo = mock(JugadorRepositorio.class);
        servicio = nuevoServicio();
        local = TestDatos.equipo(1, "Local");
        visitante = TestDatos.equipo(2, "Visitante");
        partido = TestDatos.partido(1, local, visitante, EstadoPartido.PROGRAMADO, LocalDateTime.now().plusDays(1));
        when(partidoRepo.findFinalizadosDeEquipo(anyLong(), eq(EstadoPartido.FINALIZADO))).thenReturn(List.of());
        when(partidoRepo.findFinalizadosConEstadisticas(EstadoPartido.FINALIZADO)).thenReturn(List.of());
        when(jugadorRepo.findByEquipoIdAndEstadoOrderByDorsalAsc(anyLong(), eq(EstadoJugador.ACTIVO))).thenReturn(List.of());
    }

    private ServicioCuotasPoisson nuevoServicio() {
        return new ServicioCuotasPoisson(partidoRepo, jugadorRepo,
                new EstadisticaServicio(partidoRepo), new ConfiguracionCuotas());
    }

    private BigDecimal cuota(SeleccionDto s) {
        return servicio.calcularCuota(partido, s).orElseThrow();
    }

    private SeleccionDto resultado(OpcionSeleccion o) {
        return new SeleccionDto(1L, TipoMercado.RESULTADO, o, null, null, null);
    }

    private SeleccionDto masMenos(TipoMercado m, OpcionSeleccion o, String linea, LadoEquipo lado) {
        return new SeleccionDto(1L, m, o, new BigDecimal(linea), lado, null);
    }

    private SeleccionDto goleador(Jugador j) {
        return new SeleccionDto(1L, TipoMercado.GOLEADOR, OpcionSeleccion.ANOTA, null, null, j.getId());
    }

    private static double implicita(BigDecimal cuota) {
        return 1.0 / cuota.doubleValue();
    }

    @Test
    void sinPartidos_elEquipoDeMayorNivelTieneMejorCuota() {
        local.setValoracionInicial(9);
        visitante.setValoracionInicial(2);

        assertTrue(cuota(resultado(OpcionSeleccion.LOCAL)).compareTo(cuota(resultado(OpcionSeleccion.VISITANTE))) < 0);
    }

    @Test
    void sinPartidos_equiposIgualesElLocalEsFavoritoLigero() {
        assertTrue(cuota(resultado(OpcionSeleccion.LOCAL)).compareTo(cuota(resultado(OpcionSeleccion.VISITANTE))) < 0);
    }

    @Test
    void resultado1X2_lasProbabilidadesImplicitasSumanUnoMasElMargen() {
        double suma = implicita(cuota(resultado(OpcionSeleccion.LOCAL)))
                + implicita(cuota(resultado(OpcionSeleccion.EMPATE)))
                + implicita(cuota(resultado(OpcionSeleccion.VISITANTE)));
        assertEquals(1.05, suma, 0.05); // margen 5% (más el redondeo a 2 decimales)
    }

    @Test
    void masYMenosGoles_tambienIncluyenElMargen() {
        double suma = implicita(cuota(masMenos(TipoMercado.GOLES, OpcionSeleccion.MAS, "2.5", null)))
                + implicita(cuota(masMenos(TipoMercado.GOLES, OpcionSeleccion.MENOS, "2.5", null)));
        assertEquals(1.05, suma, 0.05);
    }

    @Test
    void masGolesEsMasDificilConUnaLineaMasAlta() {
        BigDecimal linea15 = cuota(masMenos(TipoMercado.GOLES, OpcionSeleccion.MAS, "1.5", null));
        BigDecimal linea35 = cuota(masMenos(TipoMercado.GOLES, OpcionSeleccion.MAS, "3.5", null));
        assertTrue(linea15.compareTo(linea35) < 0);
    }

    @Test
    void tirosYCorners_tienenCuotaParaCadaLado() {
        assertTrue(servicio.calcularCuota(partido, masMenos(TipoMercado.TIROS, OpcionSeleccion.MAS, "6.5", LadoEquipo.LOCAL)).isPresent());
        assertTrue(servicio.calcularCuota(partido, masMenos(TipoMercado.CORNERS, OpcionSeleccion.MENOS, "3.5", LadoEquipo.VISITANTE)).isPresent());
    }

    @Test
    void goleador_dependeDeLaPosicion() {
        Jugador delantero = TestDatos.jugador(10, "Delantero", local, PosicionJugador.DELANTERO, 75);
        Jugador medio = TestDatos.jugador(11, "Medio", local, PosicionJugador.MEDIOCAMPISTA, 75);
        Jugador defensa = TestDatos.jugador(12, "Defensa", local, PosicionJugador.DEFENSA, 75);
        Jugador portero = TestDatos.jugador(13, "Portero", local, PosicionJugador.PORTERO, 75);
        when(jugadorRepo.findByEquipoIdAndEstadoOrderByDorsalAsc(1L, EstadoJugador.ACTIVO))
                .thenReturn(List.of(delantero, medio, defensa, portero));

        BigDecimal cDel = cuota(goleador(delantero));
        BigDecimal cMed = cuota(goleador(medio));
        BigDecimal cDef = cuota(goleador(defensa));
        BigDecimal cPor = cuota(goleador(portero));

        // Con el tope de 5.00 las posiciones menos ofensivas pueden empatar en 5.00; nunca se invierte el orden.
        assertTrue(cDel.compareTo(cMed) <= 0, "delantero <= mediocampista");
        assertTrue(cMed.compareTo(cDef) <= 0, "mediocampista <= defensa");
        assertTrue(cDef.compareTo(cPor) <= 0, "defensa <= portero");
        assertTrue(cDel.compareTo(cPor) < 0, "delantero < portero");
    }

    @Test
    void goleador_conProbabilidadBaseEquivalente_jerarquiaEstrictaPorPosicion() {
        BigDecimal base = new BigDecimal("0.40"); // misma probabilidad base de Poisson para las 4 posiciones
        BigDecimal cDel = servicio.aCuota(servicio.ajustarPorPosicion(base, PosicionJugador.DELANTERO));
        BigDecimal cMed = servicio.aCuota(servicio.ajustarPorPosicion(base, PosicionJugador.MEDIOCAMPISTA));
        BigDecimal cDef = servicio.aCuota(servicio.ajustarPorPosicion(base, PosicionJugador.DEFENSA));
        BigDecimal cPor = servicio.aCuota(servicio.ajustarPorPosicion(base, PosicionJugador.PORTERO));

        assertTrue(cDel.compareTo(cMed) < 0, "delantero < mediocampista");
        assertTrue(cMed.compareTo(cDef) < 0, "mediocampista < defensa");
        assertTrue(cDef.compareTo(cPor) < 0, "defensa < portero");
        assertTrue(cPor.compareTo(new BigDecimal("5.00")) <= 0 && cDel.compareTo(new BigDecimal("1.10")) >= 0);
    }

    @Test
    void primerPartido_todosLosMercadosUsanLasMismasReglasYLimites() {
        // Sin historial: la valoración inicial solo alimenta λ; la cuota pasa por el mismo aCuota (1.10 - 5.00, 2 decimales).
        local.setValoracionInicial(10);
        visitante.setValoracionInicial(1);
        Jugador delantero = TestDatos.jugador(10, "Delantero", local, PosicionJugador.DELANTERO, 99);
        Jugador portero = TestDatos.jugador(13, "Portero", visitante, PosicionJugador.PORTERO, 40);
        when(jugadorRepo.findByEquipoIdAndEstadoOrderByDorsalAsc(1L, EstadoJugador.ACTIVO)).thenReturn(List.of(delantero));
        when(jugadorRepo.findByEquipoIdAndEstadoOrderByDorsalAsc(2L, EstadoJugador.ACTIVO)).thenReturn(List.of(portero));
        List<BigDecimal> cuotas = List.of(
                cuota(resultado(OpcionSeleccion.LOCAL)), cuota(resultado(OpcionSeleccion.EMPATE)),
                cuota(resultado(OpcionSeleccion.VISITANTE)),
                cuota(masMenos(TipoMercado.GOLES, OpcionSeleccion.MAS, "3.5", null)),
                cuota(masMenos(TipoMercado.GOLES, OpcionSeleccion.MENOS, "1.5", null)),
                cuota(masMenos(TipoMercado.TIROS, OpcionSeleccion.MAS, "10.5", LadoEquipo.VISITANTE)),
                cuota(masMenos(TipoMercado.CORNERS, OpcionSeleccion.MAS, "4.5", LadoEquipo.VISITANTE)),
                cuota(goleador(delantero)), cuota(goleador(portero)));
        for (BigDecimal c : cuotas) {
            assertTrue(c.compareTo(new BigDecimal("1.10")) >= 0 && c.compareTo(new BigDecimal("5.00")) <= 0, "fuera de límites: " + c);
            assertEquals(2, c.scale());
        }
    }

    @Test
    void goleador_dependeDeLaMedia() {
        Jugador estrella = TestDatos.jugador(10, "Estrella", local, PosicionJugador.DELANTERO, 92);
        Jugador regular = TestDatos.jugador(11, "Regular", local, PosicionJugador.DELANTERO, 60);
        when(jugadorRepo.findByEquipoIdAndEstadoOrderByDorsalAsc(1L, EstadoJugador.ACTIVO))
                .thenReturn(List.of(estrella, regular));

        assertTrue(cuota(goleador(estrella)).compareTo(cuota(goleador(regular))) < 0);
    }

    @Test
    void goleador_jugadorInactivoODesconocidoNoTieneCuota() {
        Jugador fantasma = TestDatos.jugador(99, "Fantasma", local);

        assertEquals(Optional.empty(), servicio.calcularCuota(partido, goleador(fantasma)));
    }

    @Test
    void conHistorial_unEquipoQueGoleaYTiraMuchoBajaSuCuota() {
        BigDecimal sinHistorial = cuota(resultado(OpcionSeleccion.LOCAL));

        Equipo otro = TestDatos.equipo(3, "Otro");
        List<Partido> historial = List.of(
                TestDatos.finalizado(21, local, otro, 3, 0, 15, 3, 7, 1),
                TestDatos.finalizado(22, otro, local, 0, 3, 3, 15, 1, 7),
                TestDatos.finalizado(23, local, otro, 4, 0, 16, 2, 8, 0));
        when(partidoRepo.findFinalizadosDeEquipo(1L, EstadoPartido.FINALIZADO)).thenReturn(historial);
        when(partidoRepo.findFinalizadosConEstadisticas(EstadoPartido.FINALIZADO)).thenReturn(historial);
        servicio = nuevoServicio(); // sin caché

        BigDecimal conHistorial = cuota(resultado(OpcionSeleccion.LOCAL));

        assertTrue(conHistorial.compareTo(sinHistorial) < 0, conHistorial + " debería ser menor que " + sinHistorial);
    }

    @Test
    void todasLasCuotasQuedanDentroDeLosLimites() {
        local.setValoracionInicial(10);
        visitante.setValoracionInicial(1);
        for (OpcionSeleccion o : List.of(OpcionSeleccion.LOCAL, OpcionSeleccion.EMPATE, OpcionSeleccion.VISITANTE)) {
            BigDecimal c = cuota(resultado(o));
            assertTrue(c.compareTo(new BigDecimal("1.10")) >= 0 && c.compareTo(new BigDecimal("5.00")) <= 0, "fuera de límites: " + c);
            assertEquals(2, c.scale());
        }
    }

    @Test
    void aCuota_aplicaElMargenDel5UnaSolaVez() {
        // 1 / (0.50 × 1.05) = 1.9047... -> 1.90 (sin comisión dentro de la cuota)
        assertEquals(0, new BigDecimal("1.90").compareTo(servicio.aCuota(new BigDecimal("0.50"))));
    }

    @Test
    void aCuota_probabilidadesDiminutasNuncaSuperan5() {
        // Con margen 5% y tope 5.00, toda p < 19.05% queda exactamente en 5.00.
        for (String p : List.of("0", "0.0001", "0.01", "0.05", "0.0952", "0.19")) {
            assertEquals(0, new BigDecimal("5.00").compareTo(servicio.aCuota(new BigDecimal(p))), "p = " + p);
        }
    }

    @Test
    void aCuota_probabilidadesAltasNuncaBajanDe110() {
        assertEquals(0, new BigDecimal("1.10").compareTo(servicio.aCuota(BigDecimal.ONE)));
        assertEquals(0, new BigDecimal("1.10").compareTo(servicio.aCuota(new BigDecimal("0.95"))));
    }

    @Test
    void goleadoresYMercadosExtremos_respetanLosLimites() {
        Jugador portero = TestDatos.jugador(13, "Portero", local, PosicionJugador.PORTERO, 60);
        Jugador delantero = TestDatos.jugador(10, "Delantero", local, PosicionJugador.DELANTERO, 90);
        when(jugadorRepo.findByEquipoIdAndEstadoOrderByDorsalAsc(1L, EstadoJugador.ACTIVO))
                .thenReturn(List.of(portero, delantero));
        List<BigDecimal> cuotas = List.of(
                cuota(goleador(portero)), cuota(goleador(delantero)),
                cuota(masMenos(TipoMercado.GOLES, OpcionSeleccion.MAS, "3.5", null)),
                cuota(masMenos(TipoMercado.TIROS, OpcionSeleccion.MAS, "10.5", LadoEquipo.VISITANTE)),
                cuota(masMenos(TipoMercado.CORNERS, OpcionSeleccion.MAS, "4.5", LadoEquipo.LOCAL)));
        for (BigDecimal c : cuotas) {
            assertTrue(c.compareTo(new BigDecimal("1.10")) >= 0 && c.compareTo(new BigDecimal("5.00")) <= 0, "fuera de límites: " + c);
        }
    }

    @Test
    void seleccionIncompletaNoTieneCuota() {
        assertEquals(Optional.empty(), servicio.calcularCuota(partido,
                new SeleccionDto(1L, TipoMercado.GOLES, OpcionSeleccion.MAS, null, null, null)));
        assertEquals(Optional.empty(), servicio.calcularCuota(partido,
                new SeleccionDto(1L, TipoMercado.TIROS, OpcionSeleccion.MAS, new BigDecimal("4.5"), null, null)));
        assertEquals(Optional.empty(), servicio.calcularCuota(partido,
                new SeleccionDto(1L, TipoMercado.RESULTADO, OpcionSeleccion.MAS, null, null, null)));
    }
}