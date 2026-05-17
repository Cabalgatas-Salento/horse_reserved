package horse_reserved.service.chatbot;

import horse_reserved.dto.request.ChatbotQueryRequest;
import horse_reserved.dto.request.ParticipanteRequest;
import horse_reserved.dto.response.ChatbotAnswerResponse;
import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.model.Rol;
import horse_reserved.model.Ruta;
import horse_reserved.model.Usuario;
import horse_reserved.model.chatbot.ReservationFlowStep;
import horse_reserved.repository.RutaRepository;
import horse_reserved.repository.UsuarioRepository;
import horse_reserved.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatbotReservationFlowServiceTest {

    @Mock private ReservationChatSessionStore sessionStore;
    @Mock private RutaRepository rutaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ChatbotReservationAvailabilityService availabilityService;
    @Mock private ReservaService reservaService;

    @InjectMocks
    private ChatbotReservationFlowService service;

    private Usuario clienteUsuario;

    @BeforeEach
    void setUp() {
        clienteUsuario = new Usuario();
        clienteUsuario.setEmail("cliente@test.com");
        clienteUsuario.setRole(Rol.CLIENTE);
    }

    // ── 1. Sin autenticación ──────────────────────────────────────────────────

    @Test
    @DisplayName("crear_reserva sin auth devuelve acción de navegación a /auth/login")
    void cuandoNoAutenticado_devuelveLoginRequired() {
        // Arrange: poner AnonymousAuthentication en el contexto
        Authentication anon = new AnonymousAuthenticationToken(
                "anon", "anon", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question("quiero reservar")
                .build();

        // Act
        ChatbotAnswerResponse response = service.handle(request, 0.9);

        // Assert
        assertThat(response.getAction()).isNotNull();
        assertThat(response.getAction().getEndpoint()).isEqualTo("/auth/login");
        assertThat(response.isAwaitingUserInput()).isFalse();
        assertThat(response.getIntentId()).isEqualTo("crear_reserva");
    }

    // ── 2. Cliente autenticado inicia flujo y recibe rutas ────────────────────

    @Test
    @DisplayName("Cliente autenticado inicia flujo y recibe lista de rutas disponibles")
    void cuandoClienteAutenticado_inicia_flujo_y_devuelve_rutas() {
        autenticarComo("cliente@test.com", "CLIENTE");
        given(usuarioRepository.findByEmail("cliente@test.com")).willReturn(Optional.of(clienteUsuario));

        Ruta ruta = buildRuta(1L, "Ruta Valle", new BigDecimal("120000"), 90);
        given(rutaRepository.findByActivaTrue()).willReturn(List.of(ruta));

        var sesion = buildSesion("s1", "cliente@test.com", ReservationFlowStep.SELECT_ROUTE);
        given(sessionStore.create("cliente@test.com")).willReturn(sesion);

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question("quiero reservar")
                .build();

        ChatbotAnswerResponse response = service.handle(request, 0.85);

        assertThat(response.getFlow()).isEqualTo("crear_reserva");
        assertThat(response.getStep()).isEqualTo(ReservationFlowStep.SELECT_ROUTE.name());
        assertThat(response.isAwaitingUserInput()).isTrue();
        assertThat(response.getData()).containsKey("routes");
    }

    // ── 3. Selección de ruta avanza a SELECT_DATE ─────────────────────────────

    @Test
    @DisplayName("Enviar routeId en payload avanza la sesión a SELECT_DATE")
    void cuandoEnviaRouteId_avanza_a_SELECT_DATE() {
        autenticarComo("cliente@test.com", "CLIENTE");
        given(usuarioRepository.findByEmail("cliente@test.com")).willReturn(Optional.of(clienteUsuario));

        Ruta ruta = buildRuta(1L, "Ruta Valle", new BigDecimal("120000"), 90);
        given(rutaRepository.findById(1L)).willReturn(Optional.of(ruta));

        var sesion = buildSesion("s1", "cliente@test.com", ReservationFlowStep.SELECT_ROUTE);
        given(sessionStore.find("s1", "cliente@test.com")).willReturn(Optional.of(sesion));

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question("elijo ruta 1")
                .sessionId("s1")
                .payload(Map.of("routeId", 1))
                .build();

        ChatbotAnswerResponse response = service.handle(request, 1.0);

        assertThat(response.getStep()).isEqualTo(ReservationFlowStep.SELECT_DATE.name());
        assertThat(response.isAwaitingUserInput()).isTrue();
    }

    // ── 4. Fecha válida devuelve horarios ─────────────────────────────────────

    @Test
    @DisplayName("Fecha válida futura devuelve horarios disponibles")
    void cuandoFechaValida_devuelve_horarios() {
        autenticarComo("cliente@test.com", "CLIENTE");
        given(usuarioRepository.findByEmail("cliente@test.com")).willReturn(Optional.of(clienteUsuario));

        LocalDate manana = LocalDate.now().plusDays(2);
        var sesion = buildSesion("s1", "cliente@test.com", ReservationFlowStep.SELECT_DATE);
        sesion.setRutaId(1L);
        given(sessionStore.find("s1", "cliente@test.com")).willReturn(Optional.of(sesion));

        var horario = new horse_reserved.dto.response.ChatbotHorarioDisponibleResponse(
                LocalTime.of(8, 30), LocalTime.of(10, 0), 90, 5, new BigDecimal("120000"));
        given(availabilityService.horariosDisponibles(1L, manana)).willReturn(List.of(horario));

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question(manana.toString())
                .sessionId("s1")
                .payload(Map.of("date", manana.toString()))
                .build();

        ChatbotAnswerResponse response = service.handle(request, 1.0);

        assertThat(response.getStep()).isEqualTo(ReservationFlowStep.SELECT_TIME.name());
        assertThat(response.getData()).containsKey("availableTimes");
    }

    // ── 5. Confirmación llama a ReservaService y devuelve action de navegación ─

    @Test
    @DisplayName("Al confirmar, se llama a ReservaService y la respuesta incluye action de navegación")
    void cuandoConfirma_llama_reservaService_y_devuelve_action_navegacion() {

        autenticarComo("cliente@test.com", "CLIENTE");

        given(usuarioRepository.findByEmail("cliente@test.com"))
                .willReturn(Optional.of(clienteUsuario));

        var sesion = buildSesion(
                "s1",
                "cliente@test.com",
                ReservationFlowStep.CONFIRM_RESERVATION
        );

        sesion.setRutaId(1L);
        sesion.setFecha(LocalDate.now().plusDays(3));
        sesion.setHoraInicio(LocalTime.of(8, 30));
        sesion.setCantPersonas(1);
        sesion.getParticipantes().add(buildParticipante());

        given(sessionStore.find("s1", "cliente@test.com"))
                .willReturn(Optional.of(sesion));

        ReservaResponse reservaResponse = new ReservaResponse();
        reservaResponse.setId(42L);
        reservaResponse.setPrecioTotal(new BigDecimal("120000"));
        reservaResponse.setEstado("reservado");

        given(reservaService.crearReserva(any()))
                .willReturn(reservaResponse);

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question("confirmo")
                .sessionId("s1")
                .payload(Map.of("confirm", true))
                .build();

        ChatbotAnswerResponse response = service.handle(request, 1.0);

        assertThat(response.getStep())
                .isEqualTo(ReservationFlowStep.COMPLETED.name());

        assertThat(response.isAwaitingUserInput())
                .isFalse();

        assertThat(response.getAction())
                .isNotNull();

        assertThat(response.getAction().getEndpoint())
                .isEqualTo("/tabs/reservas/42");

        assertThat(response.getData())
                .containsKey("reservation");
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private void autenticarComo(String email, String rol) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(rol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private horse_reserved.model.chatbot.ReservationChatSession buildSesion(
            String id, String email, ReservationFlowStep step) {
        horse_reserved.model.chatbot.ReservationChatSession s =
                horse_reserved.model.chatbot.ReservationChatSession.builder()
                        .id(id)
                        .userEmail(email)
                        .step(step)
                        .updatedAt(java.time.Instant.now())
                        .build();
        return s;
    }

    private Ruta buildRuta(Long id, String nombre, BigDecimal precio, int duracion) {
        Ruta r = new Ruta();
        r.setId(id);
        r.setNombre(nombre);
        r.setPrecio(precio);
        r.setDuracionMinutos(duracion);
        r.setActiva(true);
        r.setDificultad(horse_reserved.model.Dificultad.FACIL);
        return r;
    }

    private ParticipanteRequest buildParticipante() {
        return ParticipanteRequest.builder()
                .primerNombre("Ana")
                .primerApellido("Pérez")
                .tipoDocumento("CEDULA")
                .documento("123456789")
                .edad((short) 30)
                .cmAltura((short) 165)
                .kgPeso(new BigDecimal("62.50"))
                .build();
    }
}