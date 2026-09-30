package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.dto.ApuestaFormDto;
import Fifan.t.Egoisu.dto.PartidoApuestaDto;
import Fifan.t.Egoisu.dto.ResumenBoletoDto;
import Fifan.t.Egoisu.dto.SeleccionDto;
import Fifan.t.Egoisu.entidades.Apuesta;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.exception.ReglaNegocioException;
import Fifan.t.Egoisu.servicios.ApuestaServicio;
import Fifan.t.Egoisu.servicios.BoletoApuestas;
import Fifan.t.Egoisu.servicios.CatalogoMercadosServicio;
import Fifan.t.Egoisu.servicios.PartidoServicio;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller de apuestas del usuario: mercados, boleto, vista previa, confirmación e historial.
 * Solo recibe peticiones y delega en ApuestaServicio; nunca calcula cuotas ni dinero.
 */
@Controller
@RequiredArgsConstructor
public class ApuestaRecurso {

    private final ApuestaServicio apuestaServicio;
    private final PartidoServicio partidoServicio;
    private final CatalogoMercadosServicio catalogo;
    private final BoletoApuestas boleto;

    @GetMapping("/apuestas")
    public String apuestas(Model model) {
        java.util.List<PartidoApuestaDto> partidos = partidoServicio.proximos().stream()
                .filter(p -> p.getEstado() == EstadoPartido.PROGRAMADO)
                .map(catalogo::construirCompleto)
                .filter(PartidoApuestaDto::isApostable)
                .toList();
        model.addAttribute("partidos", partidos);
        model.addAttribute("boleto", apuestaServicio.resumir(boleto.getSelecciones(), null));
        model.addAttribute("form", new ApuestaFormDto());
        model.addAttribute("volver", "/apuestas");
        return "apuestas/apuestas";
    }

    @PostMapping("/apuestas/boleto/agregar")
    public String agregar(@ModelAttribute SeleccionDto seleccion, BindingResult binding,
                          @RequestParam(required = false) String volver,
                          Authentication auth, RedirectAttributes ra) {
        String destino = destinoSeguro(volver);
        if (esAdmin(auth)) {
            ra.addFlashAttribute("error", "El administrador solo puede consultar los mercados, no apostar.");
            return destino;
        }
        if (binding.hasErrors()) {
            ra.addFlashAttribute("error", "La selección enviada no es válida.");
            return destino;
        }
        try {
            SeleccionDto normalizada = apuestaServicio.validarNuevaSeleccion(boleto.getSelecciones(), seleccion);
            boleto.agregar(normalizada);
            ra.addFlashAttribute("ok", "Selección añadida a tu boleto.");
        } catch (ReglaNegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return destino;
    }

    @PostMapping("/apuestas/boleto/quitar")
    public String quitar(@RequestParam int indice) {
        boleto.quitar(indice);
        return "redirect:/apuestas";
    }

    @PostMapping("/apuestas/boleto/vaciar")
    public String vaciar() {
        boleto.vaciar();
        return "redirect:/apuestas";
    }

    /** Paso "cantidad -> ganancia potencial": muestra el cálculo del servidor ANTES de confirmar. */
    @PostMapping("/apuestas/previsualizar")
    public String previsualizar(@Valid @ModelAttribute("form") ApuestaFormDto form, BindingResult binding,
                                Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            ra.addFlashAttribute("error", "Escribe una cantidad válida: un número mayor que 0 con máximo 2 decimales.");
            return "redirect:/apuestas";
        }
        ResumenBoletoDto resumen = apuestaServicio.resumir(boleto.getSelecciones(), form.getMonto());
        if (!resumen.isValido()) {
            ra.addFlashAttribute("error", resumen.getLineas().isEmpty()
                    ? "Tu boleto está vacío: elige al menos una selección."
                    : "Revisa tu boleto: alguna selección ya no está disponible.");
            return "redirect:/apuestas";
        }
        model.addAttribute("resumen", resumen);
        return "apuestas/previsualizar";
    }

    @PostMapping("/apuestas/confirmar")
    public String confirmar(@RequestParam BigDecimal monto,
                            @RequestParam(required = false) BigDecimal cuotaMostrada,
                            Authentication auth, RedirectAttributes ra) {
        try {
            Apuesta apuesta = apuestaServicio.confirmar(auth.getName(), boleto.getSelecciones(), monto, cuotaMostrada);
            boleto.vaciar();
            return "redirect:/apuestas/confirmacion/" + apuesta.getId();
        } catch (ReglaNegocioException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/apuestas";
        }
    }

    @GetMapping("/apuestas/confirmacion/{id}")
    public String confirmacion(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("apuesta", apuestaServicio.obtenerDeUsuario(id, auth.getName()));
        return "apuestas/confirmacion";
    }

    @GetMapping("/mis-apuestas")
    public String misApuestas(Authentication auth, Model model) {
        model.addAttribute("apuestas", apuestaServicio.historial(auth.getName()));
        return "apuestas/mis-apuestas";
    }

    // Solo se permiten destinos internos conocidos (evita redirecciones abiertas).
    private String destinoSeguro(String volver) {
        if ("/".equals(volver)) return "redirect:/";
        if (volver != null && volver.matches("^/partidos/\\d+$")) return "redirect:" + volver;
        return "redirect:/apuestas";
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
