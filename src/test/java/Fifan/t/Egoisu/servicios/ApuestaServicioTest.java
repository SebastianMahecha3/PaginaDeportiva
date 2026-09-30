package Fifan.t.Egoisu.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Fifan.t.Egoisu.config.ConfiguracionApuestas;
import Fifan.t.Egoisu.dto.SeleccionDto;
import Fifan.t.Egoisu.entidades.Apuesta;
import Fifan.t.Egoisu.entidades.Equipo;
import Fifan.t.Egoisu.entidades.Partido;
import Fifan.t.Egoisu.entidades.Usuario;
import Fifan.t.Egoisu.entidades.enums.EstadoApuesta;
import Fifan.t.Egoisu.entidades.enums.EstadoPartido;
import Fifan.t.Egoisu.entidades.enums.EstadoSeleccion;
import Fifan.t.Egoisu.entidades.enums.OpcionSeleccion;
import Fifan.t.Egoisu.entidades.enums.Rol;
import Fifan.t.Egoisu.entidades.enums.TipoApuesta;
import Fifan.t.Egoisu.entidades.enums.TipoMercado;
import Fifan.t.Egoisu.exception.InvalidBetException;
import Fifan.t.Egoisu.exception.UnauthorizedActionException;
import Fifan.t.Egoisu.repositorios.ApuestaRepositorio;
import Fifan.t.Egoisu.repositorios.JugadorRepositorio;
import Fifan.t.Egoisu.repositorios.PartidoRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El servicio de cuotas se simula (mock): la fórmula real de cuotas aún no está aprobada. */
class ApuestaServicioTest {

    private ApuestaRepositorio apuestaRepo;
    private PartidoRepositorio partidoRepo;
    private UsuarioServicio usuarioServicio;
    private ServicioCuotas servicioCuotas;
    private CatalogoMercadosServicio catalogo;
    private ApuestaServicio servicio;

    private Partido partido1;
    private Partido partido2;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        apuestaRepo = mock(ApuestaRepositorio.class);
        partidoRepo = mock(PartidoRepositorio.class);
        JugadorRepositorio jugadorRepo = mock(JugadorRepositorio.class);
        usuarioServicio = mock(UsuarioServicio.class);
        servicioCuotas = mock(ServicioCuotas.class);
        catalogo = mock(CatalogoMercadosServicio.class);
        ConfiguracionApuestas config = new ConfiguracionApuestas();
        servicio = new ApuestaServicio(apuestaRepo, partidoRepo, jugadorRepo, usuarioServicio, servicioCuotas,
                catalogo, new CalculadoraApuesta(config), config);

        Equipo a = TestDatos.equipo(1, "A");
        Equipo b = TestDatos.equipo(2, "B");
        Equipo c = TestDatos.equipo(3, "C");
        partido1 = TestDatos.partido(1, a, b, EstadoPartido.PROGRAMADO, LocalDateTime.now().plusDays(1));
        partido2 = TestDatos.partido(2, b, c, EstadoPartido.PROGRAMADO, LocalDateTime.now().plusDays(2));
        when(partidoRepo.findById(1L)).thenReturn(Optional.of(partido1));
        when(partidoRepo.findById(2L)).thenReturn(Optional.of(partido2));

        ana = new Usuario();
        ana.setId(7L);
        ana.setUsername("ana");
        ana.setRol(Rol.ROLE_USER);
        when(usuarioServicio.obtenerPorUsername("ana")).thenReturn(ana);

