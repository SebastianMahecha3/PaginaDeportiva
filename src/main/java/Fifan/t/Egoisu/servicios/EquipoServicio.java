package Fifan.t.Egoisu.servicios;

import Fifan.t.Egoisu.dto.EquipoDto;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.enums.EstadoEquipo;
import Fifan.t.Egoisu.exception.ReglaNegocioException;
import Fifan.t.Egoisu.exception.ResourceNotFoundException;
import Fifan.t.Egoisu.repositorios.EquipoRepositorio;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lógica de equipos. Las operaciones que modifican datos exigen ROLE_ADMIN (segunda barrera tras Spring Security). */
@Service
@RequiredArgsConstructor
public class EquipoServicio {

    private final EquipoRepositorio equipoRepositorio;

    @Transactional(readOnly = true)
    public List<Equipo> listar() { return equipoRepositorio.findAllByOrderByNombreAsc(); }

    @Transactional(readOnly = true)
    public List<Equipo> listarActivos() { return equipoRepositorio.findByEstadoOrderByNombreAsc(EstadoEquipo.ACTIVO); }

    @Transactional(readOnly = true)
    public Equipo obtener(Long id) {
        return equipoRepositorio.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Equipo crear(EquipoDto dto) {
        validar(dto, null);
        Equipo equipo = new Equipo();
        equipo.setNombre(dto.getNombre().trim());
        equipo.setDescripcion(limpiar(dto.getDescripcion()));
        equipo.setValoracionInicial(dto.getValoracionInicial());
        return equipoRepositorio.save(equipo);
    }

    /** Si el equipo ya tiene experiencia (jugó un partido) la valoración inicial se conserva como dato histórico. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Equipo actualizar(Long id, EquipoDto dto) {
        Equipo equipo = obtener(id);
        validar(dto, id);
        equipo.setNombre(dto.getNombre().trim());
        equipo.setDescripcion(limpiar(dto.getDescripcion()));
        if (!equipo.isTieneExperiencia()) {
            equipo.setValoracionInicial(dto.getValoracionInicial());
        }
        return equipoRepositorio.save(equipo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Equipo cambiarEstado(Long id, EstadoEquipo estado) {
        Equipo equipo = obtener(id);
        equipo.setEstado(estado);
        return equipoRepositorio.save(equipo);
    }

    private void validar(EquipoDto dto, Long idActual) {
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new ReglaNegocioException("El nombre del equipo es obligatorio.");
        }
        Integer v = dto.getValoracionInicial();
        if (v == null || v < 1 || v > 10) {
            throw new ReglaNegocioException("La valoración inicial debe estar entre 1 y 10.");
        }
        String nombre = dto.getNombre().trim();
        boolean duplicado = idActual == null
                ? equipoRepositorio.existsByNombreIgnoreCase(nombre)
                : equipoRepositorio.existsByNombreIgnoreCaseAndIdNot(nombre, idActual);
        if (duplicado) {
            throw new ReglaNegocioException("Ya existe un equipo con ese nombre.");
        }
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
