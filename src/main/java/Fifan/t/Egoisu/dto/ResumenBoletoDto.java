package Fifan.t.Egoisu.dto;

import Fifan.t.Egoisu.entidades.enums.TipoApuesta;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resumen calculado por el servidor de un boleto: líneas, cuota combinada y, si se indicó un monto,
 * comisión, monto neto y ganancia potencial. Los campos económicos son null si el boleto no es válido.
 */
@Getter @AllArgsConstructor
public class ResumenBoletoDto {
    private final List<LineaBoletoDto> lineas;
    private final boolean valido;
    private final TipoApuesta tipo;
    private final BigDecimal cuotaBase;
    private final BigDecimal bonusParley;
    private final BigDecimal cuotaTotal;
    private final BigDecimal monto;
    private final BigDecimal comision;
    private final BigDecimal montoNeto;
    private final BigDecimal gananciaPotencial;
}
