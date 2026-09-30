package Fifan.t.Egoisu.entidades;

import Fifan.t.Egoisu.entidades.enums.EstadoEquipo;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Equipo del campeonato.
 * valoracionInicial (1-10) solo se usa para las cuotas mientras tieneExperiencia = false.
 * Al disputar su primer partido tieneExperiencia pasa a true, pero la valoración NO se borra (queda como dato histórico).
 */
@Entity
@Table(name = "equipos")
@Getter @Setter @NoArgsConstructor
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEquipo estado = EstadoEquipo.ACTIVO;

    @Column(nullable = false)
    private int valoracionInicial;

    @Column(nullable = false)
    private boolean tieneExperiencia = false;
}
