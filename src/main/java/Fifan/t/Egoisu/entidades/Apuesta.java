package Fifan.t.Egoisu.entidades;

import Fifan.t.Egoisu.entidades.enums.EstadoApuesta;
import Fifan.t.Egoisu.entidades.enums.TipoApuesta;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Apuesta confirmada (SIMPLE = 1 selección, PARLEY = 2 o más).
 * Los datos económicos son un "snapshot" del momento de apostar (updatable = false): nada los cambia después.
 * Solo el estado y la fecha de liquidación se modifican, y únicamente desde LiquidacionServicio.
 */
@Entity
@Table(name = "apuestas")
@Getter @Setter @NoArgsConstructor
public class Apuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private TipoApuesta tipo;

    @Column(nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal montoApostado;

    @Column(nullable = false, updatable = false, precision = 6, scale = 4)
    private BigDecimal porcentajeComision;

    /** Producto de las cuotas individuales (antes del bonus). */
    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal cuotaBase;

    /** Bonus de parley como fracción (0.05 = +5%). 0 si no aplica. */
    @Column(nullable = false, updatable = false, precision = 6, scale = 4)
    private BigDecimal bonusParley;

    /** Cuota final usada para calcular la ganancia potencial. */
    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal cuotaTotal;

    @Column(nullable = false, updatable = false, precision = 16, scale = 2)
    private BigDecimal gananciaPotencial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoApuesta estado = EstadoApuesta.PENDIENTE;

    private LocalDateTime fechaLiquidacion;

    @OneToMany(mappedBy = "apuesta", cascade = CascadeType.ALL)
    private List<SeleccionApuesta> selecciones = new ArrayList<>();
}
