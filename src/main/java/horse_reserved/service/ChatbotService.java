package horse_reserved.service;

import horse_reserved.dto.response.ChatbotAnswerResponse;
import horse_reserved.model.chatbot.FaqIntent;
import horse_reserved.model.chatbot.FaqKnowledgeBase;
import horse_reserved.service.chatbot.FaqKnowledgeBaseProvider;
import horse_reserved.service.chatbot.IntentMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
/**
 * Orquesta la lógica principal del chatbot.
 * Coordina la obtención de intents, matching
 * y construcción de la respuesta final.
 */
public class ChatbotService {

    private final IntentMatcher intentMatcher;
    private final FaqKnowledgeBaseProvider kbProvider;

    public ChatbotService(IntentMatcher intentMatcher, FaqKnowledgeBaseProvider kbProvider) {
        this.intentMatcher = intentMatcher;
        this.kbProvider = kbProvider;
    }

    /**
     * Procesa la pregunta del usuario y retorna la mejor respuesta posible.
     * Aplica validación de threshold y fallback si es necesario.
     */
    @Cacheable(value = "chatbot-faq", key = "#question")
    public ChatbotAnswerResponse answer(String question) {
        log.debug("Consulta chatbot recibida — longitud={} chars", question != null ? question.length() : 0);

        FaqKnowledgeBase kb = kbProvider.getKnowledgeBase();

        IntentMatcher.MatchResult result = intentMatcher.findBestMatch(question, kb.getIntents());
        FaqIntent intent = result.intent();

        if (intent == null) {
            log.info("Chatbot fallback — sin intent detectado, score={}", String.format("%.2f", 0.0));
            return fallback(kb, 0.0);
        }

        double threshold = intent.getThreshold() == null ? 0.70 : intent.getThreshold();
        if (result.score() < threshold) {
            log.info("Chatbot fallback — score={} por debajo del umbral={} para intentId={}",
                    String.format("%.2f", result.score()),
                    String.format("%.2f", threshold),
                    intent.getId());
            return fallback(kb, result.score());
        }

        log.info("Chatbot respuesta — intentId={}, score={}", intent.getId(), String.format("%.2f", result.score()));
        return ChatbotAnswerResponse.builder()
                .intentId(intent.getId())
                .confidence(round(result.score()))
                .answer(intent.getResponse().getText())
                .action(intent.getResponse().getAction())
                .notes(intent.getResponse().getNotes())
                .suggestions(List.of())
                .build();
    }

    /**
     * Construye la respuesta de fallback cuando no hay coincidencia válida.
     */
    private ChatbotAnswerResponse fallback(FaqKnowledgeBase kb, double confidence) {
        return ChatbotAnswerResponse.builder()
                .intentId("fallback")
                .confidence(round(confidence))
                .answer(kb.getFallback().getMessage())
                .suggestions(kb.getFallback().getSuggestions())
                .action(null)
                .build();
    }

    /**
     * Redondea el score a 3 decimales para presentación.
     */
    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}