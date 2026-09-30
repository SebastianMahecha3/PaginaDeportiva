package Fifan.t.Egoisu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Formulario de resultado final: marcador, estadísticas y goleadores. Todos los números son enteros >= 0. */
@Getter @Setter
public class ResultadoPartidoDto {

    @NotNull(message = "Obligatorio") @Min(value = 0, message = "No puede ser negativo") @Max(value = 99, message = "Máximo 99")
    private Integer golesLocal;

    @NotNull(message = "Obligatorio") @Min(value = 0, message = "No puede ser negativo") @Max(value = 99, message = "Máximo 99")
    private Integer golesVisitante;

    @NotNull(message = "Obligatorio") @Min(value = 0, message = "No puede ser negativo") @Max(value = 200, message = "Máximo 200")
    private Integer tirosLocal;

    @NotNull(message = "Obligatorio") @Min(value = 0, message = "No puede ser negativo") @Max(value = 200, message = "Máximo 200")
    private Integer tirosVisitante;

    @NotNull(message = "Obligatorio") @Min(value = 0, message = "No puede ser negativo") @Max(value = 100, message = "Máximo 100")
    private Integer cornersLocal;

    @NotNull(message = "Obligatorio") @Min(value = 0, message = "No puede ser negativo") @Max(value = 100, message = "Máximo 100")
    private Integer cornersVisitante;

    @Valid
    private List<GolDto> goles = new ArrayList<>();
}
