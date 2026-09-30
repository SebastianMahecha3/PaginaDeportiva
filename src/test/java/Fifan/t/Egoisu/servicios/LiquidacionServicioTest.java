package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Fifan.t.Egoisu.entidades.Apuesta;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.SeleccionApuesta;
import Fifan.t.Egoisu.entidades.enums.EstadoApuesta;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.entidades.enums.EstadoSeleccion;
import Fifan.t.Egoisu.entidades.enums.LadoEquipo;
import Fifan.t.Egoisu.entidades.enums.OpcionSeleccion;
import Fifan.t.Egoisu.entidades.enums.TipoMercado;
import Fifan.t.Egoisu.exception.InvalidMatchException;
import Fifan.t.Egoisu.repositorios.ApuestaRepositorio;
import Fifan.t.Egoisu.repositorios.SeleccionApuestaRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiquidacionServicioTest {

    private SeleccionApuestaRepositorio seleccionRepo;
    private ApuestaRepositorio apuestaRepo;
    private LiquidacionServicio servicio;

    private Equipo local;
    private Equipo visitante;
    private Jugador delantero;
    private Jugador portero;
    private Partido partido; // 2-1, tiros 10-5, corners 6-3, gol de "delantero" (local) x2 y uno del visitante

    @BeforeEach
    void preparar() {
        seleccionRepo = mock(SeleccionApuestaRepositorio.class);
        apuestaRepo = mock(ApuestaRepositorio.class);
        servicio = new LiquidacionServicio(seleccionRepo, apuestaRepo);

        local = TestDatos.equipo(1, "Local");
        visitante = TestDatos.equipo(2, "Visitante");
        delantero = TestDatos.jugador(10, "Delantero", local);
        portero = TestDatos.jugador(11, "Portero", local);
        Jugador rival = TestDatos.jugador(20, "Rival", visitante);

        partido = TestDatos.finalizado(1, local, visitante, 2, 1, 10, 5, 6, 3);
        partido.getGoles().add(TestDatos.gol(partido, delantero, 10));
        partido.getGoles().add(TestDatos.gol(partido, delantero, 50));
        partido.getGoles().add(TestDatos.gol(partido, rival, 70));
    }

    private SeleccionApuesta seleccion(TipoMercado mercado, OpcionSeleccion opcion, String linea, LadoEquipo lado, Jugador jugador) {
        SeleccionApuesta s = new SeleccionApuesta();
        s.setPartido(partido);
        s.setMercado(mercado);
        s.setOpcion(opcion);
        s.setLinea(linea == null ? null : new BigDecimal(linea));
        s.setLado(lado);
        s.setJugador(jugador);
        return s;
    }

    @Test
    void resultado_localGana() {
        assertEquals(EstadoSeleccion.GANADA, servicio.evaluar(seleccion(TipoMercado.RESULTADO, OpcionSeleccion.LOCAL, null, null, null), partido));
        assertEquals(EstadoSeleccion.PERDIDA, servicio.evaluar(seleccion(TipoMercado.RESULTADO, OpcionSeleccion.EMPATE, null, null, null), partido));
        assertEquals(EstadoSeleccion.PERDIDA, servicio.evaluar(seleccion(TipoMercado.RESULTADO, OpcionSeleccion.VISITANTE, null, null, null), partido));
    }

    @Test
    void goles_masMenosDeUnaLinea() {
        // total 3 goles
        assertEquals(EstadoSeleccion.GANADA, servicio.evaluar(seleccion(TipoMercado.GOLES, OpcionSeleccion.MAS, "2.5", null, null), partido));
        assertEquals(EstadoSeleccion.PERDIDA, servicio.evaluar(seleccion(TipoMercado.GOLES, OpcionSeleccion.MAS, "3.5", null, null), partido));
        assertEquals(EstadoSeleccion.GANADA, servicio.evaluar(seleccion(TipoMercado.GOLES, OpcionSeleccion.MENOS, "3.5", null, null), partido));
    }

    @Test
    void goleador_ganaSiMarcoAlMenosUnGol() {
        assertEquals(EstadoSeleccion.GANADA, servicio.evaluar(seleccion(TipoMercado.GOLEADOR, OpcionSeleccion.ANOTA, null, null, delantero), partido));
        assertEquals(EstadoSeleccion.PERDIDA, servicio.evaluar(seleccion(TipoMercado.GOLEADOR, OpcionSeleccion.ANOTA, null, null, portero), partido));
    }

    @Test
    void tirosYCorners_porEquipo() {
        assertEquals(EstadoSeleccion.GANADA, servicio.evaluar(seleccion(TipoMercado.TIROS, OpcionSeleccion.MAS, "8.5", LadoEquipo.LOCAL, null), partido));
        // visitante hizo 5 tiros: más de 4.5 gana, más de 6.5 pierde
        assertEquals(EstadoSeleccion.GANADA, servicio.evaluar(seleccion(TipoMercado.TIROS, OpcionSeleccion.MAS, "4.5", LadoEquipo.VISITANTE, null), partido));
        assertEquals(EstadoSeleccion.PERDIDA, servicio.evaluar(seleccion(TipoMercado.TIROS, OpcionSeleccion.MAS, "6.5", LadoEquipo.VISITANTE, null), partido));
        // corners: local 6, visitante 3
        assertEquals(EstadoSeleccion.GANADA, servicio.evaluar(seleccion(TipoMercado.CORNERS, OpcionSeleccion.MAS, "4.5", LadoEquipo.LOCAL, null), partido));
        assertEquals(EstadoSeleccion.GANADA, servicio.evaluar(seleccion(TipoMercado.CORNERS, OpcionSeleccion.MENOS, "3.5", LadoEquipo.VISITANTE, null), partido));
    }

    private Apuesta apuestaConEstados(EstadoSeleccion... estados) {
        Apuesta a = new Apuesta();
        for (EstadoSeleccion e : estados) {
            SeleccionApuesta s = new SeleccionApuesta();
            s.setEstado(e);
            s.setApuesta(a);
            a.getSelecciones().add(s);
        }
        return a;
    }

    @Test
    void apuesta_unaSeleccionPerdidaHacePerderElParley() {
        assertEquals(EstadoApuesta.PERDIDA, servicio.calcularEstado(apuestaConEstados(EstadoSeleccion.GANADA, EstadoSeleccion.PERDIDA, EstadoSeleccion.PENDIENTE)));
    }

    @Test
    void apuesta_todasGanadasGana() {
        assertEquals(EstadoApuesta.GANADA, servicio.calcularEstado(apuestaConEstados(EstadoSeleccion.GANADA, EstadoSeleccion.GANADA)));
    }

    @Test
    void apuesta_conPendientesSigueEsperando() {
        assertEquals(EstadoApuesta.PENDIENTE, servicio.calcularEstado(apuestaConEstados(EstadoSeleccion.GANADA, EstadoSeleccion.PENDIENTE)));
    }

    @Test
    void apuesta_canceladaSinPerdidasQuedaCancelada() {
        assertEquals(EstadoApuesta.CANCELADA, servicio.calcularEstado(apuestaConEstados(EstadoSeleccion.GANADA, EstadoSeleccion.CANCELADA)));
    }

    @Test
    void liquidar_marcaSeleccionesYActualizaLaApuesta() {
        Apuesta apuesta = new Apuesta();
        SeleccionApuesta s = seleccion(TipoMercado.RESULTADO, OpcionSeleccion.LOCAL, null, null, null);
        s.setEstado(EstadoSeleccion.PENDIENTE);
        s.setApuesta(apuesta);
        apuesta.getSelecciones().add(s);
        when(seleccionRepo.findByPartidoAndEstado(partido, EstadoSeleccion.PENDIENTE)).thenReturn(List.of(s));

        servicio.liquidar(partido);

        assertEquals(EstadoSeleccion.GANADA, s.getEstado());
        assertEquals(EstadoApuesta.GANADA, apuesta.getEstado());
        verify(apuestaRepo).save(apuesta);
    }

    @Test
    void liquidar_rechazaPartidosNoFinalizados() {
        Partido enJuego = TestDatos.partido(5, local, visitante, EstadoPartido.EN_JUEGO, LocalDateTime.now());
        assertThrows(InvalidMatchException.class, () -> servicio.liquidar(enJuego));
        verify(apuestaRepo, never()).save(any(Apuesta.class));
    }

    @Test
    void cancelarSelecciones_cancelaLasPendientes() {
        Apuesta apuesta = new Apuesta();
        SeleccionApuesta s = seleccion(TipoMercado.RESULTADO, OpcionSeleccion.LOCAL, null, null, null);
        s.setEstado(EstadoSeleccion.PENDIENTE);
        s.setApuesta(apuesta);
        apuesta.getSelecciones().add(s);
        when(seleccionRepo.findByPartidoAndEstado(partido, EstadoSeleccion.PENDIENTE)).thenReturn(List.of(s));

        servicio.cancelarSelecciones(partido);

        assertEquals(EstadoSeleccion.CANCELADA, s.getEstado());
        assertEquals(EstadoApuesta.CANCELADA, apuesta.getEstado());
    }
}
