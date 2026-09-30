package Fifan.t.Egoisu.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Una selección del boleto ya resuelta por el servidor (con su cuota actual) o con un mensaje de error. */
@Getter @AllArgsConstructor
public class LineaBoletoDto {
    private final SeleccionDto seleccion;
    private final String partido;
    private final String mercado;
    private final String descripcion;
    private final BigDecimal cuota;
    private final String error;

    public boolean isValida() { return error == null; }
}
