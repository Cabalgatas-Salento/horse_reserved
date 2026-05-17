package horse_reserved.model.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Representa una acción ejecutable asociada a la respuesta del chatbot.
 * Permite que el frontend dispare comportamientos adicionales
 * como navegación o llamadas a API.
 */
public class Action {
    private String type;        // NAVIGATION, API_CALL, CHAT_FLOW, BUTTONS
    private String endpoint;
    private String method;
    private boolean authRequired;
    private Map<String, Object> payload;
}
