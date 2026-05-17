package horse_reserved.dto.response;

import horse_reserved.model.chatbot.Action;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotAnswerResponse {

    private String intentId;
    private double confidence;
    private String answer;
    private Action action;

    @Builder.Default
    private List<String> notes = List.of();

    @Builder.Default
    private List<String> suggestions = List.of();

    // ── Campos de flujo conversacional
    /** ID de sesión del flujo de reserva en curso. Null para respuestas FAQ normales. */
    private String sessionId;

    /** Nombre del flujo activo, p.ej. "crear_reserva". Null para FAQ normales. */
    private String flow;

    /** Paso actual del flujo, p.ej. "SELECT_ROUTE". Null para FAQ normales. */
    private String step;

    /** true si el chatbot está esperando una respuesta del usuario. */
    private boolean awaitingUserInput;

    /** Datos estructurados para que el frontend renderice listas, resúmenes, etc. */
    private Map<String, Object> data;
}