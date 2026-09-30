package Fifan.t.Egoisu.dto;

import Fifan.t.Egoisu.entidades.Partido;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Un partido junto con sus mercados y cuotas (vacío si ya no admite apuestas). */
@Getter @AllArgsConstructor
public class PartidoApuestaDto {
    private final Partido partido;
    private final boolean apostable;
    private final List<GrupoMercadoDto> grupos;
}
