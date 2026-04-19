package horse_reserved.service;

import horse_reserved.dto.request.CreateReservaRequest;
import horse_reserved.dto.request.ParticipanteRequest;
import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.exception.AccessDeniedBusinessException;
import horse_reserved.exception.BusinessRuleException;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.model.*;
import horse_reserved.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock ReservaRepository reservaRepository;
    @Mock SalidaRepository salidaRepository;
    @Mock RutaRepository rutaRepository;
    @Mock CaballoRepository caballoRepository;
    @Mock GuiaRepository guiaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ReservaMapper reservaMapper;
    @Mock EmailService emailService;
    @Mock AuditLogService auditLogService;
    @Mock EntityManager em;

    @InjectMocks ReservaService reservaService;

    private Usuario cliente;
    private Usuario operador;
    private Ruta ruta;
    private Salida salida;
    private Caballo caballo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservaService, "em", em);

        cliente = Usuario.builder()
                .id(1L).email("cliente@test.com").role(Rol.CLIENTE)
                .isActive(true).passwordChangedAt(Instant.EPOCH).build();

        operador = Usuario.builder()
                .id(2L).email("operador@test.com").role(Rol.OPERADOR)
                .isActive(true).passwordChangedAt(Instant.EPOCH).build();

        ruta = Ruta.builder()
                .id(10L).nombre("Ruta del Bosque").descripcion("Hermosa ruta")
                .precio(BigDecimal.valueOf(50_000)).duracionMinutos(60)
                .dificultad(Dificultad.MEDIA).activa(true).build();

        caballo = Caballo.builder().id(1L).nombre("Tornado").raza("Andaluz").activo(true).build();

        salida = Salida.builder()
                .id(100L).ruta(ruta)
                .fechaProgramada(LocalDate.now().plusDays(2))
                .tiempoInicio(LocalTime.of(8, 0))
                .tiempoFin(LocalTime.of(9, 0))
                .estado("programado")
                .build();
        salida.getCaballos().add(caballo);

        autenticarComo("cliente@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── crearReserva ──────────────────────────────────────────────────────────

    @Test
    void crearReserva_fechaHoy_lanzaBusinessRule() {
        CreateReservaRequest request = requestValido();
        request.setFecha(LocalDate.now());
        autenticarComo("cliente@test.com");

        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("1 día");
    }

    @Test
    void crearReserva_cantPersonasNoCoincideConParticipantes_lanzaBusinessRule() {
        CreateReservaRequest request = requestValido();
        request.setCantPersonas(3);

        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void crearReserva_sinParticipantes_lanzaBusinessRule() {
        CreateReservaRequest request = requestValido();
        request.setParticipantes(new ArrayList<>());

        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void crearReserva_sinCaballosDisponibles_lanzaBusinessRule() {
        CreateReservaRequest request = requestValido();
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(salidaRepository.findProgramadaByRutaAndFechaAndHora(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(rutaRepository.findById(10L)).thenReturn(Optional.of(ruta));
        when(caballoRepository.findDisponibles(any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("caballos");
    }

    @Test
    void crearReserva_cupoInsuficiente_lanzaBusinessRule() {
        CreateReservaRequest request = requestValido();
        // Salida sin caballos (0 cupos)
        Salida salidaSinCupo = Salida.builder()
                .id(101L).ruta(ruta)
                .fechaProgramada(LocalDate.now().plusDays(2))
                .tiempoInicio(LocalTime.of(8, 0))
                .tiempoFin(LocalTime.of(9, 0))
                .estado("programado").build();

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(salidaRepository.findProgramadaByRutaAndFechaAndHora(any(), any(), any()))
                .thenReturn(Optional.of(salidaSinCupo));
        when(reservaRepository.sumPersonasReservadasActivasBySalida(salidaSinCupo.getId()))
                .thenReturn(0L);
        when(caballoRepository.findDisponibles(any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void crearReserva_clienteValido_guardaReserva() {
        CreateReservaRequest request = requestValido();
        Reserva saved = reservaConCliente(client -> client);

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(salidaRepository.findProgramadaByRutaAndFechaAndHora(any(), any(), any()))
                .thenReturn(Optional.of(salida));
        when(reservaRepository.sumPersonasReservadasActivasBySalida(salida.getId())).thenReturn(0L);
        when(guiaRepository.findDisponibles(any(), any(), any())).thenReturn(List.of());
        when(reservaRepository.save(any())).thenReturn(saved);
        when(reservaMapper.toResponse(any())).thenReturn(new ReservaResponse());
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());
        doNothing().when(emailService).sendReservaConfirmacionEmail(any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
        doNothing().when(emailService).sendProgramacionSalidaEmail(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any());

        ReservaResponse resp = reservaService.crearReserva(request);
        assertThat(resp).isNotNull();
        verify(reservaRepository).save(any(Reserva.class));
    }

    // ── cancelarReserva ───────────────────────────────────────────────────────

    @Test
    void cancelar_estadoReservado_cambiasEstadoACancelado() {
        Reserva reserva = reservaConCliente(c -> c);
        reserva.setEstado("reservado");

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(reservaRepository.findDetailedById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenReturn(reserva);
        when(reservaMapper.toResponse(any())).thenReturn(new ReservaResponse());
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());
        doNothing().when(emailService).sendReservaCancelacionEmail(any(), any(), any(), any(), any(), any(), any());

        reservaService.cancelarReserva(1L);
        assertThat(reserva.getEstado()).isEqualTo("cancelado");
    }

    @Test
    void cancelar_yaEstaCandelada_lanzaBusinessRule() {
        Reserva reserva = reservaConCliente(c -> c);
        reserva.setEstado("cancelado");

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(reservaRepository.findDetailedById(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelarReserva(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void cancelar_estaCompletada_lanzaBusinessRule() {
        Reserva reserva = reservaConCliente(c -> c);
        reserva.setEstado("completado");

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(reservaRepository.findDetailedById(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelarReserva(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completada");
    }

    @Test
    void cancelar_otroCliente_lanzaAccessDenied() {
        Usuario otro = Usuario.builder()
                .id(99L).email("otro@test.com").role(Rol.CLIENTE)
                .isActive(true).passwordChangedAt(Instant.EPOCH).build();
        Reserva reserva = reservaConCliente(c -> c);
        reserva.setEstado("reservado");
        reserva.setCliente(otro);

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(reservaRepository.findDetailedById(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelarReserva(1L))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    @Test
    void cancelar_operador_puedeCancelarCualquier() {
        autenticarComo("operador@test.com");
        Reserva reserva = reservaConCliente(c -> c);
        reserva.setEstado("reservado");

        when(usuarioRepository.findByEmail("operador@test.com")).thenReturn(Optional.of(operador));
        when(reservaRepository.findDetailedById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenReturn(reserva);
        when(reservaMapper.toResponse(any())).thenReturn(new ReservaResponse());
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());
        doNothing().when(emailService).sendReservaCancelacionEmail(any(), any(), any(), any(), any(), any(), any());

        assertThatCode(() -> reservaService.cancelarReserva(1L)).doesNotThrowAnyException();
    }

    // ── listarMisReservas ─────────────────────────────────────────────────────

    @Test
    void listarMisReservas_cliente_retornaReservasDeCliente() {
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByClienteIdOrderByIdDesc(1L)).thenReturn(List.of());

        List<ReservaResponse> result = reservaService.listarMisReservas();
        assertThat(result).isEmpty();
        verify(reservaRepository).findByClienteIdOrderByIdDesc(1L);
    }

    @Test
    void listarMisReservas_operador_retornaReservasDeOperador() {
        autenticarComo("operador@test.com");
        when(usuarioRepository.findByEmail("operador@test.com")).thenReturn(Optional.of(operador));
        when(reservaRepository.findByOperadorIdOrderByIdDesc(2L)).thenReturn(List.of());

        List<ReservaResponse> result = reservaService.listarMisReservas();
        assertThat(result).isEmpty();
        verify(reservaRepository).findByOperadorIdOrderByIdDesc(2L);
    }

    // ── obtenerPorId ──────────────────────────────────────────────────────────

    @Test
    void obtener_clienteVeSuPropia_retornaReserva() {
        Reserva reserva = reservaConCliente(c -> c);
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(reservaRepository.findDetailedById(1L)).thenReturn(Optional.of(reserva));
        when(reservaMapper.toResponse(any())).thenReturn(new ReservaResponse());

        assertThatCode(() -> reservaService.obtenerPorId(1L)).doesNotThrowAnyException();
    }

    @Test
    void obtener_reservaNoExiste_lanzaResourceNotFound() {
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(reservaRepository.findDetailedById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.obtenerPorId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtener_clienteNoEsDuenio_lanzaAccessDenied() {
        Usuario otro = Usuario.builder()
                .id(99L).email("otro@test.com").role(Rol.CLIENTE)
                .isActive(true).passwordChangedAt(Instant.EPOCH).build();
        Reserva reserva = reservaConCliente(c -> c);
        reserva.setCliente(otro);

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(reservaRepository.findDetailedById(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.obtenerPorId(1L))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void autenticarComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
    }

    private CreateReservaRequest requestValido() {
        ParticipanteRequest participante = ParticipanteRequest.builder()
                .primerNombre("Ana").primerApellido("García")
                .tipoDocumento("cedula").documento("987654321")
                .edad((short) 25).cmAltura((short) 165).kgPeso(BigDecimal.valueOf(60))
                .build();

        return CreateReservaRequest.builder()
                .rutaId(10L)
                .fecha(LocalDate.now().plusDays(2))
                .horaInicio(LocalTime.of(8, 0))
                .cantPersonas(1)
                .participantes(List.of(participante))
                .build();
    }

    private Reserva reservaConCliente(java.util.function.UnaryOperator<Usuario> clienteOp) {
        return Reserva.builder()
                .id(1L)
                .salida(salida)
                .cliente(clienteOp.apply(cliente))
                .cantPersonas(1)
                .precioUnitario(BigDecimal.valueOf(50_000))
                .precioTotal(BigDecimal.valueOf(50_000))
                .estado("reservado")
                .build();
    }
}
