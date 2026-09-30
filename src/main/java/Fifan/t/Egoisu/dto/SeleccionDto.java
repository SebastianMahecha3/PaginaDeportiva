package Fifan.t.Egoisu.dto;

import Fifan.t.Egoisu.entidades.enums.LadoEquipo;
import Fifan.t.Egoisu.entidades.enums.OpcionSeleccion;
import Fifan.t.Egoisu.entidades.enums.TipoMercado;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Describe QUÉ se quiere apostar (partido + mercado + opción + parámetros). NO contiene cuota:
 * la cuota siempre la calcula el servidor. Se guarda en el boleto de la sesión y también se usa
 * como entrada de ServicioCuotas.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SeleccionDto implements Serializable {

    private Long partidoId;
    private TipoMercado mercado;
    private OpcionSeleccion opcion;
    private BigDecimal linea;
    private LadoEquipo lado;
    private Long jugadorId;

    /** Igualdad "de negocio" (compara la línea con compareTo para que 2.5 == 2.50). */
    public boolean mismaSeleccionQue(SeleccionDto o) {
        if (o == null) return false;
        boolean lineaIgual = (linea == null && o.linea == null)
                || (linea != null && o.linea != null && linea.compareTo(o.linea) == 0);
        return java.util.Objects.equals(partidoId, o.partidoId) && mercado == o.mercado && opcion == o.opcion
                && lado == o.lado && java.util.Objects.equals(jugadorId, o.jugadorId) && lineaIgual;
    }
}
