package Fifan.t.Egoisu.dto;

import jakarta.validation.constraints.*;
import Fifan.t.Egoisu.entidades.enums.PosicionJugador;
import lombok.Getter;
import lombok.Setter;

/** Datos del formulario de jugadores (crear / editar). */
@Getter @Setter
public class JugadorDto {

    private Long id;

    @NotBlank(message = "El nombre del jugador es obligatorio")
    @Size(max = 80, message = "Máximo 80 caracteres")
    private String nombre;

    @NotNull(message = "El dorsal es obligatorio")
    @Min(value = 1, message = "El dorsal mínimo es 1")
    @Max(value = 99, message = "El dorsal máximo es 99")
    private Integer dorsal;

    @NotNull(message = "Selecciona un equipo")
    private Long equipoId;

    @NotNull(message = "Selecciona la posición del jugador")
    private PosicionJugador posicion;

    @NotNull(message = "La media es obligatoria")
    @Min(value = 1, message = "La media mínima es 1")
    @Max(value = 99, message = "La media máxima es 99")
    private Integer media;
}
