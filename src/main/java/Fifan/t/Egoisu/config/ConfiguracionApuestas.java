package Fifan.t.Egoisu.config;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Reglas de apuestas leídas de application.properties (prefijo "apuestas").
 * Así la comisión, las líneas de los mercados y el bonus de parley se cambian sin tocar el código.
 */
@Component
@ConfigurationProperties(prefix = "apuestas")
@Getter @Setter
public class ConfiguracionApuestas {

    /** Fracción descontada del dinero apostado (0.05 = 5%). */
    private BigDecimal comision = new BigDecimal("0.05");

    private List<BigDecimal> lineasGoles = List.of(new BigDecimal("1.5"), new BigDecimal("2.5"), new BigDecimal("3.5"));
    private List<BigDecimal> lineasTiros = List.of(new BigDecimal("4.5"), new BigDecimal("6.5"), new BigDecimal("8.5"));
    private List<BigDecimal> lineasCorners = List.of(new BigDecimal("2.5"), new BigDecimal("3.5"), new BigDecimal("4.5"));

    private int parleyMaximoSelecciones = 10;

    /** Bonus (fracción) por cada selección adicional a la primera. 0 = sin bonus. */
    private BigDecimal parleyBonusPorSeleccion = BigDecimal.ZERO;
}
