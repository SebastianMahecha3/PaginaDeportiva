package Fifan.t.Egoisu.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un gol de un partido, asociado al jugador que lo marcó. El equipo del gol es el equipo del jugador. */
@Entity
@Table(name = "goles")
@Getter @Setter @NoArgsConstructor
public class Gol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partido_id")
    private Partido partido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    @Column(nullable = false)
    private int minuto;
}
