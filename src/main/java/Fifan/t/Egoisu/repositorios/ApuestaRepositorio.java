package Fifan.t.Egoisu.repositorios;

import Fifan.t.Egoisu.entidades.Apuesta;
import Fifan.t.Egoisu.entidades.enums.EstadoApuesta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de apuestas. */
public interface ApuestaRepositorio extends JpaRepository<Apuesta, Long> {
    List<Apuesta> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
    List<Apuesta> findAllByOrderByFechaDesc();
    long countByEstado(EstadoApuesta estado);
}
