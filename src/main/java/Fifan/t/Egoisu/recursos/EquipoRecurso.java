package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.servicios.EquipoServicio;
import Fifan.t.Egoisu.servicios.EstadisticaServicio;
import Fifan.t.Egoisu.servicios.JugadorServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Consulta de equipos: listado y ficha con jugadores, historial y últimos partidos. */
@Controller
@RequiredArgsConstructor
public class EquipoRecurso {

    private final EquipoServicio equipoServicio;
    private final JugadorServicio jugadorServicio;
    private final EstadisticaServicio estadisticaServicio;

    @GetMapping("/equipos")
    public String listar(Model model) {
        model.addAttribute("equipos", equipoServicio.listarActivos());
        return "equipos/lista";
    }

    @GetMapping("/equipos/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Equipo equipo = equipoServicio.obtener(id);
        model.addAttribute("equipo", equipo);
        model.addAttribute("resumen", estadisticaServicio.resumen(equipo));
        model.addAttribute("jugadores", jugadorServicio.activosDeEquipo(id));
        model.addAttribute("ultimos", estadisticaServicio.ultimosPartidos(equipo, 5));
        return "equipos/detalle";
    }
}
