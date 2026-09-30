package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.entidades.Apuesta;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.SeleccionApuesta;
import Fifan.t.Egoisu.entidades.enums.*;
import Fifan.t.Egoisu.exception.InvalidMatchException;
import Fifan.t.Egoisu.repositorios.ApuestaRepositorio;
import Fifan.t.Egoisu.repositorios.SeleccionApuestaRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liquidación: cuando un partido termina (o se cancela) revisa las selecciones pendientes de ese partido,
 * decide si se cumplieron y recalcula el estado de cada apuesta afectada.
 * Regla de la apuesta: alguna selección PERDIDA -> PERDIDA (en un parley basta una); si no, alguna CANCELADA -> CANCELADA;
 * si todas GANADA -> GANADA; en otro caso sigue PENDIENTE (parley esperando otros partidos).
 * Se ejecuta dentro de la transacción de PartidoServicio, así todo se guarda junto o nada.
 */
@Service
@RequiredArgsConstructor
public class LiquidacionServicio {

    private final SeleccionApuestaRepositorio seleccionRepositorio;
    private final ApuestaRepositorio apuestaRepositorio;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void liquidar(Partido partido) {
        if (partido.getEstado() != EstadoPartido.FINALIZADO) {
            throw new InvalidMatchException("Solo se pueden liquidar partidos finalizados.");
        }
        List<SeleccionApuesta> pendientes = seleccionRepositorio.findByPartidoAndEstado(partido, EstadoSeleccion.PENDIENTE);
        Set<Apuesta> afectadas = new LinkedHashSet<>();
        for (SeleccionApuesta s : pendientes) {
            s.setEstado(evaluar(s, partido));
            afectadas.add(s.getApuesta());
        }
        actualizarApuestas(afectadas);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void cancelarSelecciones(Partido partido) {
        List<SeleccionApuesta> pendientes = seleccionRepositorio.findByPartidoAndEstado(partido, EstadoSeleccion.PENDIENTE);
        Set<Apuesta> afectadas = new LinkedHashSet<>();
        for (SeleccionApuesta s : pendientes) {
            s.setEstado(EstadoSeleccion.CANCELADA);
            afectadas.add(s.getApuesta());
        }
        actualizarApuestas(afectadas);
    }

    /** Decide si UNA selección se cumplió, según el marcador y las estadísticas del partido finalizado. */
    public EstadoSeleccion evaluar(SeleccionApuesta s, Partido p) {
        int golesLocal = p.getGolesLocal();
        int golesVisitante = p.getGolesVisitante();
        boolean cumple = switch (s.getMercado()) {
            case RESULTADO -> switch (s.getOpcion()) {
                case LOCAL -> golesLocal > golesVisitante;
                case EMPATE -> golesLocal == golesVisitante;
                case VISITANTE -> golesVisitante > golesLocal;
                default -> throw new IllegalStateException("Opción inválida para RESULTADO: " + s.getOpcion());
            };
            case GOLES -> superaLinea(golesLocal + golesVisitante, s);
            case GOLEADOR -> p.getGoles().stream()
                    .anyMatch(g -> g.getJugador().getId().equals(s.getJugador().getId()));
            case TIROS -> superaLinea(s.getLado() == LadoEquipo.LOCAL
                    ? p.getEstadistica().getTirosLocal() : p.getEstadistica().getTirosVisitante(), s);
            case CORNERS -> superaLinea(s.getLado() == LadoEquipo.LOCAL
                    ? p.getEstadistica().getCornersLocal() : p.getEstadistica().getCornersVisitante(), s);
        };
        return cumple ? EstadoSeleccion.GANADA : EstadoSeleccion.PERDIDA;
    }

    /** Estado global de una apuesta a partir del estado de todas sus selecciones. */
    public EstadoApuesta calcularEstado(Apuesta apuesta) {
        List<SeleccionApuesta> selecciones = apuesta.getSelecciones();
        if (selecciones.stream().anyMatch(s -> s.getEstado() == EstadoSeleccion.PERDIDA)) return EstadoApuesta.PERDIDA;
        if (selecciones.stream().anyMatch(s -> s.getEstado() == EstadoSeleccion.CANCELADA)) return EstadoApuesta.CANCELADA;
        if (selecciones.stream().allMatch(s -> s.getEstado() == EstadoSeleccion.GANADA)) return EstadoApuesta.GANADA;
        return EstadoApuesta.PENDIENTE;
    }

    private void actualizarApuestas(Collection<Apuesta> apuestas) {
        for (Apuesta a : apuestas) {
            EstadoApuesta nuevo = calcularEstado(a);
            if (nuevo != a.getEstado()) {
                a.setEstado(nuevo);
                if (nuevo != EstadoApuesta.PENDIENTE) {
                    a.setFechaLiquidacion(LocalDateTime.now());
                }
            }
            apuestaRepositorio.save(a);
        }
    }

    private boolean superaLinea(int valor, SeleccionApuesta s) {
        int comparacion = BigDecimal.valueOf(valor).compareTo(s.getLinea());
        return s.getOpcion() == OpcionSeleccion.MAS ? comparacion > 0 : comparacion < 0;
    }
}
