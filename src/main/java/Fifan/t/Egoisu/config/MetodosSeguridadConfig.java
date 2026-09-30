package Fifan.t.Egoisu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Activa @PreAuthorize en los Services (segunda barrera de autorización, además de las URLs). */
@Configuration
@EnableMethodSecurity
public class MetodosSeguridadConfig {
}
