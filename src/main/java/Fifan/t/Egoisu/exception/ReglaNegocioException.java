package Fifan.t.Egoisu.exception;

/**
 * Violación de una regla de negocio (dato válido por formato, pero no permitido por las reglas).
 * Los Services la lanzan y los Controllers muestran el mensaje al usuario.
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) { super(mensaje); }
}
