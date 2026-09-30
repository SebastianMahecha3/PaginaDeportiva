package Fifan.t.Egoisu.repositorios;

import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.enums.EstadoEquipo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de equipos. */
public interface EquipoRepositorio extends JpaRepository<Equipo, Long> {
    List<Equipo> findAllByOrderByNombreAsc();
    List<Equipo> findByEstadoOrderByNombreAsc(EstadoEquipo estado);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
