package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.dto.EquipoDto;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.enums.EstadoEquipo;
import Fifan.t.Egoisu.exception.ReglaNegocioException;
import Fifan.t.Egoisu.servicios.EquipoServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Gestión de equipos (solo ADMIN): crear, editar, activar / desactivar. */
@Controller
@RequestMapping("/admin/equipos")
@RequiredArgsConstructor
public class AdminEquipoRecurso {

    private final EquipoServicio equipoServicio;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("equipos", equipoServicio.listar());
        model.addAttribute("equipo", new EquipoDto());
        return "admin/equipos";
    }

    @PostMapping("/crear")
    public String crear(@Valid @ModelAttribute("equipo") EquipoDto dto, BindingResult binding,
                        Model model, RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                equipoServicio.crear(dto);
                ra.addFlashAttribute("ok", "Equipo creado correctamente.");
                return "redirect:/admin/equipos";
            } catch (ReglaNegocioException e) {
                binding.reject("regla", e.getMessage());
            }
        }
        model.addAttribute("equipos", equipoServicio.listar());
        return "admin/equipos";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        Equipo equipo = equipoServicio.obtener(id);
        EquipoDto dto = new EquipoDto();
        dto.setId(equipo.getId());
        dto.setNombre(equipo.getNombre());
        dto.setDescripcion(equipo.getDescripcion());
        dto.setValoracionInicial(equipo.getValoracionInicial());
        model.addAttribute("equipo", dto);
        model.addAttribute("equipoEntidad", equipo);
        return "admin/equipo-editar";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("equipo") EquipoDto dto,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                equipoServicio.actualizar(id, dto);
                ra.addFlashAttribute("ok", "Equipo actualizado.");
                return "redirect:/admin/equipos";
            } catch (ReglaNegocioException e) {
                binding.reject("regla", e.getMessage());
            }
        }
        model.addAttribute("equipoEntidad", equipoServicio.obtener(id));
        return "admin/equipo-editar";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoEquipo estado, RedirectAttributes ra) {
        equipoServicio.cambiarEstado(id, estado);
        ra.addFlashAttribute("ok", "Estado del equipo actualizado.");
        return "redirect:/admin/equipos";
    }
}
