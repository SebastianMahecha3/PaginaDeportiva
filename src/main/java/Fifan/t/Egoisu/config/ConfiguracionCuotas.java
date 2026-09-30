package Fifan.t.Egoisu.config;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Parámetros del motor de cuotas (application.properties, prefijo "cuotas"). */
@Component
@ConfigurationProperties(prefix = "cuotas")
@Getter @Setter
public class ConfiguracionCuotas {

    /** Margen de la casa: cuota = 1 / (probabilidad * (1 + margen)). 0.08 = 8%. */
    private BigDecimal margen = new BigDecimal("0.08");

    /** k del suavizado: peso del historial = n / (n + k), donde n = partidos jugados. */
    private int suavizadoK = 3;

    private BigDecimal cuotaMinima = new BigDecimal("1.05");
    private BigDecimal cuotaMaxima = new BigDecimal("50.00");

    /** Promedios de la liga cuando todavía no hay partidos (por equipo y por partido). */
    private BigDecimal golesLocalLiga = new BigDecimal("1.5");
    private BigDecimal golesVisitanteLiga = new BigDecimal("1.2");
    private BigDecimal tirosLiga = new BigDecimal("8");
    private BigDecimal cornersLiga = new BigDecimal("4");

    /** Máximo de goles por equipo considerados al calcular 1X2 (más allá la probabilidad es despreciable). */
    private int maxGolesMatriz = 12;

    /** Los cálculos de un partido se reutilizan este tiempo (evita repetir consultas por cada botón de apuesta). */
    private int cacheSegundos = 5;
}
