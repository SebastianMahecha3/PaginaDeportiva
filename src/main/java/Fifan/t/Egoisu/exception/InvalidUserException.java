package Fifan.t.Egoisu.exception;

/** Error al registrar un usuario. Indica el campo del formulario al que pertenece el error. */
public class InvalidUserException extends ReglaNegocioException {
    private final String campo;

    public InvalidUserException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() { return campo; }
}
