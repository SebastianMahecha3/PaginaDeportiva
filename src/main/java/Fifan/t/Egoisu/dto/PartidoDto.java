package Fifan.t.Egoisu.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Datos del formulario de partidos (crear / editar fixture). La fecha y hora vienen de un input datetime-local. */
@Getter @Setter
public class PartidoDto {

    private Long id;

    @NotNull(message = "Selecciona el equipo local")
    private Long localId;

    @NotNull(message = "Selecciona el equipo visitante")
    private Long visitanteId;

    @NotNull(message = "La fecha y hora son obligatorias")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaHora;
}
