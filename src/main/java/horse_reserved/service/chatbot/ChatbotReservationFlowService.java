package horse_reserved.service.chatbot;

import horse_reserved.dto.request.ChatbotQueryRequest;
import horse_reserved.dto.request.CreateReservaRequest;
import horse_reserved.dto.request.ParticipanteRequest;
import horse_reserved.dto.response.ChatbotAnswerResponse;
import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.model.Rol;
import horse_reserved.model.Ruta;
import horse_reserved.model.Usuario;
import horse_reserved.model.chatbot.Action;
import horse_reserved.model.chatbot.ReservationChatSession;
import horse_reserved.model.chatbot.ReservationFlowStep;
import horse_reserved.repository.RutaRepository;
import horse_reserved.repository.UsuarioRepository;
import horse_reserved.service.ReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Maneja el flujo conversacional completo para crear una reserva desde el chatbot.
 *
 * Pasos del flujo:
 *  1. SELECT_ROUTE        – muestra rutas activas; el usuario elige una.
 *  2. SELECT_DATE         – solicita fecha (posterior a hoy).
 *  3. SELECT_TIME         – muestra horarios disponibles; el usuario elige hora.
 *  4. SELECT_PEOPLE_COUNT – solicita cantidad de personas.
 *  5. COLLECT_PARTICIPANT – recopila datos de cada participante (N iteraciones).
 *  6. CONFIRM_RESERVATION – muestra resumen; espera confirmación explícita.
 *  7. COMPLETED           – llama a ReservaService.crearReserva y devuelve la respuesta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotReservationFlowService {

    private final ReservationChatSessionStore sessionStore;
    private final RutaRepository rutaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChatbotReservationAvailabilityService availabilityService;
    private final ReservaService reservaService;

    // ── Entrada principal ─────────────────────────────────────────────────────

    @Transactional
    public ChatbotAnswerResponse handle(ChatbotQueryRequest request, double confidence) {

        // 1. Validar autenticación
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return loginRequired(confidence);
        }

        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        if (usuario == null) return loginRequired(confidence);

        // 2. Validar rol: solo CLIENTE puede usar este flujo conversacional
        if (usuario.getRole() != Rol.CLIENTE) {
            return ChatbotAnswerResponse.builder()
                    .intentId("crear_reserva")
                    .confidence(confidence)
                    .answer("Por ahora las reservas desde el chat están disponibles únicamente para usuarios con rol CLIENTE. "
                            + "Si eres operador, usa el flujo administrativo correspondiente.")
                    .awaitingUserInput(false)
                    .build();
        }

        // 3. Resolver o crear sesión
        ReservationChatSession session = resolveSession(request, usuario.getEmail());
        Map<String, Object> payload = request.getPayload() != null ? request.getPayload() : Map.of();

        // 4. Cancelación global
        if (Boolean.TRUE.equals(payload.get("cancel"))) {
            sessionStore.delete(session.getId());
            return ChatbotAnswerResponse.builder()
                    .intentId("crear_reserva")
                    .flow("crear_reserva")
                    .step(ReservationFlowStep.CANCELLED.name())
                    .sessionId(session.getId())
                    .awaitingUserInput(false)
                    .answer("He cancelado la creación de la reserva. Escribe 'quiero reservar' cuando quieras intentarlo de nuevo.")
                    .build();
        }

        // 5. Despachar al paso actual
        return switch (session.getStep()) {
            case SELECT_ROUTE        -> selectRoute(session, payload);
            case SELECT_DATE         -> selectDate(session, payload);
            case SELECT_TIME         -> selectTime(session, payload);
            case SELECT_PEOPLE_COUNT -> selectPeopleCount(session, payload);
            case COLLECT_PARTICIPANT -> collectParticipant(session, payload);
            case CONFIRM_RESERVATION -> confirmReservation(session, payload);
            default -> response(session,
                    "Este flujo ya finalizó. Escribe 'quiero reservar' para iniciar una nueva reserva.",
                    false, Map.of());
        };
    }

    // ── Pasos del flujo ───────────────────────────────────────────────────────

    private ChatbotAnswerResponse selectRoute(ReservationChatSession session, Map<String, Object> payload) {
        Object routeIdValue = payload.get("routeId");
        if (routeIdValue != null) {
            Long routeId = Long.valueOf(routeIdValue.toString());
            rutaRepository.findById(routeId)
                    .filter(Ruta::isActiva)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La ruta seleccionada no existe o no está activa. Elige una de las rutas disponibles."));
            session.setRutaId(routeId);
            session.setStep(ReservationFlowStep.SELECT_DATE);
            sessionStore.save(session);
            return response(session,
                    "¡Perfecto! Ahora indícame la fecha de la reserva en formato YYYY-MM-DD. Debe ser posterior a hoy.",
                    true, Map.of("selectedRouteId", routeId));
        }

        // Mostrar rutas activas disponibles
        List<Map<String, Object>> rutas = rutaRepository.findByActivaTrue().stream()
                .map(r -> Map.<String, Object>of(
                        "id",               r.getId(),
                        "nombre",           r.getNombre(),
                        "precio",           r.getPrecio(),
                        "duracionMinutos",  r.getDuracionMinutos(),
                        "dificultad",       r.getDificultad().name()))
                .toList();

        return response(session,
                "¡Hola! Vamos a crear tu reserva. ¿En cuál ruta deseas reservar? Aquí están las rutas activas disponibles.",
                true, Map.of("routes", rutas));
    }

    private ChatbotAnswerResponse selectDate(ReservationChatSession session, Map<String, Object> payload) {
        Object dateValue = payload.get("date");
        if (dateValue == null) {
            return response(session,
                    "Indícame la fecha de la reserva en formato YYYY-MM-DD. Debe ser posterior a hoy.",
                    true, Map.of());
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(dateValue.toString());
        } catch (Exception e) {
            return response(session,
                    "Fecha inválida. Por favor usa el formato YYYY-MM-DD (ejemplo: 2026-06-15).",
                    true, Map.of());
        }

        if (!fecha.isAfter(LocalDate.now())) {
            return response(session,
                    "La fecha debe ser al menos 1 día después de hoy. Intenta con otra fecha en formato YYYY-MM-DD.",
                    true, Map.of());
        }

        session.setFecha(fecha);
        session.setStep(ReservationFlowStep.SELECT_TIME);
        sessionStore.save(session);
        return askTime(session);
    }

    private ChatbotAnswerResponse askTime(ReservationChatSession session) {
        var horarios = availabilityService.horariosDisponibles(session.getRutaId(), session.getFecha());
        if (horarios.isEmpty()) {
            // Retroceder al paso de fecha para que el usuario elija otra
            session.setStep(ReservationFlowStep.SELECT_DATE);
            sessionStore.save(session);
            return response(session,
                    "No encontré horarios disponibles para esa ruta y fecha. "
                            + "Por favor indícame otra fecha en formato YYYY-MM-DD.",
                    true, Map.of());
        }
        return response(session,
                "Estos son los horarios disponibles para la ruta seleccionada. Elige la hora de inicio que prefieras.",
                true, Map.of("availableTimes", horarios));
    }

    private ChatbotAnswerResponse selectTime(ReservationChatSession session, Map<String, Object> payload) {
        Object timeValue = payload.get("time");
        if (timeValue == null) return askTime(session);

        LocalTime hora;
        try {
            hora = LocalTime.parse(timeValue.toString());
        } catch (Exception e) {
            return response(session,
                    "Hora inválida. Usa el formato HH:mm (ejemplo: 08:30).",
                    true, Map.of());
        }

        session.setHoraInicio(hora);
        session.setStep(ReservationFlowStep.SELECT_PEOPLE_COUNT);
        sessionStore.save(session);
        return response(session,
                "¿Cuántos turistas/personas deseas incluir en la reserva?",
                true, Map.of());
    }

    private ChatbotAnswerResponse selectPeopleCount(ReservationChatSession session, Map<String, Object> payload) {
        Object countValue = payload.get("peopleCount");
        if (countValue == null) {
            return response(session,
                    "Indícame la cantidad de turistas/personas para la reserva (número entero mayor a 0).",
                    true, Map.of());
        }

        int count;
        try {
            count = Integer.parseInt(countValue.toString());
        } catch (NumberFormatException e) {
            return response(session, "Por favor ingresa un número válido de personas.", true, Map.of());
        }

        if (count < 1) {
            return response(session, "La cantidad debe ser al menos 1 persona.", true, Map.of());
        }

        session.setCantPersonas(count);
        session.getParticipantes().clear();
        session.setStep(ReservationFlowStep.COLLECT_PARTICIPANT);
        sessionStore.save(session);

        return response(session,
                "Perfecto, " + count + " persona(s). Ahora necesito los datos del participante 1 de " + count + ".",
                true, participantSchema(session));
    }

    private ChatbotAnswerResponse collectParticipant(ReservationChatSession session, Map<String, Object> payload) {
        Object participantValue = payload.get("participant");
        if (!(participantValue instanceof Map<?, ?> participantMap)) {
            return response(session,
                    "Envía los datos del participante " + (session.getParticipantes().size() + 1)
                            + " usando el campo 'participant' en el payload.",
                    true, participantSchema(session));
        }

        ParticipanteRequest participante;
        try {
            participante = mapParticipant(participantMap);
        } catch (IllegalArgumentException e) {
            return response(session,
                    "Faltan datos del participante: " + e.getMessage()
                            + ". Completa todos los campos requeridos e intenta de nuevo.",
                    true, participantSchema(session));
        }

        session.getParticipantes().add(participante);
        sessionStore.save(session);

        int recibidos = session.getParticipantes().size();
        int total = session.getCantPersonas();

        if (recibidos < total) {
            int siguiente = recibidos + 1;
            return response(session,
                    "Datos recibidos ✓. Ahora envía los datos del participante " + siguiente + " de " + total + ".",
                    true, participantSchema(session));
        }

        // Todos los participantes recopilados → pasar a confirmación
        session.setStep(ReservationFlowStep.CONFIRM_RESERVATION);
        sessionStore.save(session);
        return confirmationSummary(session);
    }

    private ChatbotAnswerResponse confirmReservation(ReservationChatSession session, Map<String, Object> payload) {
        if (!Boolean.TRUE.equals(payload.get("confirm"))) {
            // Mostrar resumen nuevamente si no se confirmó explícitamente
            return confirmationSummary(session);
        }

        CreateReservaRequest createRequest = CreateReservaRequest.builder()
                .rutaId(session.getRutaId())
                .fecha(session.getFecha())
                .horaInicio(session.getHoraInicio())
                .cantPersonas(session.getCantPersonas())
                .participantes(session.getParticipantes())
                .build();

        ReservaResponse reserva;
        try {
            reserva = reservaService.crearReserva(createRequest);
        } catch (Exception e) {
            // Devolver mensaje claro sin borrar la sesión para que el usuario pueda ajustar
            log.warn("Error al crear reserva en el chatbot para sesión {}: {}", session.getId(), e.getMessage());
            return response(session,
                    "No pude confirmar la reserva: " + e.getMessage()
                            + ". Revisa la información e intenta confirmar de nuevo, o escribe 'cancelar' para salir.",
                    true, Map.of("error", e.getMessage()));
        }

        sessionStore.delete(session.getId());

        return ChatbotAnswerResponse.builder()
                .intentId("crear_reserva")
                .flow("crear_reserva")
                .step(ReservationFlowStep.COMPLETED.name())
                .sessionId(session.getId())
                .awaitingUserInput(false)
                .confidence(1.0)
                .answer("¡Reserva creada con éxito! Tu número de reserva es #" + reserva.getId()
                        + ". Precio total: $" + reserva.getPrecioTotal() + ". Estado: " + reserva.getEstado() + ".")
                .action(Action.builder()
                        .type("NAVIGATION")
                        .endpoint("/tabs/reservas/" + reserva.getId())
                        .method("GET")
                        .authRequired(true)
                        .payload(Map.of("reservaId", reserva.getId()))
                        .build())
                .data(Map.of("reservation", reserva))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ChatbotAnswerResponse confirmationSummary(ReservationChatSession session) {
        Ruta ruta = rutaRepository.findById(session.getRutaId()).orElseThrow();
        BigDecimal total = ruta.getPrecio().multiply(BigDecimal.valueOf(session.getCantPersonas()));
        return response(session,
                "Aquí está el resumen de tu reserva. ¿Confirmas? Responde con {\"confirm\": true} para aceptar o {\"cancel\": true} para cancelar.",
                true,
                Map.of(
                        "summary", Map.of(
                                "rutaId",        ruta.getId(),
                                "rutaNombre",    ruta.getNombre(),
                                "fecha",         session.getFecha().toString(),
                                "horaInicio",    session.getHoraInicio().toString(),
                                "horaFin",       session.getHoraInicio().plusMinutes(ruta.getDuracionMinutos()).toString(),
                                "cantPersonas",  session.getCantPersonas(),
                                "precioUnitario", ruta.getPrecio(),
                                "precioTotal",   total,
                                "participantes", session.getParticipantes()
                        ),
                        "actions", List.of("confirm", "cancel")
                ));
    }

    private ChatbotAnswerResponse loginRequired(double confidence) {
        return ChatbotAnswerResponse.builder()
                .intentId("crear_reserva")
                .confidence(confidence)
                .answer("Para crear una reserva desde el chat necesitas iniciar sesión o registrarte primero.")
                .action(Action.builder()
                        .type("NAVIGATION")
                        .endpoint("/auth/login")
                        .method("GET")
                        .authRequired(false)
                        .build())
                .notes(List.of("Cuando inicies sesión, vuelve al chat y escribe: 'quiero reservar'."))
                .awaitingUserInput(false)
                .build();
    }

    private ReservationChatSession resolveSession(ChatbotQueryRequest request, String email) {
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return sessionStore.find(request.getSessionId(), email)
                    .orElseGet(() -> sessionStore.create(email));
        }
        return sessionStore.create(email);
    }

    private ParticipanteRequest mapParticipant(Map<?, ?> source) {
        return ParticipanteRequest.builder()
                .primerNombre(requiredString(source, "primerNombre"))
                .primerApellido(requiredString(source, "primerApellido"))
                .tipoDocumento(requiredString(source, "tipoDocumento"))
                .documento(requiredString(source, "documento"))
                .edad(Short.parseShort(requiredString(source, "edad")))
                .cmAltura(Short.parseShort(requiredString(source, "cmAltura")))
                .kgPeso(new BigDecimal(requiredString(source, "kgPeso")))
                .build();
    }

    private String requiredString(Map<?, ?> source, String key) {
        Object value = source.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("campo requerido faltante: " + key);
        }
        return value.toString().trim();
    }

    private Map<String, Object> participantSchema(ReservationChatSession session) {
        return Map.of(
                "participantIndex",    session.getParticipantes().size() + 1,
                "totalParticipants",   session.getCantPersonas(),
                "requiredFields",      List.of("primerNombre", "primerApellido", "tipoDocumento",
                        "documento", "edad", "cmAltura", "kgPeso"),
                "tipoDocumentoOptions", List.of("CEDULA", "PASAPORTE", "TARJETA_IDENTIDAD")
        );
    }

    private ChatbotAnswerResponse response(ReservationChatSession session,
                                           String answer,
                                           boolean awaiting,
                                           Map<String, Object> data) {
        return ChatbotAnswerResponse.builder()
                .intentId("crear_reserva")
                .flow("crear_reserva")
                .step(session.getStep().name())
                .sessionId(session.getId())
                .awaitingUserInput(awaiting)
                .answer(answer)
                .data(data)
                .build();
    }
}