package Fifan.t.Egoisu.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Un bloque de opciones bajo un título (ej. "Goles totales"). */
@Getter @AllArgsConstructor
public class GrupoMercadoDto {
    private final String titulo;
    private final List<OpcionMercadoDto> opciones;
}
