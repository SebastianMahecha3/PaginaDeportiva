package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.dto.SeleccionDto;
import Fifan.t.Egoisu.entidades.Partido;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * CONTRATO del motor de cuotas: probabilidad estimada -> probabilidad con margen -> cuota decimal.
 * Es el ÚNICO lugar donde vivirán las fórmulas. El resto de la aplicación solo llama a este método,
 * así el algoritmo se puede cambiar sin tocar Controllers ni ApuestaServicio.
 * Devuelve Optional.empty() si todavía no se puede ofrecer cuota para esa selección.
 */
public interface ServicioCuotas {

    Optional<BigDecimal> calcularCuota(Partido partido, SeleccionDto seleccion);
}
