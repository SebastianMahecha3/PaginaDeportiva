package Fifan.t.Egoisu.recursos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Fifan.t.Egoisu.seguridad.SeguridadConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Prueba de la PRIMERA barrera de seguridad (reglas de URL de SeguridadConfig) con controllers de mentira.
 * No levanta la aplicación completa ni la base de datos.
 */
@SpringJUnitWebConfig(SeguridadWebTest.Config.class)
class SeguridadWebTest {

    @Configuration
    @EnableWebMvc
    @Import(SeguridadConfig.class)
    static class Config {
        @RestController
        static class Falso {
            @GetMapping("/") String inicio() { return "inicio"; }
            @GetMapping("/login") String login() { return "login"; }
            @GetMapping("/partidos") String partidos() { return "partidos"; }
            @GetMapping("/equipos") String equipos() { return "equipos"; }
            @GetMapping("/perfil") String perfil() { return "perfil"; }
            @GetMapping("/apuestas") String apuestas() { return "apuestas"; }
            @GetMapping("/admin") String admin() { return "admin"; }
            @GetMapping("/admin/equipos") String adminEquipos() { return "admin equipos"; }
            @PostMapping("/registro") String registro() { return "registro"; }
        }
    }

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void visitante_puedeVerInicioPartidosEquiposYLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/partidos")).andExpect(status().isOk());
        mvc.perform(get("/equipos")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void visitante_esEnviadoAlLoginEnRutasPrivadas() throws Exception {
        mvc.perform(get("/apuestas")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/perfil")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    void usuarioNormal_noPuedeEntrarAlAdmin() throws Exception {
        mvc.perform(get("/admin").with(user("ana").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/equipos").with(user("ana").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void usuarioNormal_puedeVerSusRutas() throws Exception {
        mvc.perform(get("/apuestas").with(user("ana").roles("USER"))).andExpect(status().isOk());
        mvc.perform(get("/perfil").with(user("ana").roles("USER"))).andExpect(status().isOk());
    }

    @Test
    void administrador_puedeEntrarAlPanel() throws Exception {
        mvc.perform(get("/admin").with(user("Egoisu").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/equipos").with(user("Egoisu").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void csrf_unPostSinTokenEsRechazado() throws Exception {
        mvc.perform(post("/registro")).andExpect(status().isForbidden());
    }
}
