package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Fifan.t.Egoisu.seguridad.BCryptConRespaldoEncoder;
import org.junit.jupiter.api.Test;

class BCryptConRespaldoEncoderTest {

    private final BCryptConRespaldoEncoder encoder = new BCryptConRespaldoEncoder();

    @Test
    void funcionaComoBCryptNormalConUnSoloHash() {
        String hash = encoder.encode("clave-uno");

        assertNotEquals("clave-uno", hash);
        assertTrue(encoder.matches("clave-uno", hash));
        assertFalse(encoder.matches("otra", hash));
    }

    @Test
    void conDosHashesAceptaCualquieraDeLasDosContrasenas() {
        String doble = encoder.encode("principal") + BCryptConRespaldoEncoder.SEPARADOR + encoder.encode("respaldo");

        assertTrue(encoder.matches("principal", doble));
        assertTrue(encoder.matches("respaldo", doble));
        assertFalse(encoder.matches("intruso", doble));
    }

    @Test
    void valoresNulosOVaciosNuncaCoinciden() {
        assertFalse(encoder.matches("x", null));
        assertFalse(encoder.matches("x", ""));
        assertFalse(encoder.matches(null, encoder.encode("x")));
    }
}
