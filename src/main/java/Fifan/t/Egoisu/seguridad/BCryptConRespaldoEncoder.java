package Fifan.t.Egoisu.seguridad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt normal, pero acepta un valor almacenado con dos hashes separados por "|" ("hashPrincipal|hashRespaldo").
 * Sirve para que el administrador pueda entrar con su contraseña principal o con la de respaldo.
 * Para los usuarios normales el valor tiene un solo hash y funciona exactamente como BCrypt.
 * (Un hash BCrypt nunca contiene el carácter "|", así que la separación es segura.)
 */
public class BCryptConRespaldoEncoder implements PasswordEncoder {

    public static final String SEPARADOR = "|";

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        for (String hash : encodedPassword.split("\\" + SEPARADOR)) {
            if (bcrypt.matches(rawPassword, hash)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return false;
    }
}
