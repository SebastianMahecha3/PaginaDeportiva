package Fifan.t.Egoisu.exception;

/** Apuesta imposible o inválida (partido cerrado, monto <= 0, mercado inexistente, parley vacío...). */
public class InvalidBetException extends ReglaNegocioException {
    public InvalidBetException(String mensaje) { super(mensaje); }
}
