package horse_reserved.dto.response;

import horse_reserved.model.chatbot.Action;
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
 * DTO de salida del chatbot.
 * Contiene la respuesta generada, el intent detectado
 * y metadatos asociados al matching.
 */
public class ChatbotAnswerResponse {
    private String intentId;
    private double confidence;
    private String answer;
    private Action action;
    @Builder.Default
    private List<String> notes = List.of();
    @Builder.Default
    private List<String> suggestions = List.of();
}