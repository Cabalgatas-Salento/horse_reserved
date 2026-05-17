package horse_reserved.service;

import horse_reserved.dto.request.ChatbotQueryRequest;
import horse_reserved.dto.response.ChatbotAnswerResponse;
import horse_reserved.model.chatbot.FaqIntent;
import horse_reserved.model.chatbot.FaqKnowledgeBase;
import horse_reserved.service.chatbot.ChatbotReservationFlowService;
import horse_reserved.service.chatbot.FaqKnowledgeBaseProvider;
import horse_reserved.service.chatbot.IntentMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio principal del chatbot.
 *
 * Cambios respecto a la versión anterior:
 *  - La firma pasa de {@code answer(String question)} a {@code answer(ChatbotQueryRequest request)}
 *    para acceder a sessionId y payload sin romper compatibilidad de la respuesta.
 *  - La caché {@code @Cacheable} se mueve a un méthodo interno privado que SOLO se aplica
 *    a intents FAQ estáticos. El intent {@code crear_reserva} y cualquier request con
 *    {@code sessionId} nunca se cachean.
 *  - Delega el intent {@code crear_reserva} a {@link ChatbotReservationFlowService}.
 */
@Slf4j
@Service
public class ChatbotService {

    private static final String INTENT_CREAR_RESERVA = "crear_reserva";

    private final IntentMatcher intentMatcher;
    private final FaqKnowledgeBaseProvider kbProvider;
    private final ChatbotReservationFlowService reservationFlowService;

    public ChatbotService(IntentMatcher intentMatcher,
                          FaqKnowledgeBaseProvider kbProvider,
                          ChatbotReservationFlowService reservationFlowService) {
        this.intentMatcher = intentMatcher;
        this.kbProvider = kbProvider;
        this.reservationFlowService = reservationFlowService;
    }

    /**
     * Punto de entrada principal del chatbot.
     * <p>
     * — Si la request trae {@code sessionId}, el flujo de reserva ya está activo:
     *   se delega directamente al {@link ChatbotReservationFlowService}.
     * <p>
     * — Si se detecta el intent {@code crear_reserva}, se inicia el flujo conversacional.
     * <p>
     * — Para cualquier otro intent FAQ se usa caché por texto de pregunta.
     */
    public ChatbotAnswerResponse answer(ChatbotQueryRequest request) {
        // 1. Continuar sesión de reserva en curso (sin pasar por clasificación de intent)
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return reservationFlowService.handle(request, 1.0);
        }

        FaqKnowledgeBase kb = kbProvider.getKnowledgeBase();
        String question = request.getQuestion() != null ? request.getQuestion() : "";

        IntentMatcher.MatchResult result = intentMatcher.findBestMatch(question, kb.getIntents());
        FaqIntent intent = result.intent();

        if (intent == null) {
            return fallback(kb, 0.0);
        }

        double threshold = intent.getThreshold() == null ? 0.70 : intent.getThreshold();
        if (result.score() < threshold) {
            return fallback(kb, result.score());
        }

        // 2. Intent de reserva → flujo conversacional (nunca cacheado)
        if (INTENT_CREAR_RESERVA.equals(intent.getId())) {
            return reservationFlowService.handle(request, round(result.score()));
        }

        // 3. Resto de FAQ → respuesta cacheada por texto de pregunta
        return cachedFaqAnswer(question, kb);
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    /**
     * Respuesta cacheada SOLO para FAQ estáticas.
     * El intent {@code crear_reserva} nunca llega aquí.
     */
    @Cacheable(value = "chatbot-faq", key = "#question")
    public ChatbotAnswerResponse cachedFaqAnswer(String question, FaqKnowledgeBase kb) {
        IntentMatcher.MatchResult result = intentMatcher.findBestMatch(question, kb.getIntents());
        FaqIntent intent = result.intent();

        if (intent == null) return fallback(kb, 0.0);

        double threshold = intent.getThreshold() == null ? 0.70 : intent.getThreshold();
        if (result.score() < threshold) return fallback(kb, result.score());

        return ChatbotAnswerResponse.builder()
                .intentId(intent.getId())
                .confidence(round(result.score()))
                .answer(intent.getResponse().getText())
                .action(intent.getResponse().getAction())
                .notes(intent.getResponse().getNotes())
                .suggestions(List.of())
                .build();
    }

    private ChatbotAnswerResponse fallback(FaqKnowledgeBase kb, double confidence) {
        return ChatbotAnswerResponse.builder()
                .intentId("fallback")
                .confidence(round(confidence))
                .answer(kb.getFallback().getMessage())
                .suggestions(kb.getFallback().getSuggestions())
                .action(null)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}