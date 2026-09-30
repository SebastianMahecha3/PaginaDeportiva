package Fifan.t.Egoisu.entidades;

import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Partido (fixture). Guarda el marcador final (goles) y, por relación, sus estadísticas y goleadores.
 * Los goles son null mientras el partido no esté FINALIZADO.
 */
@Entity
@Table(name = "partidos")
@Getter @Setter @NoArgsConstructor
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "local_id")
    private Equipo local;

    @ManyToOne(optional = false)
    @JoinColumn(name = "visitante_id")
    private Equipo visitante;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPartido estado = EstadoPartido.PROGRAMADO;

    private Integer golesLocal;

    private Integer golesVisitante;

    @OneToOne(mappedBy = "partido", cascade = CascadeType.ALL, orphanRemoval = true)
    private EstadisticaPartido estadistica;

    @OneToMany(mappedBy = "partido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minuto ASC")
    private List<Gol> goles = new ArrayList<>();

    /** Regla única de "apuestas abiertas": programado y todavía no ha llegado su hora. */
    public boolean admiteApuestas(LocalDateTime ahora) {
        return estado == EstadoPartido.PROGRAMADO && fechaHora.isAfter(ahora);
    }

    public String getDescripcion() {
        return local.getNombre() + " vs " + visitante.getNombre();
    }
}
