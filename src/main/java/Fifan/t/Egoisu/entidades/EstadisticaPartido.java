package Fifan.t.Egoisu.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Estadísticas de un partido finalizado (tiros y tiros de esquina). Las cuotas se calculan solo con estas cifras. */
@Entity
@Table(name = "estadisticas_partido")
@Getter @Setter @NoArgsConstructor
public class EstadisticaPartido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "partido_id", unique = true)
    private Partido partido;

    private int tirosLocal;
    private int tirosVisitante;
    private int cornersLocal;
    private int cornersVisitante;
}
