package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.entidades.EstadisticaPartido;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.Gol;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.entidades.enums.PosicionJugador;
import java.time.LocalDateTime;

/** Fábrica de objetos de prueba (evita repetir código en los tests). */
final class TestDatos {

    private TestDatos() {}

    static Equipo equipo(long id, String nombre) {
        Equipo e = new Equipo();
        e.setId(id);
        e.setNombre(nombre);
        e.setValoracionInicial(5);
        return e;
    }

    static Jugador jugador(long id, String nombre, Equipo equipo) {
        Jugador j = new Jugador();
        j.setId(id);
        j.setNombre(nombre);
        j.setDorsal((int) id);
        j.setEquipo(equipo);
        return j;
    }

    static Jugador jugador(long id, String nombre, Equipo equipo, PosicionJugador posicion, int media) {
        Jugador j = jugador(id, nombre, equipo);
        j.setPosicion(posicion);
        j.setMedia(media);
        return j;
    }

    static Partido partido(long id, Equipo local, Equipo visitante, EstadoPartido estado, LocalDateTime fecha) {
        Partido p = new Partido();
        p.setId(id);
        p.setLocal(local);
        p.setVisitante(visitante);
        p.setEstado(estado);
        p.setFechaHora(fecha);
        return p;
    }

    /** Partido ya finalizado con marcador y estadísticas. */
    static Partido finalizado(long id, Equipo local, Equipo visitante, int golesL, int golesV,
                              int tirosL, int tirosV, int cornersL, int cornersV) {
        Partido p = partido(id, local, visitante, EstadoPartido.FINALIZADO, LocalDateTime.now().minusDays(1));
        p.setGolesLocal(golesL);
        p.setGolesVisitante(golesV);
        EstadisticaPartido e = new EstadisticaPartido();
        e.setPartido(p);
        e.setTirosLocal(tirosL);
        e.setTirosVisitante(tirosV);
        e.setCornersLocal(cornersL);
        e.setCornersVisitante(cornersV);
        p.setEstadistica(e);
        return p;
    }

    static Gol gol(Partido p, Jugador j, int minuto) {
        Gol g = new Gol();
        g.setPartido(p);
        g.setJugador(j);
        g.setMinuto(minuto);
        return g;
    }
}
