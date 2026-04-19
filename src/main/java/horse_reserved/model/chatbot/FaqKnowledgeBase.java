package horse_reserved.model.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Representa la base de conocimiento completa del chatbot.
 * Se carga desde un archivo JSON y contiene todos los intents
 * y la configuración de fallback.
 */
public class FaqKnowledgeBase {
    private String version;
    private String locale;
    private Fallback fallback;
    private List<FaqIntent> intents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    /**
     * Respuesta por defecto cuando no se encuentra un intent válido.
     */
    public static class Fallback {
        private String message;
        private List<String> suggestions;
    }
}