package Fifan.t.Egoisu.config;

import Fifan.t.Egoisu.servicios.UsuarioServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, garantiza que exista el administrador "Egoisu" con ROLE_ADMIN.
 * Contraseña: variable ADMIN_PASSWORD; si no existe, se genera una aleatoria y se imprime una sola vez.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UsuarioServicio usuarioServicio;

    @Value("${app.admin.password:}")
    private String passwordAdmin;

    /** Contraseña de respaldo del administrador (ADMIN_PASSWORD_RESPALDO). Vacía = sin respaldo. */
    @Value("${app.admin.password-respaldo:}")
    private String passwordRespaldo;

    @Override
    public void run(ApplicationArguments args) {
        usuarioServicio.asegurarAdministrador(passwordAdmin, passwordRespaldo).ifPresent(generada ->
                log.warn("\n==============================================================\n"
                        + " Administrador creado -> usuario: {}  contraseña: {}\n"
                        + " (Se muestra solo esta vez. Defínela con ADMIN_PASSWORD si prefieres.)\n"
                        + "==============================================================",
                        UsuarioServicio.USERNAME_ADMIN, generada));
    }
}
