package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.dto.UsuarioRegistroDto;
import Fifan.t.Egoisu.entidades.Usuario;
import Fifan.t.Egoisu.entidades.enums.Rol;
import Fifan.t.Egoisu.exception.InvalidUserException;
import Fifan.t.Egoisu.exception.ResourceNotFoundException;
import Fifan.t.Egoisu.repositorios.UsuarioRepositorio;
import Fifan.t.Egoisu.seguridad.BCryptConRespaldoEncoder;
import java.security.SecureRandom;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de usuarios y carga de credenciales para Spring Security.
 * Regla clave: el rol NUNCA viene del formulario; "Egoisu" solo puede existir como administrador.
 */
@Service
@RequiredArgsConstructor
public class UsuarioServicio implements UserDetailsService {

    public static final String USERNAME_ADMIN = "Egoisu";
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario registrar(UsuarioRegistroDto dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (username.isEmpty()) {
            throw new InvalidUserException("username", "El usuario es obligatorio.");
        }
        if (username.equalsIgnoreCase(USERNAME_ADMIN)) {
            throw new InvalidUserException("username", "Ese nombre de usuario no está disponible.");
        }
        if (dto.getPassword() == null || !dto.getPassword().equals(dto.getConfirmarPassword())) {
            throw new InvalidUserException("confirmarPassword", "Las contraseñas no coinciden.");
        }
        if (usuarioRepositorio.existsByUsernameIgnoreCase(username)) {
            throw new InvalidUserException("username", "Ese nombre de usuario ya está en uso.");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(Rol.ROLE_USER);
        try {
            return usuarioRepositorio.save(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new InvalidUserException("username", "Ese nombre de usuario ya está en uso.");
        }
    }

    /** Igual que la versión con respaldo, pero sin contraseña de respaldo. */
    @Transactional
    public Optional<String> asegurarAdministrador(String passwordConfigurada) {
        return asegurarAdministrador(passwordConfigurada, null);
    }

    /**
     * Crea (o repara) al administrador. Devuelve la contraseña solo si tuvo que generarla al azar.
     * Si se indica una contraseña de respaldo, el administrador podrá entrar con ella ADEMÁS de la principal;
     * se sincroniza en cada arranque (si cambias la variable, se actualiza).
     */
    @Transactional
    public Optional<String> asegurarAdministrador(String passwordConfigurada, String passwordRespaldo) {
        Optional<Usuario> existente = usuarioRepositorio.findByUsernameIgnoreCase(USERNAME_ADMIN);
        Optional<String> generada = Optional.empty();
        Usuario admin;
        if (existente.isPresent()) {
            admin = existente.get();
            admin.setRol(Rol.ROLE_ADMIN);
        } else {
            boolean generar = passwordConfigurada == null || passwordConfigurada.isBlank();
            String password = generar ? generarPassword() : passwordConfigurada;
            admin = new Usuario();
            admin.setUsername(USERNAME_ADMIN);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRol(Rol.ROLE_ADMIN);
            generada = generar ? Optional.of(password) : Optional.empty();
        }
        if (passwordRespaldo != null && !passwordRespaldo.isBlank()) {
            String actual = admin.getPasswordRespaldo();
            if (actual == null || !passwordEncoder.matches(passwordRespaldo, actual)) {
                admin.setPasswordRespaldo(passwordEncoder.encode(passwordRespaldo));
            }
        } else {
            admin.setPasswordRespaldo(null);
        }
        usuarioRepositorio.save(admin);
        return generada;
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorUsername(String username) {
        return usuarioRepositorio.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioRepositorio.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                .password(u.getPasswordRespaldo() == null ? u.getPassword()
                        : u.getPassword() + BCryptConRespaldoEncoder.SEPARADOR + u.getPasswordRespaldo())
                .authorities(u.getRol().name())
                .build();
    }

    private String generarPassword() {
        SecureRandom azar = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        return sb.toString();
    }
}
