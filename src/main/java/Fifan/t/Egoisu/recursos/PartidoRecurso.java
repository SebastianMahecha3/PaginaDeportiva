package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.servicios.CatalogoMercadosServicio;
import Fifan.t.Egoisu.servicios.PartidoServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Consulta de partidos: próximos, anteriores y detalle (resultado, estadísticas, goleadores y mercados). */
@Controller
@RequiredArgsConstructor
public class PartidoRecurso {

    private final PartidoServicio partidoServicio;
    private final CatalogoMercadosServicio catalogo;

    @GetMapping("/partidos")
    public String listar(@RequestParam(defaultValue = "proximos") String vista, Model model) {
        boolean anteriores = "anteriores".equals(vista);
        model.addAttribute("vista", anteriores ? "anteriores" : "proximos");
        model.addAttribute("partidos", anteriores ? partidoServicio.anteriores() : partidoServicio.proximos());
        return "partidos/lista";
    }

    @GetMapping("/partidos/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Partido partido = partidoServicio.obtener(id);
        model.addAttribute("partido", partido);
        model.addAttribute("apuesta", catalogo.construirCompleto(partido));
        model.addAttribute("volver", "/partidos/" + id);
        return "partidos/detalle";
    }
}
