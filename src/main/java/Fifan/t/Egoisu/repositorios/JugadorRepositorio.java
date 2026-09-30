package Fifan.t.Egoisu.repositorios;

import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.enums.EstadoJugador;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de jugadores. */
public interface JugadorRepositorio extends JpaRepository<Jugador, Long> {
    List<Jugador> findAllByOrderByEquipoNombreAscDorsalAsc();
    List<Jugador> findByEquipoIdOrderByDorsalAsc(Long equipoId);
    List<Jugador> findByEquipoIdAndEstadoOrderByDorsalAsc(Long equipoId, EstadoJugador estado);
    boolean existsByEquipoIdAndDorsalAndEstado(Long equipoId, int dorsal, EstadoJugador estado);
    boolean existsByEquipoIdAndDorsalAndEstadoAndIdNot(Long equipoId, int dorsal, EstadoJugador estado, Long id);
    long countByEstado(EstadoJugador estado);
}
