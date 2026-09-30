package Fifan.t.Egoisu.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** Datos del formulario de equipos (crear / editar). */
@Getter @Setter
public class EquipoDto {

    private Long id;

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(max = 60, message = "Máximo 60 caracteres")
    private String nombre;

    @Size(max = 500, message = "Máximo 500 caracteres")
    private String descripcion;

    @NotNull(message = "La valoración inicial es obligatoria")
    @Min(value = 1, message = "La valoración mínima es 1")
    @Max(value = 10, message = "La valoración máxima es 10")
    private Integer valoracionInicial;
}
