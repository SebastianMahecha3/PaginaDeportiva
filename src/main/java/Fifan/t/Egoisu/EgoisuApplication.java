package Fifan.t.Egoisu;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada. @EnableScheduling activa el cierre automático de partidos que ya comenzaron. */
@SpringBootApplication
@EnableScheduling
public class EgoisuApplication {

    public static void main(String[] args) {
        // Los servidores en la nube corren en UTC. Las horas de los partidos se escriben en hora local,
        // así que se fija la zona horaria de la aplicación (cámbiala con la variable APP_TIMEZONE).
        String zona = System.getenv().getOrDefault("APP_TIMEZONE", "America/Bogota");
        TimeZone.setDefault(TimeZone.getTimeZone(zona));
        SpringApplication.run(EgoisuApplication.class, args);
    }
}
