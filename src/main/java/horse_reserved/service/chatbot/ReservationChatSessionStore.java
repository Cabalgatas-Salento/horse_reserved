package horse_reserved.service.chatbot;

import horse_reserved.model.chatbot.ReservationChatSession;
import horse_reserved.model.chatbot.ReservationFlowStep;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén en memoria de sesiones de reserva del chatbot.
 * TTL de 30 minutos; se limpia en cada operación.
 * Para entornos multi-instancia reemplazar por Redis o tabla de BD.
 */
@Component
public class ReservationChatSessionStore {

    private static final Duration TTL = Duration.ofMinutes(30);
    private final ConcurrentHashMap<String, ReservationChatSession> sessions = new ConcurrentHashMap<>();

    /** Crea una sesión nueva para el email dado y la persiste. */
    public ReservationChatSession create(String userEmail) {
        cleanupExpired();
        ReservationChatSession session = ReservationChatSession.builder()
                .id(UUID.randomUUID().toString())
                .userEmail(userEmail)
                .step(ReservationFlowStep.SELECT_ROUTE)
                .updatedAt(Instant.now())
                .build();
        sessions.put(session.getId(), session);
        return session;
    }

    /**
     * Busca una sesión por ID validando que pertenezca al email indicado.
     * Renueva el timestamp si se encuentra.
     */
    public Optional<ReservationChatSession> find(String id, String userEmail) {
        cleanupExpired();
        ReservationChatSession session = sessions.get(id);
        if (session == null || !session.getUserEmail().equals(userEmail)) {
            return Optional.empty();
        }
        session.setUpdatedAt(Instant.now());
        return Optional.of(session);
    }

    /** Persiste los cambios de una sesión existente. */
    public void save(ReservationChatSession session) {
        session.setUpdatedAt(Instant.now());
        sessions.put(session.getId(), session);
    }

    /** Elimina una sesión (flujo completado o cancelado). */
    public void delete(String id) {
        sessions.remove(id);
    }

    private void cleanupExpired() {
        Instant limit = Instant.now().minus(TTL);
        sessions.entrySet().removeIf(e -> e.getValue().getUpdatedAt().isBefore(limit));
    }
}