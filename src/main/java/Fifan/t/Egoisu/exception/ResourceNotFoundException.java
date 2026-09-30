package Fifan.t.Egoisu.exception;

/** Se lanza cuando un recurso (equipo, partido, etc.) no existe. El ManejadorExcepciones lo muestra como 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String mensaje) { super(mensaje); }
}
