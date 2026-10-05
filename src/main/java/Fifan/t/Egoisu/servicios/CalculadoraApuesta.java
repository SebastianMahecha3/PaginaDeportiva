package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.config.ConfiguracionApuestas;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Matemática de dinero de las apuestas (todo en BigDecimal):
 *   montoNeto = apostado - apostado * comisión
 *   gananciaPotencial = montoNeto * cuotaTotal
 *   cuotaCombinada = SUMA de las cuotas individuales (una apuesta simple es su propia cuota)
 *   cuotaTotal = cuotaCombinada × (1 + bonus de parley)
 * No accede a la base de datos: la usa ApuestaServicio y se prueba de forma aislada.
 */
@Component
@RequiredArgsConstructor
public class CalculadoraApuesta {

    private final ConfiguracionApuestas config;

    public BigDecimal comision(BigDecimal monto) {
        return monto.multiply(config.getComision());
    }

    public BigDecimal montoNeto(BigDecimal monto) {
        return monto.subtract(comision(monto));
    }

    public BigDecimal gananciaPotencial(BigDecimal monto, BigDecimal cuotaTotal) {
        return montoNeto(monto).multiply(cuotaTotal).setScale(2, RoundingMode.HALF_UP);
    }

    /** Parley: cuota 1 + cuota 2 + cuota 3 + ... (ya no se multiplican). El bonus se aplica después con aplicarBonus. */
    public BigDecimal cuotaCombinada(List<BigDecimal> cuotas) {
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal c : cuotas) {
            suma = suma.add(c);
        }
        return suma.setScale(2, RoundingMode.HALF_UP);
    }

    /** Bonus como fracción: (selecciones - 1) * bonusPorSeleccion. Una apuesta simple nunca tiene bonus. */
    public BigDecimal bonusParley(int numeroSelecciones) {
        if (numeroSelecciones < 2) {
            return BigDecimal.ZERO;
        }
        return config.getParleyBonusPorSeleccion().multiply(BigDecimal.valueOf(numeroSelecciones - 1L));
    }

    public BigDecimal aplicarBonus(BigDecimal cuotaBase, BigDecimal bonus) {
        return cuotaBase.multiply(BigDecimal.ONE.add(bonus)).setScale(2, RoundingMode.HALF_UP);
    }
}