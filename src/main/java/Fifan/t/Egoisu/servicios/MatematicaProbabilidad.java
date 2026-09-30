package Fifan.t.Egoisu.servicios;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Herramientas de probabilidad con BigDecimal (sin double), usadas por el motor de cuotas.
 * Distribución de Poisson: probabilidad de que ocurran k eventos (goles, tiros, corners) si se esperan λ.
 */
public final class MatematicaProbabilidad {

    /** Precisión de los cálculos intermedios (40 dígitos significativos). */
    public static final MathContext MC = new MathContext(40, RoundingMode.HALF_EVEN);
    private static final BigDecimal EPSILON = new BigDecimal("1e-45");

    private MatematicaProbabilidad() {}

    /** e^x calculado con la serie de Taylor. Para x negativo se usa 1 / e^(-x) (evita restas inestables). */
    public static BigDecimal exp(BigDecimal x) {
        if (x.signum() < 0) {
            return BigDecimal.ONE.divide(exp(x.negate()), MC);
        }
        BigDecimal termino = BigDecimal.ONE;
        BigDecimal suma = BigDecimal.ONE;
        for (int k = 1; k < 1000; k++) {
            termino = termino.multiply(x, MC).divide(BigDecimal.valueOf(k), MC);
            suma = suma.add(termino, MC);
            if (termino.compareTo(EPSILON) < 0) {
                break;
            }
        }
        return suma;
    }

    /** Probabilidades P(X = 0), P(X = 1), ..., P(X = max) de una Poisson con media lambda. */
    public static BigDecimal[] pmf(int max, BigDecimal lambda) {
        BigDecimal[] p = new BigDecimal[max + 1];
        p[0] = exp(lambda.negate());
        for (int k = 1; k <= max; k++) {
            p[k] = p[k - 1].multiply(lambda, MC).divide(BigDecimal.valueOf(k), MC);
        }
        return p;
    }

    /** P(X <= kMax). Si kMax es negativo la probabilidad es 0. */
    public static BigDecimal cdf(int kMax, BigDecimal lambda) {
        if (kMax < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal p : pmf(kMax, lambda)) {
            suma = suma.add(p, MC);
        }
        return limitar(suma);
    }

    /** P(X > linea), ej. "más de 2.5" = P(X >= 3). */
    public static BigDecimal masQue(BigDecimal linea, BigDecimal lambda) {
        int k = linea.setScale(0, RoundingMode.FLOOR).intValueExact();
        return limitar(BigDecimal.ONE.subtract(cdf(k, lambda), MC));
    }

    /** P(X < linea), ej. "menos de 2.5" = P(X <= 2). */
    public static BigDecimal menosQue(BigDecimal linea, BigDecimal lambda) {
        int k = linea.setScale(0, RoundingMode.CEILING).intValueExact() - 1;
        return cdf(k, lambda);
    }

    /**
     * Probabilidades {gana local, empate, gana visitante} a partir de los goles esperados de cada equipo,
     * suponiendo que los goles de cada uno son Poisson independientes. Se normaliza para que sumen 1.
     */
    public static BigDecimal[] resultado1X2(BigDecimal lambdaLocal, BigDecimal lambdaVisitante, int maxGoles) {
        BigDecimal[] pl = pmf(maxGoles, lambdaLocal);
        BigDecimal[] pv = pmf(maxGoles, lambdaVisitante);
        BigDecimal local = BigDecimal.ZERO;
        BigDecimal empate = BigDecimal.ZERO;
        BigDecimal visitante = BigDecimal.ZERO;
        for (int i = 0; i <= maxGoles; i++) {
            for (int j = 0; j <= maxGoles; j++) {
                BigDecimal p = pl[i].multiply(pv[j], MC);
                if (i > j) {
                    local = local.add(p, MC);
                } else if (i == j) {
                    empate = empate.add(p, MC);
                } else {
                    visitante = visitante.add(p, MC);
                }
            }
        }
        BigDecimal total = local.add(empate, MC).add(visitante, MC);
        return new BigDecimal[] {local.divide(total, MC), empate.divide(total, MC), visitante.divide(total, MC)};
    }

    /** Mantiene una probabilidad dentro de [0, 1] (por si el redondeo la deja apenas fuera). */
    public static BigDecimal limitar(BigDecimal p) {
        if (p.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return p.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : p;
    }
}
