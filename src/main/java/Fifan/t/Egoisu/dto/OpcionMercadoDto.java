package Fifan.t.Egoisu.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Un botón de apuesta: texto, cuota vigente (null si aún no hay cuota) y la selección que se enviará al boleto. */
@Getter @AllArgsConstructor
public class OpcionMercadoDto {
    private final String etiqueta;
    private final BigDecimal cuota;
    private final SeleccionDto seleccion;
}
