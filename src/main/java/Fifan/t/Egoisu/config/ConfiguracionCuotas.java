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

    /**
     * Límites de cuota para TODOS los mercados (se aplican después de calcular la cuota).
     * El máximo baja de 10.00 a 5.00 para que las probabilidades pequeñas no generen cuotas enormes.
     */
    private BigDecimal cuotaMinima = new BigDecimal("1.10");
    private BigDecimal cuotaMaxima = new BigDecimal("5.00");

    /**
     * Factor de posición del mercado GOLEADOR: multiplica la probabilidad de gol del jugador (Poisson).
     * Más probabilidad = menos cuota, así que delantero < mediocampista < defensa < portero en cuota.
     * Moderados a propósito: el reparto por peso ya diferencia posiciones, y así el ajuste no dispara cuotas.
     */
    private BigDecimal factorPosicionDelantero = new BigDecimal("1.15");
    private BigDecimal factorPosicionMediocampista = new BigDecimal("1.00");
    private BigDecimal factorPosicionDefensa = new BigDecimal("0.90");
    private BigDecimal factorPosicionPortero = new BigDecimal("0.60");

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