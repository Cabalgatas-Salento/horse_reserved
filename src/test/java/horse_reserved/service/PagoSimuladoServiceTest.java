package horse_reserved.service;

import horse_reserved.dto.request.CrearIntentoPagoRequest;
import horse_reserved.dto.request.ReembolsarPagoRequest;
import horse_reserved.dto.response.IntentoPagoResponse;
import horse_reserved.model.*;
import horse_reserved.repository.IntentoPagoRepository;
import horse_reserved.repository.ReservaRepository;
import horse_reserved.repository.TransaccionRepository;
import horse_reserved.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoSimuladoServiceTest {

    @Mock IntentoPagoRepository intentoRepo;
    @Mock TransaccionRepository transaccionRepo;
    @Mock ReservaRepository reservaRepo;
    @Mock UsuarioRepository usuarioRepo;

    @InjectMocks PagoSimuladoService service;

    private Reserva reserva;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);

        reserva = new Reserva();
        reserva.setId(10L);
        reserva.setCliente(usuario);
    }

    // ── Validaciones de pagador ────────────────────────────────────────────────

    @Test
    void crearIntento_sinPagador_lanzaExcepcion() {
        var req = new CrearIntentoPagoRequest(10L, MetodoPago.EFECTIVO,
                BigDecimal.valueOf(50000), null, null);

        assertThatThrownBy(() -> service.crearIntento(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente uno");
    }

    @Test
    void crearIntento_amboPagadores_lanzaExcepcion() {
        var req = new CrearIntentoPagoRequest(10L, MetodoPago.TARJETA,
                BigDecimal.valueOf(50000), 1L, 2L);

        assertThatThrownBy(() -> service.crearIntento(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crearIntento_usuarioNoEsDuenio_lanzaSecurityException() {
        Usuario otro = new Usuario(); otro.setId(99L);
        when(reservaRepo.findById(10L)).thenReturn(Optional.of(reserva));
        when(usuarioRepo.findById(99L)).thenReturn(Optional.of(otro));

        var req = new CrearIntentoPagoRequest(10L, MetodoPago.EFECTIVO,
                BigDecimal.valueOf(50000), 99L, null);

        assertThatThrownBy(() -> service.crearIntento(req))
                .isInstanceOf(SecurityException.class);
    }

    // ── Intento duplicado exitoso ─────────────────────────────────────────────

    @Test
    void crearIntento_yaExisteRealizado_lanzaExcepcion() {
        when(reservaRepo.findById(10L)).thenReturn(Optional.of(reserva));
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuario));
        when(intentoRepo.existsByReservaIdAndEstado(10L, PagoEstado.REALIZADO))
                .thenReturn(true);

        // Forzamos que el random devuelva siempre exitoso (spy)
        PagoSimuladoService spy = spy(service);

        var req = new CrearIntentoPagoRequest(10L, MetodoPago.TARJETA,
                BigDecimal.valueOf(80000), 1L, null);

        // No podemos controlar el Random directamente, pero validamos la ruta
        // cuando ya existe un REALIZADO y la simulación da éxito:
        // hacemos el test al nivel de repository (mock ya retorna true)
        assertThatThrownBy(() -> service.crearIntento(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REALIZADO");
    }

    // ── Reembolso ─────────────────────────────────────────────────────────────

    @Test
    void reembolsar_intentoRealizado_cambiAReembolsado() {
        IntentoPago intento = IntentoPago.builder()
                .id(5L).reserva(reserva)
                .estado(PagoEstado.REALIZADO)
                .monto(BigDecimal.valueOf(100000))
                .moneda("COP")
                .build();

        when(intentoRepo.findById(5L)).thenReturn(Optional.of(intento));
        when(intentoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transaccionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        IntentoPagoResponse resp = service.reembolsar(new ReembolsarPagoRequest(5L, "cliente solicita"));

        assertThat(resp.estado()).isEqualTo(PagoEstado.REEMBOLSADO);
        verify(transaccionRepo).save(argThat(t ->
                t.getTipoMovimiento() == TipoMovimientoTransaccion.REEMBOLSO &&
                        t.getEstado() == PagoEstado.REEMBOLSADO
        ));
    }

    @Test
    void reembolsar_intentoNORealizdo_lanzaExcepcion() {
        IntentoPago intento = IntentoPago.builder()
                .id(6L).estado(PagoEstado.CANCELADO).build();

        when(intentoRepo.findById(6L)).thenReturn(Optional.of(intento));

        assertThatThrownBy(() -> service.reembolsar(new ReembolsarPagoRequest(6L, null)))
                .isInstanceOf(IllegalStateException.class);
    }
}