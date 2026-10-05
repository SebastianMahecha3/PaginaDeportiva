package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import Fifan.t.Egoisu.config.ConfiguracionApuestas;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalculadoraApuestaTest {

    private ConfiguracionApuestas config;
    private CalculadoraApuesta calculadora;

    @BeforeEach
    void preparar() {
        config = new ConfiguracionApuestas();
        calculadora = new CalculadoraApuesta(config);
    }

    private static void assertNumero(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void gananciaPotencialSimple_apuesta100Cuota2_da190() {
        assertNumero("5.00", calculadora.comision(new BigDecimal("100")));
        assertNumero("95.00", calculadora.montoNeto(new BigDecimal("100")));
        assertNumero("190.00", calculadora.gananciaPotencial(new BigDecimal("100"), new BigDecimal("2.00")));
    }

    @Test
    void parley_sumaLasCuotas() {
        BigDecimal cuota = calculadora.cuotaCombinada(
                List.of(new BigDecimal("1.50"), new BigDecimal("1.80"), new BigDecimal("2.00")));
        assertNumero("5.30", cuota);
    }

    @Test
    void apuestaSimple_cuotaCombinadaEsSuPropiaCuota() {
        assertNumero("2.35", calculadora.cuotaCombinada(List.of(new BigDecimal("2.35"))));
    }

    @Test
    void parley_sumaYLuegoAplicaElBonus() {
        config.setParleyBonusPorSeleccion(new BigDecimal("0.10"));
        BigDecimal base = calculadora.cuotaCombinada(List.of(new BigDecimal("1.50"), new BigDecimal("1.80")));
        BigDecimal bonus = calculadora.bonusParley(2);
        assertNumero("3.30", base);
        assertNumero("3.63", calculadora.aplicarBonus(base, bonus)); // (1.50 + 1.80) × 1.10
    }

    @Test
    void gananciaParley_usaMontoNetoPorCuotaTotal() {
        // 100 apostados -> neto 95.00 -> 95 * 5.40 = 513.00
        assertNumero("513.00", calculadora.gananciaPotencial(new BigDecimal("100"), new BigDecimal("5.40")));
    }

    @Test
    void redondeaConDosDecimalesHalfUp() {
        // neto 9.5 * 1.33 = 12.635 -> 12.64
        assertNumero("12.64", calculadora.gananciaPotencial(new BigDecimal("10"), new BigDecimal("1.33")));
    }

    @Test
    void apuestaSimpleNuncaTieneBonus() {
        config.setParleyBonusPorSeleccion(new BigDecimal("0.10"));
        assertNumero("0", calculadora.bonusParley(1));
    }

    @Test
    void bonusParley_esConfigurable() {
        config.setParleyBonusPorSeleccion(new BigDecimal("0.10"));
        BigDecimal bonus = calculadora.bonusParley(3);
        assertNumero("0.20", bonus);
        assertNumero("6.48", calculadora.aplicarBonus(new BigDecimal("5.40"), bonus));
    }

    @Test
    void sinBonusConfigurado_laCuotaNoCambia() {
        assertNumero("5.40", calculadora.aplicarBonus(new BigDecimal("5.40"), calculadora.bonusParley(3)));
    }

    @Test
    void comisionEsConfigurable() {
        config.setComision(new BigDecimal("0.10"));
        assertNumero("90.00", calculadora.montoNeto(new BigDecimal("100")));
    }
}