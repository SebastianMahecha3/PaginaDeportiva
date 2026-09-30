package Fifan.t.Egoisu.entidades.enums;

import java.math.BigDecimal;

/**
 * Posición del jugador en la cancha. El "peso de gol" indica qué tan probable es, en general,
 * que un jugador de esa posición marque (se usa en la cuota del mercado Goleador).
 */
public enum PosicionJugador {
    PORTERO("Portero", "0.02"),
    DEFENSA("Defensa", "0.25"),
    MEDIOCAMPISTA("Mediocampista", "0.60"),
    DELANTERO("Delantero", "1.00");

    private final String etiqueta;
    private final BigDecimal pesoGol;

    PosicionJugador(String etiqueta, String pesoGol) {
        this.etiqueta = etiqueta;
        this.pesoGol = new BigDecimal(pesoGol);
    }

    public String getEtiqueta() { return etiqueta; }

    public BigDecimal getPesoGol() { return pesoGol; }
}
