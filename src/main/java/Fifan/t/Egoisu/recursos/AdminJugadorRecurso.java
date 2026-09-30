package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.dto.JugadorDto;
import Fifan.t.Egoisu.entidades.Jugador;
import Fifan.t.Egoisu.entidades.enums.EstadoJugador;
import Fifan.t.Egoisu.entidades.enums.PosicionJugador;
import Fifan.t.Egoisu.exception.ReglaNegocioException;
import Fifan.t.Egoisu.servicios.EquipoServicio;
import Fifan.t.Egoisu.servicios.JugadorServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Gestión de jugadores (solo ADMIN): crear, editar, cambiar de equipo y desactivar. */
@Controller
@RequestMapping("/admin/jugadores")
@RequiredArgsConstructor
public class AdminJugadorRecurso {

    private final JugadorServicio jugadorServicio;
    private final EquipoServicio equipoServicio;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("jugador", new JugadorDto());
        cargarListas(model);
        return "admin/jugadores";
    }

    @PostMapping("/crear")
    public String crear(@Valid @ModelAttribute("jugador") JugadorDto dto, BindingResult binding,
                        Model model, RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                jugadorServicio.crear(dto);
                ra.addFlashAttribute("ok", "Jugador creado correctamente.");
                return "redirect:/admin/jugadores";
            } catch (ReglaNegocioException e) {
                binding.reject("regla", e.getMessage());
            }
        }
        cargarListas(model);
        return "admin/jugadores";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        Jugador j = jugadorServicio.obtener(id);
        JugadorDto dto = new JugadorDto();
        dto.setId(j.getId());
        dto.setNombre(j.getNombre());
        dto.setDorsal(j.getDorsal());
        dto.setEquipoId(j.getEquipo().getId());
        dto.setPosicion(j.posicionEfectiva());
        dto.setMedia(j.mediaEfectiva());
        model.addAttribute("jugador", dto);
        model.addAttribute("equipos", equipoServicio.listarActivos());
        model.addAttribute("posiciones", PosicionJugador.values());
        return "admin/jugador-editar";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("jugador") JugadorDto dto,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                jugadorServicio.actualizar(id, dto);
                ra.addFlashAttribute("ok", "Jugador actualizado.");
                return "redirect:/admin/jugadores";
            } catch (ReglaNegocioException e) {
                binding.reject("regla", e.getMessage());
            }
        }
        model.addAttribute("equipos", equipoServicio.listarActivos());
        model.addAttribute("posiciones", PosicionJugador.values());
        return "admin/jugador-editar";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoJugador estado, RedirectAttributes ra) {
        try {
            jugadorServicio.cambiarEstado(id, estado);
            ra.addFlashAttribute("ok", "Estado del jugador actualizado.");
        } catch (ReglaNegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/jugadores";
    }

    private void cargarListas(Model model) {
        model.addAttribute("jugadores", jugadorServicio.listar());
        model.addAttribute("equipos", equipoServicio.listarActivos());
        model.addAttribute("posiciones", PosicionJugador.values());
    }
}
