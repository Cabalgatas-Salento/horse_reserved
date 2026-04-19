package horse_reserved.model.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String type;        // "API_CALL", "NAVIGATION", etc.
    private String endpoint;
    private String method;      // GET, POST
    private boolean authRequired;
}
