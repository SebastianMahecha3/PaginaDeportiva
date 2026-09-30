package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.dto.JugadorDto;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.enums.EstadoEquipo;
import Fifan.t.Egoisu.entidades.enums.EstadoJugador;
import Fifan.t.Egoisu.exception.ReglaNegocioException;
import Fifan.t.Egoisu.exception.ResourceNotFoundException;
import Fifan.t.Egoisu.repositorios.JugadorRepositorio;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lógica de jugadores: crear, editar, cambiar de equipo y desactivar (nunca se borran). */
@Service
@RequiredArgsConstructor
public class JugadorServicio {

    private final JugadorRepositorio jugadorRepositorio;
    private final EquipoServicio equipoServicio;

    @Transactional(readOnly = true)
    public List<Jugador> listar() { return jugadorRepositorio.findAllByOrderByEquipoNombreAscDorsalAsc(); }

    @Transactional(readOnly = true)
    public List<Jugador> activosDeEquipo(Long equipoId) {
        return jugadorRepositorio.findByEquipoIdAndEstadoOrderByDorsalAsc(equipoId, EstadoJugador.ACTIVO);
    }

    @Transactional(readOnly = true)
    public Jugador obtener(Long id) {
        return jugadorRepositorio.findById(id).orElseThrow(() -> new ResourceNotFoundException("Jugador no encontrado."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Jugador crear(JugadorDto dto) {
        Equipo equipo = equipoServicio.obtener(dto.getEquipoId());
        validar(dto, equipo, null);
        Jugador jugador = new Jugador();
        jugador.setNombre(dto.getNombre().trim());
        jugador.setDorsal(dto.getDorsal());
        jugador.setEquipo(equipo);
        jugador.setPosicion(dto.getPosicion());
        jugador.setMedia(dto.getMedia());
        return jugadorRepositorio.save(jugador);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Jugador actualizar(Long id, JugadorDto dto) {
        Jugador jugador = obtener(id);
        Equipo equipo = equipoServicio.obtener(dto.getEquipoId());
        if (jugador.getEstado() == EstadoJugador.ACTIVO) {
            validar(dto, equipo, id);
        }
        jugador.setNombre(dto.getNombre().trim());
        jugador.setDorsal(dto.getDorsal());
        jugador.setEquipo(equipo);
        jugador.setPosicion(dto.getPosicion());
        jugador.setMedia(dto.getMedia());
        return jugadorRepositorio.save(jugador);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Jugador cambiarEstado(Long id, EstadoJugador estado) {
        Jugador jugador = obtener(id);
        if (estado == EstadoJugador.ACTIVO && jugadorRepositorio.existsByEquipoIdAndDorsalAndEstadoAndIdNot(
                jugador.getEquipo().getId(), jugador.getDorsal(), EstadoJugador.ACTIVO, id)) {
            throw new ReglaNegocioException("No se puede activar: el dorsal ya lo usa otro jugador activo del equipo.");
        }
        jugador.setEstado(estado);
        return jugadorRepositorio.save(jugador);
    }

    private void validar(JugadorDto dto, Equipo equipo, Long idActual) {
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new ReglaNegocioException("El nombre del jugador es obligatorio.");
        }
        if (dto.getDorsal() == null || dto.getDorsal() < 1 || dto.getDorsal() > 99) {
            throw new ReglaNegocioException("El dorsal debe estar entre 1 y 99.");
        }
        if (dto.getPosicion() == null) {
            throw new ReglaNegocioException("Selecciona la posición del jugador.");
        }
        if (dto.getMedia() == null || dto.getMedia() < 1 || dto.getMedia() > 99) {
            throw new ReglaNegocioException("La media del jugador debe estar entre 1 y 99.");
        }
        if (equipo.getEstado() != EstadoEquipo.ACTIVO) {
            throw new ReglaNegocioException("No se pueden asignar jugadores a un equipo inactivo.");
        }
        boolean ocupado = idActual == null
                ? jugadorRepositorio.existsByEquipoIdAndDorsalAndEstado(equipo.getId(), dto.getDorsal(), EstadoJugador.ACTIVO)
                : jugadorRepositorio.existsByEquipoIdAndDorsalAndEstadoAndIdNot(equipo.getId(), dto.getDorsal(), EstadoJugador.ACTIVO, idActual);
        if (ocupado) {
            throw new ReglaNegocioException("Ese dorsal ya lo usa otro jugador activo del equipo.");
        }
    }
}
