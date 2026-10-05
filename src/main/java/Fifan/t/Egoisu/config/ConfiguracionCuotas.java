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

    /**
     * Margen de la casa: cuota = 1 / (probabilidad * (1 + margen)). 0.05 = 5%.
     * Se aplica UNA sola vez. La comisión de la apuesta (apuestas.comision) es aparte y no entra en la cuota.
     */
    private BigDecimal margen = new BigDecimal("0.05");

    /** k del suavizado: peso del historial = n / (n + k), donde n = partidos jugados. */
    private int suavizadoK = 5;

    /** Límites de cuota para TODOS los mercados. */
    private BigDecimal cuotaMinima = new BigDecimal("1.10");
    private BigDecimal cuotaMaxima = new BigDecimal("10.00");

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