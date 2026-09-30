package Fifan.t.Egoisu.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Rendimiento de un equipo en un conjunto de partidos finalizados (todos, solo de local o solo de visitante). */
@Getter @AllArgsConstructor
public class RendimientoDto {
    private final int partidos;
    private final int ganados;
    private final int empatados;
    private final int perdidos;
    private final BigDecimal promGolesFavor;
    private final BigDecimal promGolesContra;
    private final BigDecimal promTiros;
    private final BigDecimal promTirosContra;
    private final BigDecimal promCorners;
    private final BigDecimal promCornersContra;
}
