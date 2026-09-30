package Fifan.t.Egoisu.entidades;

import Fifan.t.Egoisu.entidades.enums.EstadoJugador;
import Fifan.t.Egoisu.entidades.enums.PosicionJugador;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Jugador de un equipo. No se elimina: se desactiva, para conservar el historial de goles. */
@Entity
@Table(name = "jugadores")
@Getter @Setter @NoArgsConstructor
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private int dorsal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoJugador estado = EstadoJugador.ACTIVO;

    /** Posición en la cancha. Puede ser null en jugadores creados antes de existir este dato. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PosicionJugador posicion;

    /** Media del jugador (1 a 99, como en el FIFA). Puede ser null en jugadores antiguos. */
    private Integer media;

    public static final int MEDIA_POR_DEFECTO = 60;

    /** Posición a usar en cálculos: si no está definida se asume mediocampista. */
    public PosicionJugador posicionEfectiva() {
        return posicion != null ? posicion : PosicionJugador.MEDIOCAMPISTA;
    }

    /** Media a usar en cálculos: si no está definida se asume 60. */
    public int mediaEfectiva() {
        return media != null ? media : MEDIA_POR_DEFECTO;
    }
}
