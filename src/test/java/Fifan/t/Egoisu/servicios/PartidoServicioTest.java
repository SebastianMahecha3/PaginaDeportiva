package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Fifan.t.Egoisu.dto.GolDto;
import Fifan.t.Egoisu.dto.PartidoDto;
import Fifan.t.Egoisu.dto.ResultadoPartidoDto;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.exception.InvalidMatchException;
import Fifan.t.Egoisu.repositorios.JugadorRepositorio;
import Fifan.t.Egoisu.repositorios.PartidoRepositorio;
import Fifan.t.Egoisu.repositorios.SeleccionApuestaRepositorio;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartidoServicioTest {

    private PartidoRepositorio partidoRepo;
    private EquipoServicio equipoServicio;
    private JugadorRepositorio jugadorRepo;
    private SeleccionApuestaRepositorio seleccionRepo;
    private LiquidacionServicio liquidacion;
    private PartidoServicio servicio;

    private Equipo local;
    private Equipo visitante;
    private Jugador golLocal;
    private Jugador golVisitante;

    @BeforeEach
    void preparar() {
        partidoRepo = mock(PartidoRepositorio.class);
        equipoServicio = mock(EquipoServicio.class);
        jugadorRepo = mock(JugadorRepositorio.class);
        seleccionRepo = mock(SeleccionApuestaRepositorio.class);
        liquidacion = mock(LiquidacionServicio.class);
        servicio = new PartidoServicio(partidoRepo, equipoServicio, jugadorRepo, seleccionRepo, liquidacion);

        local = TestDatos.equipo(1, "Local");
        visitante = TestDatos.equipo(2, "Visitante");
        golLocal = TestDatos.jugador(10, "Goleador Local", local);
        golVisitante = TestDatos.jugador(20, "Goleador Visitante", visitante);
        when(jugadorRepo.findById(10L)).thenReturn(Optional.of(golLocal));
        when(jugadorRepo.findById(20L)).thenReturn(Optional.of(golVisitante));
        when(partidoRepo.save(any(Partido.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ResultadoPartidoDto resultado(int gl, int gv, int tl, int tv, int cl, int cv) {
        ResultadoPartidoDto d = new ResultadoPartidoDto();
        d.setGolesLocal(gl);
        d.setGolesVisitante(gv);
        d.setTirosLocal(tl);
        d.setTirosVisitante(tv);
        d.setCornersLocal(cl);
        d.setCornersVisitante(cv);
        return d;
    }

    private GolDto gol(long jugadorId, int minuto) {
        GolDto g = new GolDto();
        g.setJugadorId(jugadorId);
        g.setMinuto(minuto);
        return g;
    }

    private Partido partidoEnJuego() {
        Partido p = TestDatos.partido(1, local, visitante, EstadoPartido.EN_JUEGO, LocalDateTime.now().minusHours(1));
        when(partidoRepo.findById(1L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void registrarResultado_guardaTodoYLiquida() {
        Partido p = partidoEnJuego();
        ResultadoPartidoDto dto = resultado(1, 1, 5, 4, 3, 2);
        dto.getGoles().add(gol(10, 20));
        dto.getGoles().add(gol(20, 60));

        servicio.registrarResultado(1L, dto);

        assertEquals(EstadoPartido.FINALIZADO, p.getEstado());
        assertEquals(1, p.getGolesLocal());
        assertEquals(1, p.getGolesVisitante());
        assertEquals(2, p.getGoles().size());
        assertEquals(5, p.getEstadistica().getTirosLocal());
        assertTrue(local.isTieneExperiencia());
        verify(liquidacion).liquidar(p);
    }

    @Test
    void registrarResultado_unaVezFinalizadoNoSePuedeCambiar() {
        Partido p = partidoEnJuego();
        p.setEstado(EstadoPartido.FINALIZADO);

        assertThrows(InvalidMatchException.class, () -> servicio.registrarResultado(1L, resultado(0, 0, 0, 0, 0, 0)));
        verify(liquidacion, never()).liquidar(any(Partido.class));
    }

    @Test
    void registrarResultado_rechazaPartidoCancelado() {
        Partido p = partidoEnJuego();
        p.setEstado(EstadoPartido.CANCELADO);

        assertThrows(InvalidMatchException.class, () -> servicio.registrarResultado(1L, resultado(0, 0, 0, 0, 0, 0)));
    }

    @Test
    void registrarResultado_rechazaPartidoProgramadoQueNoHaComenzado() {
        Partido p = partidoEnJuego();
        p.setEstado(EstadoPartido.PROGRAMADO);
        p.setFechaHora(LocalDateTime.now().plusHours(2));

        assertThrows(InvalidMatchException.class, () -> servicio.registrarResultado(1L, resultado(0, 0, 0, 0, 0, 0)));
    }

    @Test
    void registrarResultado_aceptaPartidoIniciadoAntesDeLaHoraProgramada() {
        Partido p = partidoEnJuego();
        p.setFechaHora(LocalDateTime.now().plusHours(2)); // el admin lo inició antes: estado EN_JUEGO

        servicio.registrarResultado(1L, resultado(0, 0, 0, 0, 0, 0));

        assertEquals(EstadoPartido.FINALIZADO, p.getEstado());
    }

    @Test
    void registrarResultado_losTirosNoPuedenSerMenoresQueLosGoles() {
        partidoEnJuego();
        ResultadoPartidoDto dto = resultado(2, 0, 1, 0, 0, 0);
        dto.getGoles().add(gol(10, 5));
        dto.getGoles().add(gol(10, 15));

        assertThrows(InvalidMatchException.class, () -> servicio.registrarResultado(1L, dto));
        verify(liquidacion, never()).liquidar(any(Partido.class));
    }

    @Test
    void registrarResultado_losGoleadoresDebenCuadrarConElMarcador() {
        partidoEnJuego();
        ResultadoPartidoDto dto = resultado(2, 0, 5, 0, 0, 0);
        dto.getGoles().add(gol(10, 5)); // solo un goleador para 2 goles

        assertThrows(InvalidMatchException.class, () -> servicio.registrarResultado(1L, dto));
        verify(liquidacion, never()).liquidar(any(Partido.class));
    }

    @Test
    void registrarResultado_ignoraFilasVaciasDeGoles() {
        Partido p = partidoEnJuego();
        ResultadoPartidoDto dto = resultado(0, 0, 3, 2, 1, 1);
        dto.getGoles().add(new GolDto()); // fila vacía del formulario

        servicio.registrarResultado(1L, dto);

        assertEquals(0, p.getGoles().size());
        assertEquals(EstadoPartido.FINALIZADO, p.getEstado());
    }

    @Test
    void crear_rechazaEquiposIguales() {
        when(equipoServicio.obtener(1L)).thenReturn(local);
        PartidoDto dto = new PartidoDto();
        dto.setLocalId(1L);
        dto.setVisitanteId(1L);
        dto.setFechaHora(LocalDateTime.now().plusDays(1));

        assertThrows(InvalidMatchException.class, () -> servicio.crear(dto));
    }

    @Test
    void crear_rechazaFechasPasadas() {
        when(equipoServicio.obtener(1L)).thenReturn(local);
        when(equipoServicio.obtener(2L)).thenReturn(visitante);
        PartidoDto dto = new PartidoDto();
        dto.setLocalId(1L);
        dto.setVisitanteId(2L);
        dto.setFechaHora(LocalDateTime.now().minusMinutes(1));

        assertThrows(InvalidMatchException.class, () -> servicio.crear(dto));
    }

    @Test
    void cancelar_cancelaLasSeleccionesPendientes() {
        Partido p = TestDatos.partido(1, local, visitante, EstadoPartido.PROGRAMADO, LocalDateTime.now().plusDays(1));
        when(partidoRepo.findById(1L)).thenReturn(Optional.of(p));

        servicio.cancelar(1L);

        assertEquals(EstadoPartido.CANCELADO, p.getEstado());
        verify(liquidacion).cancelarSelecciones(p);
    }
}
