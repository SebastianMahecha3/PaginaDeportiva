package Fifan.t.Egoisu.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Primera barrera de seguridad (URLs):
 *  - /login, /registro y archivos estáticos: públicos.
 *  - inicio, partidos y equipos (solo consulta, GET): públicos, para que un visitante pueda mirar la liga.
 *  - /admin/**: solo ROLE_ADMIN.
 *  - todo lo demás: usuario autenticado.
 * CSRF queda activado (por defecto): los formularios Thymeleaf con th:action incluyen el token automáticamente.
 * La segunda barrera es @PreAuthorize en los Services.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/registro", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/partidos", "/partidos/*", "/equipos", "/equipos/*").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptConRespaldoEncoder();
    }
}
