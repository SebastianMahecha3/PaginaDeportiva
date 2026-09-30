package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import Fifan.t.Egoisu.dto.ResumenEquipoDto;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.repositorios.PartidoRepositorio;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EstadisticaServicioTest {

    private final EstadisticaServicio servicio = new EstadisticaServicio(Mockito.mock(PartidoRepositorio.class));

    @Test
    void resumen_calculaGeneralLocalYVisitante() {
        Equipo a = TestDatos.equipo(1, "A");
        Equipo b = TestDatos.equipo(2, "B");
        // A local gana 2-0, A visitante empata 1-1
        Partido p1 = TestDatos.finalizado(1, a, b, 2, 0, 10, 4, 6, 2);
        Partido p2 = TestDatos.finalizado(2, b, a, 1, 1, 8, 6, 4, 4);

        ResumenEquipoDto r = servicio.construir(a, List.of(p1, p2));

        assertEquals(2, r.getGeneral().getPartidos());
        assertEquals(1, r.getGeneral().getGanados());
        assertEquals(1, r.getGeneral().getEmpatados());
        assertEquals(0, r.getGeneral().getPerdidos());
        assertEquals(0, new BigDecimal("1.50").compareTo(r.getGeneral().getPromGolesFavor()));
        assertEquals(0, new BigDecimal("0.50").compareTo(r.getGeneral().getPromGolesContra()));
        assertEquals(0, new BigDecimal("8.00").compareTo(r.getGeneral().getPromTiros()));
        assertEquals(1, r.getComoLocal().getPartidos());
        assertEquals(0, new BigDecimal("2.00").compareTo(r.getComoLocal().getPromGolesFavor()));
        assertEquals(1, r.getComoVisitante().getPartidos());
        assertEquals(0, new BigDecimal("6.00").compareTo(r.getComoVisitante().getPromTiros()));
    }

    @Test
    void resumen_sinPartidosDevuelveCeros() {
        Equipo a = TestDatos.equipo(1, "A");

        ResumenEquipoDto r = servicio.construir(a, List.of());

        assertEquals(0, r.getGeneral().getPartidos());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.getGeneral().getPromGolesFavor()));
    }
}
