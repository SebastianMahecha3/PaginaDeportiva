package Fifan.t.Egoisu.recursos;

import Fifan.t.Egoisu.dto.UsuarioRegistroDto;
import Fifan.t.Egoisu.exception.InvalidUserException;
import Fifan.t.Egoisu.servicios.UsuarioServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Controller de login y registro. El inicio de sesión lo procesa Spring Security; aquí solo se muestran las vistas. */
@Controller
@RequiredArgsConstructor
public class AutenticacionRecurso {

    private final UsuarioServicio usuarioServicio;

    @GetMapping("/login")
    public String login() {
        return "login/login";
    }

    @GetMapping("/registro")
    public String formularioRegistro(Model model) {
        model.addAttribute("usuario", new UsuarioRegistroDto());
        return "registro/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("usuario") UsuarioRegistroDto dto, BindingResult resultado) {
        if (resultado.hasErrors()) {
            return "registro/registro";
        }
        try {
            usuarioServicio.registrar(dto);
        } catch (InvalidUserException e) {
            resultado.rejectValue(e.getCampo(), "usuario.invalido", e.getMessage());
            return "registro/registro";
        }
        return "redirect:/login?registrado";
    }
}
