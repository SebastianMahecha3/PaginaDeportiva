package Fifan.t.Egoisu.entidades.enums;

/** Mercados disponibles. Para añadir un mercado nuevo: agregar aquí, en CatalogoMercadosServicio y en LiquidacionServicio. */
public enum TipoMercado {
    RESULTADO("Resultado del partido"),
    GOLES("Goles totales"),
    GOLEADOR("Goleador"),
    TIROS("Tiros"),
    CORNERS("Tiros de esquina");

    private final String etiqueta;

    TipoMercado(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
