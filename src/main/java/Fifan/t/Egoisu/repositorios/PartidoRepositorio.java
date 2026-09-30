package Fifan.t.Egoisu.repositorios;

import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de partidos. */
public interface PartidoRepositorio extends JpaRepository<Partido, Long> {

    List<Partido> findAllByOrderByFechaHoraDesc();

    List<Partido> findByEstadoInOrderByFechaHoraAsc(Collection<EstadoPartido> estados);

    List<Partido> findTop6ByEstadoInOrderByFechaHoraAsc(Collection<EstadoPartido> estados);

    List<Partido> findByEstadoOrderByFechaHoraDesc(EstadoPartido estado);

    List<Partido> findTop5ByEstadoOrderByFechaHoraDesc(EstadoPartido estado);

    /** Partidos programados cuya hora ya llegó (los cierra el planificador). */
    List<Partido> findByEstadoAndFechaHoraLessThanEqual(EstadoPartido estado, LocalDateTime limite);

    long countByEstado(EstadoPartido estado);

    /** Partidos finalizados de un equipo, con estadísticas cargadas (base del historial para las cuotas). */
    @Query("select p from Partido p join fetch p.local join fetch p.visitante left join fetch p.estadistica "
            + "where p.estado = :estado and (p.local.id = :equipoId or p.visitante.id = :equipoId) "
            + "order by p.fechaHora desc")
    List<Partido> findFinalizadosDeEquipo(@Param("equipoId") Long equipoId, @Param("estado") EstadoPartido estado);

    /** Todos los partidos finalizados con estadísticas (para promedios de la liga). */
    @Query("select p from Partido p join fetch p.local join fetch p.visitante left join fetch p.estadistica "
            + "where p.estado = :estado")
    List<Partido> findFinalizadosConEstadisticas(@Param("estado") EstadoPartido estado);
}
