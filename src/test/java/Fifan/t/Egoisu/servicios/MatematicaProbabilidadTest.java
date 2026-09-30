package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MatematicaProbabilidadTest {

    private static void cerca(double esperado, BigDecimal real, double tolerancia) {
        assertEquals(esperado, real.doubleValue(), tolerancia);
    }

    @Test
    void exp_valoresConocidos() {
        cerca(1.0, MatematicaProbabilidad.exp(BigDecimal.ZERO), 1e-12);
        cerca(Math.E, MatematicaProbabilidad.exp(BigDecimal.ONE), 1e-12);
        cerca(1 / Math.E, MatematicaProbabilidad.exp(BigDecimal.ONE.negate()), 1e-12);
        cerca(Math.exp(-2.5), MatematicaProbabilidad.exp(new BigDecimal("-2.5")), 1e-12);
    }

    @Test
    void poisson_lasProbabilidadesSumanUno() {
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal p : MatematicaProbabilidad.pmf(40, new BigDecimal("2.5"))) {
            suma = suma.add(p);
        }
        cerca(1.0, suma, 1e-12);
    }

    @Test
    void poisson_acumuladaConocida() {
        // Poisson(2.5): P(X<=2) = 0.5438131...
        cerca(0.5438131158, MatematicaProbabilidad.cdf(2, new BigDecimal("2.5")), 1e-8);
        assertEquals(0, BigDecimal.ZERO.compareTo(MatematicaProbabilidad.cdf(-1, new BigDecimal("2.5"))));
    }

    @Test
    void masYMenosDeUnaLineaSumanUno() {
        BigDecimal lambda = new BigDecimal("2.7");
        BigDecimal mas = MatematicaProbabilidad.masQue(new BigDecimal("2.5"), lambda);
        BigDecimal menos = MatematicaProbabilidad.menosQue(new BigDecimal("2.5"), lambda);
        cerca(1.0, mas.add(menos), 1e-12);
    }

    @Test
    void resultado1X2_sumaUnoYEsSimetricoConLambdasIguales() {
        BigDecimal[] r = MatematicaProbabilidad.resultado1X2(new BigDecimal("1.4"), new BigDecimal("1.4"), 12);
        cerca(1.0, r[0].add(r[1]).add(r[2]), 1e-12);
        cerca(r[0].doubleValue(), r[2], 1e-12);
    }

    @Test
    void resultado1X2_elEquipoConMasGolesEsperadosEsFavorito() {
        BigDecimal[] r = MatematicaProbabilidad.resultado1X2(new BigDecimal("2.2"), new BigDecimal("0.8"), 12);
        assertTrue(r[0].compareTo(r[2]) > 0);
        assertTrue(r[0].compareTo(r[1]) > 0);
    }
}
