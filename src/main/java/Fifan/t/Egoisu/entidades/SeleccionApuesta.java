package Fifan.t.Egoisu.entidades;

import Fifan.t.Egoisu.entidades.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una selección dentro de una apuesta: mercado + opción + cuota congelada.
 * Además de las relaciones (partido, jugador) guarda textos "snapshot" (partidoDescripcion, descripcion)
 * para que el historial no cambie aunque después se renombren equipos o jugadores.
 */
@Entity
@Table(name = "selecciones_apuesta")
@Getter @Setter @NoArgsConstructor
public class SeleccionApuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "apuesta_id", updatable = false)
    private Apuesta apuesta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partido_id", updatable = false)
    private Partido partido;

    @Column(nullable = false, updatable = false)
    private String partidoDescripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 15)
    private TipoMercado mercado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 15)
    private OpcionSeleccion opcion;

    /** Línea del mercado (ej. 2.5 goles). Null si el mercado no usa línea. */
    @Column(updatable = false, precision = 6, scale = 2)
    private BigDecimal linea;

    /** Equipo al que se refiere el mercado (TIROS / CORNERS). */
    @Enumerated(EnumType.STRING)
    @Column(updatable = false, length = 15)
    private LadoEquipo lado;

    /** Jugador elegido (mercado GOLEADOR). */
    @ManyToOne
    @JoinColumn(name = "jugador_id", updatable = false)
    private Jugador jugador;

    @Column(nullable = false, updatable = false)
    private String descripcion;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal cuota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoSeleccion estado = EstadoSeleccion.PENDIENTE;
}
