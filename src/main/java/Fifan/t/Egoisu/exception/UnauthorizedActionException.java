package Fifan.t.Egoisu.exception;

/** El usuario intentó una acción sobre algo que no le pertenece (ej. ver la apuesta de otro usuario). */
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String mensaje) { super(mensaje); }
}
