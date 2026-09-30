package Fifan.t.Egoisu.exception;

/** Partido o resultado incoherente (mismo equipo local y visitante, goles que no cuadran, partido ya finalizado...). */
public class InvalidMatchException extends ReglaNegocioException {
    public InvalidMatchException(String mensaje) { super(mensaje); }
}
