package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.dto.PartidoDto;
import Fifan.t.Egoisu.dto.ResultadoPartidoDto;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.exception.ReglaNegocioException;
import Fifan.t.Egoisu.servicios.EquipoServicio;
import Fifan.t.Egoisu.servicios.JugadorServicio;
import Fifan.t.Egoisu.servicios.PartidoServicio;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Fixtures y resultados (solo ADMIN). Crear/editar/iniciar/cancelar partidos y registrar el resultado final
 * (marcador, estadísticas y goleadores). La lógica real vive en PartidoServicio.
 */
@Controller
@RequiredArgsConstructor
public class AdminPartidoRecurso {

    private final PartidoServicio partidoServicio;
    private final EquipoServicio equipoServicio;
    private final JugadorServicio jugadorServicio;

    // ---------- fixtures ----------

    @GetMapping("/admin/partidos")
    public String listar(Model model) {
        model.addAttribute("partido", new PartidoDto());
        cargarListas(model);
        return "admin/partidos";
    }

    @PostMapping("/admin/partidos/crear")
    public String crear(@Valid @ModelAttribute("partido") PartidoDto dto, BindingResult binding,
                        Model model, RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                partidoServicio.crear(dto);
                ra.addFlashAttribute("ok", "Partido programado correctamente.");
                return "redirect:/admin/partidos";
            } catch (ReglaNegocioException e) {
                binding.reject("regla", e.getMessage());
            }
        }
        cargarListas(model);
        return "admin/partidos";
    }

    @GetMapping("/admin/partidos/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        Partido p = partidoServicio.obtener(id);
        PartidoDto dto = new PartidoDto();
        dto.setId(p.getId());
        dto.setLocalId(p.getLocal().getId());
        dto.setVisitanteId(p.getVisitante().getId());
        dto.setFechaHora(p.getFechaHora());
        model.addAttribute("partido", dto);
        model.addAttribute("equipos", equipoServicio.listarActivos());
        return "admin/partido-editar";
    }

    @PostMapping("/admin/partidos/{id}/editar")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("partido") PartidoDto dto,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                partidoServicio.actualizar(id, dto);
                ra.addFlashAttribute("ok", "Partido actualizado.");
                return "redirect:/admin/partidos";
            } catch (ReglaNegocioException e) {
                binding.reject("regla", e.getMessage());
            }
        }
        model.addAttribute("equipos", equipoServicio.listarActivos());
        return "admin/partido-editar";
    }

    @PostMapping("/admin/partidos/{id}/iniciar")
    public String iniciar(@PathVariable Long id, RedirectAttributes ra) {
        return accion(ra, "Partido iniciado: las apuestas quedaron cerradas.", "/admin/partidos", () -> partidoServicio.iniciar(id));
    }

    @PostMapping("/admin/partidos/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes ra) {
        return accion(ra, "Partido cancelado.", "/admin/partidos", () -> partidoServicio.cancelar(id));
    }

    // ---------- resultados ----------

    @GetMapping("/admin/resultados")
    public String resultados(Model model) {
        model.addAttribute("partidos", partidoServicio.proximos());
        return "admin/resultados";
    }

    @GetMapping("/admin/resultados/{id}")
    public String formularioResultado(@PathVariable Long id, Model model) {
        cargarFormularioResultado(model, partidoServicio.obtener(id));
        model.addAttribute("resultado", new ResultadoPartidoDto());
        return "admin/resultado-form";
    }

    @PostMapping("/admin/resultados/{id}")
    public String registrarResultado(@PathVariable Long id, @Valid @ModelAttribute("resultado") ResultadoPartidoDto dto,
                                     BindingResult binding, Model model, RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                partidoServicio.registrarResultado(id, dto);
                ra.addFlashAttribute("ok", "Resultado registrado y apuestas liquidadas.");
                return "redirect:/admin/resultados";
            } catch (ReglaNegocioException e) {
                binding.reject("regla", e.getMessage());
            }
        }
        cargarFormularioResultado(model, partidoServicio.obtener(id));
        return "admin/resultado-form";
    }

    // ---------- utilidades ----------

    private void cargarListas(Model model) {
        model.addAttribute("partidos", partidoServicio.todos());
        model.addAttribute("equipos", equipoServicio.listarActivos());
    }

    private void cargarFormularioResultado(Model model, Partido partido) {
        List<Jugador> jugadores = new ArrayList<>(jugadorServicio.activosDeEquipo(partido.getLocal().getId()));
        jugadores.addAll(jugadorServicio.activosDeEquipo(partido.getVisitante().getId()));
        model.addAttribute("partidoEntidad", partido);
        model.addAttribute("jugadores", jugadores);
    }

    private String accion(RedirectAttributes ra, String mensajeOk, String destino, Runnable operacion) {
        try {
            operacion.run();
            ra.addFlashAttribute("ok", mensajeOk);
        } catch (ReglaNegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + destino;
    }
}
