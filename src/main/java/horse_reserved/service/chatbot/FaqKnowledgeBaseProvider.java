package horse_reserved.service.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import horse_reserved.model.chatbot.FaqKnowledgeBase;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
/**
 * Encargado de cargar, validar y proveer la base de conocimiento del chatbot.
 * Implementa cache en memoria para evitar lecturas repetidas del archivo JSON.
 */
public class FaqKnowledgeBaseProvider {

    private final ObjectMapper objectMapper;
    private volatile FaqKnowledgeBase cache;

    public FaqKnowledgeBaseProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        load();
    }

    /**
     * Carga la base de conocimiento desde el archivo JSON.
     * Es sincronizado para evitar condiciones de carrera en recargas concurrentes.
     */
    public synchronized void load() {
        try (InputStream is = new ClassPathResource("chatbot/faq-intents.json").getInputStream()) {
            FaqKnowledgeBase kb = objectMapper.readValue(is, FaqKnowledgeBase.class);
            validate(kb);
            this.cache = kb;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar chatbot/faq-intents.json", e);
        }
    }

    /**
     * Retorna la base de conocimiento en memoria.
     * Si no está cargada, realiza la carga de forma lazy.
     */
    public FaqKnowledgeBase getKnowledgeBase() {
        if (cache == null) load();
        return cache;
    }

    /**
     * Valida la estructura e integridad de la base de conocimiento.
     * Lanza excepciones si encuentra datos inválidos.
     */
    private void validate(FaqKnowledgeBase kb) {
        if (kb == null) throw new IllegalArgumentException("Knowledge base nula");
        if (kb.getFallback() == null) throw new IllegalArgumentException("Fallback requerido");
        if (kb.getIntents() == null) throw new IllegalArgumentException("Lista de intents requerida");

        kb.getIntents().forEach(i -> {
            if (i.getId() == null || i.getId().isBlank()) {
                throw new IllegalArgumentException("Intent sin id");
            }
            if (i.getResponse() == null || i.getResponse().getText() == null || i.getResponse().getText().isBlank()) {
                throw new IllegalArgumentException("Intent " + i.getId() + " sin response.text");
            }
            if (i.getThreshold() != null && (i.getThreshold() < 0 || i.getThreshold() > 1)) {
                throw new IllegalArgumentException("Threshold inválido en intent " + i.getId());
            }
            if (i.getUtterances() == null) i.setUtterances(List.of());
            if (i.getKeywords() == null) i.setKeywords(List.of());
        });
    }
}