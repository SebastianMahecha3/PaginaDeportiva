package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.dto.SeleccionDto;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * "Boleto" del usuario: selecciones elegidas todavía sin confirmar. Vive en la sesión HTTP.
 * Solo guarda QUÉ se eligió, nunca cuotas ni dinero: esos datos los calcula siempre el servidor.
 */
@Component
@SessionScope
public class BoletoApuestas implements Serializable {

    private final List<SeleccionDto> selecciones = new ArrayList<>();

    public List<SeleccionDto> getSelecciones() { return Collections.unmodifiableList(new ArrayList<>(selecciones)); }

    public void agregar(SeleccionDto seleccion) { selecciones.add(seleccion); }

    public void quitar(int indice) {
        if (indice >= 0 && indice < selecciones.size()) {
            selecciones.remove(indice);
        }
    }

    public void vaciar() { selecciones.clear(); }
}