        when(catalogo.describir(any(Partido.class), any(SeleccionDto.class), any())).thenReturn("Gana el local");
        when(apuestaRepo.save(any(Apuesta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SeleccionDto resultadoLocal(long partidoId) {
        return new SeleccionDto(partidoId, TipoMercado.RESULTADO, OpcionSeleccion.LOCAL, null, null, null);
    }

    private void cuota(Partido p, String valor) {
        when(servicioCuotas.calcularCuota(eq(p), any(SeleccionDto.class))).thenReturn(Optional.of(new BigDecimal(valor)));
    }

    @Test
    void confirmarSimple_congelaCuotaYCalculaGanancia() {
        cuota(partido1, "2.00");

        Apuesta apuesta = servicio.confirmar("ana", List.of(resultadoLocal(1)), new BigDecimal("100"), null);

        assertEquals(TipoApuesta.SIMPLE, apuesta.getTipo());
        assertEquals(EstadoApuesta.PENDIENTE, apuesta.getEstado());
        assertEquals(0, new BigDecimal("2.00").compareTo(apuesta.getCuotaTotal()));
        assertEquals(0, new BigDecimal("190.00").compareTo(apuesta.getGananciaPotencial()));
        assertEquals(1, apuesta.getSelecciones().size());
        assertEquals(0, new BigDecimal("2.00").compareTo(apuesta.getSelecciones().get(0).getCuota()));
        assertEquals(EstadoSeleccion.PENDIENTE, apuesta.getSelecciones().get(0).getEstado());
        assertEquals("A vs B", apuesta.getSelecciones().get(0).getPartidoDescripcion());
    }

    @Test
    void confirmarParley_multiplicaLasCuotas() {
        cuota(partido1, "1.50");
        cuota(partido2, "1.80");

        Apuesta apuesta = servicio.confirmar("ana", List.of(resultadoLocal(1), resultadoLocal(2)), new BigDecimal("100"), null);

        assertEquals(TipoApuesta.PARLEY, apuesta.getTipo());
        assertEquals(0, new BigDecimal("2.70").compareTo(apuesta.getCuotaTotal()));
        assertEquals(0, new BigDecimal("256.50").compareTo(apuesta.getGananciaPotencial()));
    }

    @Test
    void confirmar_sinCuotaDisponibleSeRechaza() {
        when(servicioCuotas.calcularCuota(any(Partido.class), any(SeleccionDto.class))).thenReturn(Optional.empty());

        assertThrows(InvalidBetException.class,
                () -> servicio.confirmar("ana", List.of(resultadoLocal(1)), new BigDecimal("100"), null));
        verify(apuestaRepo, never()).save(any(Apuesta.class));
    }

    @Test
    void confirmar_elAdministradorNoPuedeApostar() {
        Usuario admin = new Usuario();
        admin.setUsername("Egoisu");
        admin.setRol(Rol.ROLE_ADMIN);
        when(usuarioServicio.obtenerPorUsername("Egoisu")).thenReturn(admin);
        cuota(partido1, "2.00");

        assertThrows(InvalidBetException.class,
                () -> servicio.confirmar("Egoisu", List.of(resultadoLocal(1)), new BigDecimal("100"), null));
    }

    @Test
    void confirmar_rechazaMontosInvalidos() {
        cuota(partido1, "2.00");

        assertThrows(InvalidBetException.class, () -> servicio.confirmar("ana", List.of(resultadoLocal(1)), BigDecimal.ZERO, null));
        assertThrows(InvalidBetException.class, () -> servicio.confirmar("ana", List.of(resultadoLocal(1)), new BigDecimal("-5"), null));
        assertThrows(InvalidBetException.class, () -> servicio.confirmar("ana", List.of(resultadoLocal(1)), new BigDecimal("10.123"), null));
        assertThrows(InvalidBetException.class, () -> servicio.confirmar("ana", List.of(resultadoLocal(1)), null, null));
    }

    @Test
    void confirmar_rechazaBoletoVacio() {
        assertThrows(InvalidBetException.class, () -> servicio.confirmar("ana", List.of(), new BigDecimal("10"), null));
    }

    @Test
    void confirmar_rechazaPartidosCerradosOCancelados() {
        cuota(partido1, "2.00");
        partido1.setEstado(EstadoPartido.EN_JUEGO);
        assertThrows(InvalidBetException.class, () -> servicio.confirmar("ana", List.of(resultadoLocal(1)), new BigDecimal("10"), null));

        partido1.setEstado(EstadoPartido.CANCELADO);
        assertThrows(InvalidBetException.class, () -> servicio.confirmar("ana", List.of(resultadoLocal(1)), new BigDecimal("10"), null));
    }

    @Test
    void confirmar_avisaSiLaCuotaCambioDesdeLaVistaPrevia() {
        cuota(partido1, "2.00");

        assertThrows(InvalidBetException.class,
                () -> servicio.confirmar("ana", List.of(resultadoLocal(1)), new BigDecimal("10"), new BigDecimal("1.90")));
    }

    @Test
    void confirmar_unParleyNoAdmiteDosSeleccionesDelMismoPartido() {
        cuota(partido1, "2.00");

        assertThrows(InvalidBetException.class,
                () -> servicio.confirmar("ana", List.of(resultadoLocal(1), resultadoLocal(1)), new BigDecimal("10"), null));
    }

    @Test
    void validarNuevaSeleccion_noPermiteRepetirPartidoEnElBoleto() {
        cuota(partido1, "2.00");

        assertThrows(InvalidBetException.class,
                () -> servicio.validarNuevaSeleccion(List.of(resultadoLocal(1)), resultadoLocal(1)));
    }

    @Test
    void resumir_calculaGananciaPotencialParaElBoleto() {
        cuota(partido1, "2.00");

        var resumen = servicio.resumir(List.of(resultadoLocal(1)), new BigDecimal("100"));

        assertEquals(true, resumen.isValido());
        assertEquals(0, new BigDecimal("190.00").compareTo(resumen.getGananciaPotencial()));
        assertEquals(0, new BigDecimal("5.00").compareTo(resumen.getComision()));
    }

    @Test
    void obtenerDeUsuario_noDejaVerApuestasAjenas() {
        Usuario otro = new Usuario();
        otro.setUsername("beto");
        Apuesta ajena = new Apuesta();
        ajena.setUsuario(otro);
        when(apuestaRepo.findById(99L)).thenReturn(Optional.of(ajena));

        assertThrows(UnauthorizedActionException.class, () -> servicio.obtenerDeUsuario(99L, "ana"));
    }
}
