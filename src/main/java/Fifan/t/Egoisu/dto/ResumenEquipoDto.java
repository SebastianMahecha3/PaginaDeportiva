package Fifan.t.Egoisu.dto;

import Fifan.t.Egoisu.entidades.Equipo;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Datos históricos de un equipo (solo partidos FINALIZADOS). Es la materia prima que usará ServicioCuotas. */
@Getter @AllArgsConstructor
public class ResumenEquipoDto {
    private final Equipo equipo;
    private final RendimientoDto general;
    private final RendimientoDto comoLocal;
    private final RendimientoDto comoVisitante;
}
