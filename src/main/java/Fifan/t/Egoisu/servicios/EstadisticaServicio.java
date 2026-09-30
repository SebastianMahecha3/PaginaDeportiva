package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.dto.RendimientoDto;
import Fifan.t.Egoisu.dto.ResumenEquipoDto;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.EstadisticaPartido;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.repositorios.PartidoRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula el historial de un equipo a partir de SUS PARTIDOS FINALIZADOS (nunca de programados ni cancelados).
 * Como se calcula al consultar, el historial siempre está al día después de cada resultado registrado.
 * Lo usan las páginas de equipos y, más adelante, ServicioCuotas.
 */
@Service
@RequiredArgsConstructor
public class EstadisticaServicio {

    private final PartidoRepositorio partidoRepositorio;

    @Transactional(readOnly = true)
    public ResumenEquipoDto resumen(Equipo equipo) {
        List<Partido> partidos = partidoRepositorio.findFinalizadosDeEquipo(equipo.getId(), EstadoPartido.FINALIZADO);
        return construir(equipo, partidos);
    }

    @Transactional(readOnly = true)
    public List<Partido> ultimosPartidos(Equipo equipo, int cantidad) {
        List<Partido> partidos = partidoRepositorio.findFinalizadosDeEquipo(equipo.getId(), EstadoPartido.FINALIZADO);
        return partidos.subList(0, Math.min(cantidad, partidos.size()));
    }

    ResumenEquipoDto construir(Equipo equipo, List<Partido> partidos) {
        List<Partido> comoLocal = partidos.stream().filter(p -> p.getLocal().getId().equals(equipo.getId())).toList();
        List<Partido> comoVisitante = partidos.stream().filter(p -> p.getVisitante().getId().equals(equipo.getId())).toList();
        return new ResumenEquipoDto(equipo, rendimiento(equipo, partidos),
                rendimiento(equipo, comoLocal), rendimiento(equipo, comoVisitante));
    }

    private RendimientoDto rendimiento(Equipo equipo, List<Partido> partidos) {
        int ganados = 0, empatados = 0, perdidos = 0;
        int gf = 0, gc = 0, tf = 0, tc = 0, cf = 0, cc = 0;
        for (Partido p : partidos) {
            boolean esLocal = p.getLocal().getId().equals(equipo.getId());
            int golesFavor = valor(esLocal ? p.getGolesLocal() : p.getGolesVisitante());
            int golesContra = valor(esLocal ? p.getGolesVisitante() : p.getGolesLocal());
            gf += golesFavor;
            gc += golesContra;
            EstadisticaPartido e = p.getEstadistica();
            if (e != null) {
                tf += esLocal ? e.getTirosLocal() : e.getTirosVisitante();
                tc += esLocal ? e.getTirosVisitante() : e.getTirosLocal();
                cf += esLocal ? e.getCornersLocal() : e.getCornersVisitante();
                cc += esLocal ? e.getCornersVisitante() : e.getCornersLocal();
            }
            if (golesFavor > golesContra) ganados++;
            else if (golesFavor == golesContra) empatados++;
            else perdidos++;
        }
        int n = partidos.size();
        return new RendimientoDto(n, ganados, empatados, perdidos,
                promedio(gf, n), promedio(gc, n), promedio(tf, n), promedio(tc, n), promedio(cf, n), promedio(cc, n));
    }

    private int valor(Integer numero) { return numero == null ? 0 : numero; }

    private BigDecimal promedio(int total, int partidos) {
        if (partidos == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(partidos), 2, RoundingMode.HALF_UP);
    }
}
