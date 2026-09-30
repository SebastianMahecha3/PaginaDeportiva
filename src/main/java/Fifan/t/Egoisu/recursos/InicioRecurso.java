package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.dto.PartidoApuestaDto;
import Fifan.t.Egoisu.servicios.CatalogoMercadosServicio;
import Fifan.t.Egoisu.servicios.EquipoServicio;
import Fifan.t.Egoisu.servicios.PartidoServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Página principal: próximos partidos (con cuotas 1X2), resultados recientes y equipos. */
@Controller
@RequiredArgsConstructor
public class InicioRecurso {

    private final PartidoServicio partidoServicio;
    private final EquipoServicio equipoServicio;
    private final CatalogoMercadosServicio catalogo;

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("proximos", partidoServicio.proximosDestacados().stream()
                .map(catalogo::construirResumen).toList());
        model.addAttribute("recientes", partidoServicio.recientes());
        model.addAttribute("equipos", equipoServicio.listarActivos());
        model.addAttribute("volver", "/");
        return "inicio";
    }
}
