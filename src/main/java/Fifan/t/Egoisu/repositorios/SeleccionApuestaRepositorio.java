package Fifan.t.Egoisu.repositorios;

import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.SeleccionApuesta;
import Fifan.t.Egoisu.entidades.enums.EstadoSeleccion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de selecciones de apuesta (usado para liquidar y para saber si un partido ya tiene apuestas). */
public interface SeleccionApuestaRepositorio extends JpaRepository<SeleccionApuesta, Long> {
    List<SeleccionApuesta> findByPartidoAndEstado(Partido partido, EstadoSeleccion estado);
    boolean existsByPartidoId(Long partidoId);
}
