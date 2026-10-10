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
     * Primer partido (sin historial): cuántos partidos "equivale" la valoración inicial al suavizar con k.
     * Peso de la valoración = m / (m + k) = 5/10 = 50% con los valores por defecto; el resto se acerca al promedio de la liga.
     */
    private int valoracionPartidosEquivalentes = 5;

    /**
     * Límites de cuota para TODOS los mercados (1X2, goles, goleador, tiros y corners).
     * Todas las cuotas viven en la banda 1.10 - 1.50: el favorito de un partido queda cerca de 1.10 y el
     * resultado más improbable se acerca a 1.50 sin pasarlo.
     */
    private BigDecimal cuotaMinima = new BigDecimal("1.10");
    private BigDecimal cuotaMaxima = new BigDecimal("1.50");

    /**
     * Curva de compresión: convierte la cuota bruta 1/(p × (1 + margen)) en una cuota dentro de la banda.
     * Hasta compresionInicio la cuota no se toca (por debajo de ese valor queda en cuotaMinima); por encima, el exceso
     * se curva asintóticamente hacia cuotaMaxima, sin tocarla nunca:
     *   cuota = inicio + (maxima - inicio) × exceso / (exceso + escala),  exceso = cuotaBruta - inicio.
     * compresionInicio debe ser igual a cuotaMinima. Una escala MENOR sube las cuotas más rápido hacia 1.50
     * (cuotas más parecidas entre sí); una escala MAYOR las deja más cerca de 1.10 (más separación entre mercados).
     */
    private BigDecimal compresionInicio = new BigDecimal("1.10");
    private BigDecimal compresionEscala = new BigDecimal("2");

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