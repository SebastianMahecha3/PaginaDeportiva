package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.entidades.enums.EstadoApuesta;
import Fifan.t.Egoisu.servicios.ApuestaServicio;
import Fifan.t.Egoisu.servicios.EquipoServicio;
import Fifan.t.Egoisu.servicios.JugadorServicio;
import Fifan.t.Egoisu.servicios.PartidoServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Panel del administrador (/admin/** exige ROLE_ADMIN en SeguridadConfig y los Services lo vuelven a exigir).
 * Muestra un resumen y la lista de apuestas en modo solo lectura: el admin no puede editar apuestas.
 */
@Controller
@RequiredArgsConstructor
public class AdminRecurso {

    private final EquipoServicio equipoServicio;
    private final JugadorServicio jugadorServicio;
    private final PartidoServicio partidoServicio;
    private final ApuestaServicio apuestaServicio;

    @GetMapping("/admin")
    public String panel(Model model) {
        model.addAttribute("totalEquipos", equipoServicio.listar().size());
        model.addAttribute("totalJugadores", jugadorServicio.listar().size());
        model.addAttribute("partidosPorJugar", partidoServicio.proximos().size());
        model.addAttribute("apuestasPendientes", apuestaServicio.listarTodas().stream()
                .filter(a -> a.getEstado() == EstadoApuesta.PENDIENTE).count());
        return "admin/panel";
    }

    @GetMapping("/admin/apuestas")
    public String apuestas(Model model) {
        model.addAttribute("apuestas", apuestaServicio.listarTodas());
        return "admin/apuestas";
    }
}
