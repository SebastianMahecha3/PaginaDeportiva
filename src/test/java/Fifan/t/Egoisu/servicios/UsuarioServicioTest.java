package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Fifan.t.Egoisu.dto.UsuarioRegistroDto;
import Fifan.t.Egoisu.entidades.Usuario;
import Fifan.t.Egoisu.entidades.enums.Rol;
import Fifan.t.Egoisu.exception.InvalidUserException;
import Fifan.t.Egoisu.repositorios.UsuarioRepositorio;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServicioTest {

    private UsuarioRepositorio repositorio;
    private PasswordEncoder encoder;
    private UsuarioServicio servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepositorio.class);
        encoder = new Fifan.t.Egoisu.seguridad.BCryptConRespaldoEncoder();
        servicio = new UsuarioServicio(repositorio, encoder);
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private UsuarioRegistroDto dto(String usuario, String pass, String confirmar) {
        UsuarioRegistroDto d = new UsuarioRegistroDto();
        d.setUsername(usuario);
        d.setPassword(pass);
        d.setConfirmarPassword(confirmar);
        return d;
    }

    @Test
    void registrar_creaUsuarioConRolUserYContrasenaCifrada() {
        Usuario u = servicio.registrar(dto("ana", "clave12345", "clave12345"));

        assertEquals(Rol.ROLE_USER, u.getRol());
        assertNotEquals("clave12345", u.getPassword());
        assertTrue(encoder.matches("clave12345", u.getPassword()));
    }

    @Test
    void registrar_noPermiteElNombreDelAdministrador_sinImportarMayusculas() {
        InvalidUserException e = assertThrows(InvalidUserException.class,
                () -> servicio.registrar(dto("EGOISU", "clave12345", "clave12345")));
        assertEquals("username", e.getCampo());
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void registrar_rechazaContrasenasDistintas() {
        InvalidUserException e = assertThrows(InvalidUserException.class,
                () -> servicio.registrar(dto("ana", "clave12345", "otraClave")));
        assertEquals("confirmarPassword", e.getCampo());
    }

    @Test
    void registrar_rechazaUsuarioRepetido() {
        when(repositorio.existsByUsernameIgnoreCase("ana")).thenReturn(true);
        assertThrows(InvalidUserException.class, () -> servicio.registrar(dto("ana", "clave12345", "clave12345")));
    }

    @Test
    void asegurarAdministrador_creaAdminConPasswordConfigurada() {
        when(repositorio.findByUsernameIgnoreCase("Egoisu")).thenReturn(Optional.empty());

        Optional<String> generada = servicio.asegurarAdministrador("mi-clave-segura");

        assertTrue(generada.isEmpty());
        verify(repositorio).save(any(Usuario.class));
    }

    @Test
    void asegurarAdministrador_sinPasswordGeneraUnaAleatoria() {
        when(repositorio.findByUsernameIgnoreCase("Egoisu")).thenReturn(Optional.empty());

        Optional<String> generada = servicio.asegurarAdministrador("");

        assertTrue(generada.isPresent());
        assertEquals(16, generada.get().length());
    }

    @Test
    void asegurarAdministrador_reparaElRolSiEstabaMal() {
        Usuario existente = new Usuario();
        existente.setUsername("Egoisu");
        existente.setRol(Rol.ROLE_USER);
        when(repositorio.findByUsernameIgnoreCase("Egoisu")).thenReturn(Optional.of(existente));

        Optional<String> generada = servicio.asegurarAdministrador("x");

        assertFalse(generada.isPresent());
        assertEquals(Rol.ROLE_ADMIN, existente.getRol());
    }

    @Test
    void loadUserByUsername_devuelveLaAutoridadDelRol() {
        Usuario u = new Usuario();
        u.setUsername("ana");
        u.setPassword("hash");
        u.setRol(Rol.ROLE_USER);
        when(repositorio.findByUsernameIgnoreCase("ana")).thenReturn(Optional.of(u));

        var detalles = servicio.loadUserByUsername("ana");

        assertTrue(detalles.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void asegurarAdministrador_guardaLaContrasenaDeRespaldoCifrada() {
        when(repositorio.findByUsernameIgnoreCase("Egoisu")).thenReturn(Optional.empty());
        var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);

        servicio.asegurarAdministrador("principal-1234", "respaldo-1234");

        verify(repositorio).save(captor.capture());
        Usuario admin = captor.getValue();
        assertNotEquals("respaldo-1234", admin.getPasswordRespaldo());
        assertTrue(encoder.matches("respaldo-1234", admin.getPasswordRespaldo()));
        assertTrue(encoder.matches("principal-1234", admin.getPassword()));
    }

    @Test
    void asegurarAdministrador_agregaElRespaldoAUnAdminQueYaExistia() {
        Usuario existente = new Usuario();
        existente.setUsername("Egoisu");
        existente.setPassword(encoder.encode("principal-1234"));
        existente.setRol(Rol.ROLE_ADMIN);
        when(repositorio.findByUsernameIgnoreCase("Egoisu")).thenReturn(Optional.of(existente));

        servicio.asegurarAdministrador("", "respaldo-1234");

        assertTrue(encoder.matches("respaldo-1234", existente.getPasswordRespaldo()));
        assertTrue(encoder.matches("principal-1234", existente.getPassword())); // la principal no cambia
    }

    @Test
    void loadUserByUsername_delAdminIncluyeAmbasContrasenas() {
        Usuario admin = new Usuario();
        admin.setUsername("Egoisu");
        admin.setPassword(encoder.encode("principal-1234"));
        admin.setPasswordRespaldo(encoder.encode("respaldo-1234"));
        admin.setRol(Rol.ROLE_ADMIN);
        when(repositorio.findByUsernameIgnoreCase("Egoisu")).thenReturn(Optional.of(admin));

        var detalles = servicio.loadUserByUsername("Egoisu");

        assertTrue(encoder.matches("principal-1234", detalles.getPassword()));
        assertTrue(encoder.matches("respaldo-1234", detalles.getPassword()));
        assertFalse(encoder.matches("otra-clave", detalles.getPassword()));
    }
}
