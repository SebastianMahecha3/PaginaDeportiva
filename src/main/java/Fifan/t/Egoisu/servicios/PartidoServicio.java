package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.dto.GolDto;
import Fifan.t.Egoisu.dto.PartidoDto;
import Fifan.t.Egoisu.dto.ResultadoPartidoDto;
import Fifan.t.Egoisu.entidades.*;
import Fifan.t.Egoisu.entidades.enums.EstadoEquipo;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.exception.InvalidMatchException;
import Fifan.t.Egoisu.exception.ResourceNotFoundException;
import Fifan.t.Egoisu.repositorios.JugadorRepositorio;
import Fifan.t.Egoisu.repositorios.PartidoRepositorio;
import Fifan.t.Egoisu.repositorios.SeleccionApuestaRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixtures y resultados. registrarResultado es UNA transacción:
 * valida -> guarda marcador, estadísticas y goleadores -> marca experiencia de los equipos -> liquida apuestas.
 * Si algo falla, no se guarda nada.
 */
@Service
@RequiredArgsConstructor
public class PartidoServicio {

    private static final List<EstadoPartido> ESTADOS_PROXIMOS = List.of(EstadoPartido.PROGRAMADO, EstadoPartido.EN_JUEGO);

    private final PartidoRepositorio partidoRepositorio;
    private final EquipoServicio equipoServicio;
    private final JugadorRepositorio jugadorRepositorio;
    private final SeleccionApuestaRepositorio seleccionRepositorio;
    private final LiquidacionServicio liquidacionServicio;

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public Partido obtener(Long id) {
        return partidoRepositorio.findById(id).orElseThrow(() -> new ResourceNotFoundException("Partido no encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Partido> proximos() { return partidoRepositorio.findByEstadoInOrderByFechaHoraAsc(ESTADOS_PROXIMOS); }

    @Transactional(readOnly = true)
    public List<Partido> proximosDestacados() { return partidoRepositorio.findTop6ByEstadoInOrderByFechaHoraAsc(ESTADOS_PROXIMOS); }

    @Transactional(readOnly = true)
    public List<Partido> anteriores() { return partidoRepositorio.findByEstadoOrderByFechaHoraDesc(EstadoPartido.FINALIZADO); }

    @Transactional(readOnly = true)
    public List<Partido> recientes() { return partidoRepositorio.findTop5ByEstadoOrderByFechaHoraDesc(EstadoPartido.FINALIZADO); }

    @Transactional(readOnly = true)
    public List<Partido> todos() { return partidoRepositorio.findAllByOrderByFechaHoraDesc(); }

    // ---------- administración ----------

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Partido crear(PartidoDto dto) {
        Equipo[] equipos = validar(dto);
        Partido partido = new Partido();
        partido.setLocal(equipos[0]);
        partido.setVisitante(equipos[1]);
        partido.setFechaHora(dto.getFechaHora());
        return partidoRepositorio.save(partido);
    }

    /** Solo partidos PROGRAMADOS. Cambiar los equipos no se permite si ya existen apuestas sobre el partido. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Partido actualizar(Long id, PartidoDto dto) {
        Partido partido = obtener(id);
        if (partido.getEstado() != EstadoPartido.PROGRAMADO) {
            throw new InvalidMatchException("Solo se pueden modificar partidos programados.");
        }
        Equipo[] equipos = validar(dto);
        boolean cambianEquipos = !partido.getLocal().getId().equals(equipos[0].getId())
                || !partido.getVisitante().getId().equals(equipos[1].getId());
        if (cambianEquipos && seleccionRepositorio.existsByPartidoId(id)) {
            throw new InvalidMatchException("No se pueden cambiar los equipos: ya hay apuestas sobre este partido.");
        }
        partido.setLocal(equipos[0]);
        partido.setVisitante(equipos[1]);
        partido.setFechaHora(dto.getFechaHora());
        return partidoRepositorio.save(partido);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Partido iniciar(Long id) {
        Partido partido = obtener(id);
        if (partido.getEstado() != EstadoPartido.PROGRAMADO) {
            throw new InvalidMatchException("Solo se puede iniciar un partido programado.");
        }
        partido.setEstado(EstadoPartido.EN_JUEGO);
        return partidoRepositorio.save(partido);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Partido cancelar(Long id) {
        Partido partido = obtener(id);
        if (partido.getEstado() == EstadoPartido.FINALIZADO || partido.getEstado() == EstadoPartido.CANCELADO) {
            throw new InvalidMatchException("Este partido ya está finalizado o cancelado.");
        }
        partido.setEstado(EstadoPartido.CANCELADO);
        partidoRepositorio.save(partido);
        liquidacionServicio.cancelarSelecciones(partido);
        return partido;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Partido registrarResultado(Long id, ResultadoPartidoDto dto) {
        Partido partido = obtener(id);
        if (partido.getEstado() == EstadoPartido.FINALIZADO) {
            throw new InvalidMatchException("El resultado de este partido ya fue registrado y no se puede modificar.");
        }
        if (partido.getEstado() == EstadoPartido.CANCELADO) {
            throw new InvalidMatchException("No se puede registrar el resultado de un partido cancelado.");
        }
        // Un partido EN_JUEGO ya comenzó (aunque el administrador lo haya iniciado antes de la hora programada).
        if (partido.getEstado() == EstadoPartido.PROGRAMADO && partido.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new InvalidMatchException("El partido aún no ha comenzado: presiona \"Iniciar\" o espera a su hora para registrar el resultado.");
        }
        validarNumeros(dto, partido);
        List<Gol> goles = construirGoles(partido, dto);

        partido.setGolesLocal(dto.getGolesLocal());
        partido.setGolesVisitante(dto.getGolesVisitante());

        EstadisticaPartido estadistica = new EstadisticaPartido();
        estadistica.setPartido(partido);
        estadistica.setTirosLocal(dto.getTirosLocal());
        estadistica.setTirosVisitante(dto.getTirosVisitante());
        estadistica.setCornersLocal(dto.getCornersLocal());
        estadistica.setCornersVisitante(dto.getCornersVisitante());
        partido.setEstadistica(estadistica);

        partido.getGoles().addAll(goles);
        partido.setEstado(EstadoPartido.FINALIZADO);

        // La valoración inicial se conserva; solo se marca que el equipo ya tiene historial.
        partido.getLocal().setTieneExperiencia(true);
        partido.getVisitante().setTieneExperiencia(true);

        partidoRepositorio.save(partido);
        liquidacionServicio.liquidar(partido);
        return partido;
    }

    /** Cada minuto pasa a EN_JUEGO los partidos programados cuya hora ya llegó (cierra las apuestas). */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cerrarPartidosIniciados() {
        partidoRepositorio.findByEstadoAndFechaHoraLessThanEqual(EstadoPartido.PROGRAMADO, LocalDateTime.now())
                .forEach(p -> p.setEstado(EstadoPartido.EN_JUEGO));
    }

    // ---------- validaciones internas ----------

    private Equipo[] validar(PartidoDto dto) {
        Equipo local = equipoServicio.obtener(dto.getLocalId());
        Equipo visitante = equipoServicio.obtener(dto.getVisitanteId());
        if (local.getId().equals(visitante.getId())) {
            throw new InvalidMatchException("El equipo local y el visitante deben ser distintos.");
        }
        if (local.getEstado() != EstadoEquipo.ACTIVO || visitante.getEstado() != EstadoEquipo.ACTIVO) {
            throw new InvalidMatchException("Solo se pueden programar partidos con equipos activos.");
        }
        if (dto.getFechaHora() == null || !dto.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new InvalidMatchException("La fecha y hora del partido deben ser futuras.");
        }
        return new Equipo[]{local, visitante};
    }

    private void validarNumeros(ResultadoPartidoDto dto, Partido partido) {
        Integer[] numeros = {dto.getGolesLocal(), dto.getGolesVisitante(), dto.getTirosLocal(), dto.getTirosVisitante(),
                dto.getCornersLocal(), dto.getCornersVisitante()};
        for (Integer n : numeros) {
            if (n == null || n < 0) {
                throw new InvalidMatchException("Goles, tiros y tiros de esquina deben ser números enteros mayores o iguales a 0.");
            }
        }
        if (dto.getTirosLocal() < dto.getGolesLocal()) {
            throw new InvalidMatchException("Los tiros de " + partido.getLocal().getNombre() + " no pueden ser menos que sus goles.");
        }
        if (dto.getTirosVisitante() < dto.getGolesVisitante()) {
            throw new InvalidMatchException("Los tiros de " + partido.getVisitante().getNombre() + " no pueden ser menos que sus goles.");
        }
    }

    /** Convierte las filas del formulario en goles y exige que los goleadores cuadren con el marcador. */
    private List<Gol> construirGoles(Partido partido, ResultadoPartidoDto dto) {
        List<Gol> goles = new java.util.ArrayList<>();
        int deLocal = 0;
        int deVisitante = 0;
        for (GolDto fila : dto.getGoles()) {
            if (fila.getJugadorId() == null && fila.getMinuto() == null) {
                continue; // fila vacía
            }
            if (fila.getJugadorId() == null || fila.getMinuto() == null) {
                throw new InvalidMatchException("Cada gol necesita un jugador y un minuto.");
            }
            if (fila.getMinuto() < 0 || fila.getMinuto() > 130) {
                throw new InvalidMatchException("El minuto de un gol debe estar entre 0 y 130.");
            }
            Jugador jugador = jugadorRepositorio.findById(fila.getJugadorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Jugador no encontrado."));
            Long equipoId = jugador.getEquipo().getId();
            if (equipoId.equals(partido.getLocal().getId())) {
                deLocal++;
            } else if (equipoId.equals(partido.getVisitante().getId())) {
                deVisitante++;
            } else {
                throw new InvalidMatchException(jugador.getNombre() + " no pertenece a ninguno de los equipos del partido.");
            }
            Gol gol = new Gol();
            gol.setPartido(partido);
            gol.setJugador(jugador);
            gol.setMinuto(fila.getMinuto());
            goles.add(gol);
        }
        if (deLocal != dto.getGolesLocal()) {
            throw new InvalidMatchException("Los goleadores de " + partido.getLocal().getNombre() + " (" + deLocal
                    + ") no coinciden con sus goles (" + dto.getGolesLocal() + ").");
        }
        if (deVisitante != dto.getGolesVisitante()) {
            throw new InvalidMatchException("Los goleadores de " + partido.getVisitante().getNombre() + " (" + deVisitante
                    + ") no coinciden con sus goles (" + dto.getGolesVisitante() + ").");
        }
        return goles;
    }
}
