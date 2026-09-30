package Fifan.t.Egoisu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** Una fila "goleador + minuto" del formulario de resultado. Las filas vacías se ignoran. */
@Getter @Setter
public class GolDto {

    private Long jugadorId;

    @Min(value = 0, message = "El minuto no puede ser negativo")
    @Max(value = 130, message = "El minuto máximo es 130")
    private Integer minuto;
}
