package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.entidades.Apuesta;
import Fifan.t.Egoisu.entidades.enums.EstadoApuesta;
import Fifan.t.Egoisu.servicios.ApuestaServicio;
import Fifan.t.Egoisu.servicios.UsuarioServicio;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Perfil del usuario: datos básicos y resumen de sus apuestas. */
@Controller
@RequiredArgsConstructor
public class PerfilRecurso {

    private final UsuarioServicio usuarioServicio;
    private final ApuestaServicio apuestaServicio;

    @GetMapping("/perfil")
    public String perfil(Authentication auth, Model model) {
        List<Apuesta> apuestas = apuestaServicio.historial(auth.getName());
        model.addAttribute("usuario", usuarioServicio.obtenerPorUsername(auth.getName()));
        model.addAttribute("total", apuestas.size());
        model.addAttribute("ganadas", apuestas.stream().filter(a -> a.getEstado() == EstadoApuesta.GANADA).count());
        model.addAttribute("perdidas", apuestas.stream().filter(a -> a.getEstado() == EstadoApuesta.PERDIDA).count());
        model.addAttribute("pendientes", apuestas.stream().filter(a -> a.getEstado() == EstadoApuesta.PENDIENTE).count());
        return "perfil/perfil";
    }
}
