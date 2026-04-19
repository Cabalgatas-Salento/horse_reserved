package horse_reserved.model.chatbot;

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
/**
 * Define un intent del chatbot basado en FAQ.
 * Contiene la información necesaria para hacer matching
 * y generar una respuesta.
 */
public class FaqIntent {
    private String id;
    private String categoria;
    private Double threshold;
    private List<String> utterances;
    private List<String> keywords;
    private Map<String, List<String>> synonyms;
    private Response response;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    /**
     * Contenedor de la respuesta asociada al intent.
     */
    public static class Response {
        private String text;
        private Action action;
        private String endpoint;
        private Boolean authRequired;
        private List<String> notes;
    }
}