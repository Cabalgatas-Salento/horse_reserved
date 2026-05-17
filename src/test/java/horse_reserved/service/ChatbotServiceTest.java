package horse_reserved.service;

import horse_reserved.dto.request.ChatbotQueryRequest;
import horse_reserved.dto.response.ChatbotAnswerResponse;
import horse_reserved.model.chatbot.FaqIntent;
import horse_reserved.model.chatbot.FaqKnowledgeBase;
import horse_reserved.model.chatbot.FaqKnowledgeBase.Fallback;
import horse_reserved.service.chatbot.ChatbotReservationFlowService;
import horse_reserved.service.chatbot.FaqKnowledgeBaseProvider;
import horse_reserved.service.chatbot.IntentMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock private IntentMatcher intentMatcher;
    @Mock private FaqKnowledgeBaseProvider kbProvider;
    @Mock private ChatbotReservationFlowService reservationFlowService;

    @InjectMocks
    private ChatbotService chatbotService;

    // ── FAQ normales ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("FAQ normal con confianza suficiente devuelve respuesta del intent")
    void cuandoFaqNormal_devuelveRespuestaIntent() {
        FaqKnowledgeBase kb = buildKb();
        given(kbProvider.getKnowledgeBase()).willReturn(kb);

        FaqIntent horarios = buildIntent("horarios", "Nuestros horarios son de 8:30 a 14:30");
        IntentMatcher.MatchResult matchResult = new IntentMatcher.MatchResult(horarios, 0.85);
        given(intentMatcher.findBestMatch(eq("cuáles son los horarios"), anyList())).willReturn(matchResult);

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question("cuáles son los horarios")
                .build();

        ChatbotAnswerResponse response = chatbotService.answer(request);

        assertThat(response.getIntentId()).isEqualTo("horarios");
        assertThat(response.getAnswer()).isEqualTo("Nuestros horarios son de 8:30 a 14:30");
        verify(reservationFlowService, never()).handle(any(), anyDouble());
    }

    @Test
    @DisplayName("Pregunta sin match suficiente devuelve fallback")
    void cuandoBajaConfianza_devuelveFallback() {
        FaqKnowledgeBase kb = buildKb();
        given(kbProvider.getKnowledgeBase()).willReturn(kb);

        FaqIntent intent = buildIntent("horarios", "Horarios...");
        intent.setThreshold(0.75);
        IntentMatcher.MatchResult matchResult = new IntentMatcher.MatchResult(intent, 0.50);
        given(intentMatcher.findBestMatch(anyString(), anyList())).willReturn(matchResult);

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question("algo muy raro que no reconoce")
                .build();

        ChatbotAnswerResponse response = chatbotService.answer(request);

        assertThat(response.getIntentId()).isEqualTo("fallback");
    }

    // ── Delegación a flujo de reserva ─────────────────────────────────────────

    @Test
    @DisplayName("Intent crear_reserva delega a ChatbotReservationFlowService")
    void cuandoIntentCrearReserva_delega_al_flujo() {
        FaqKnowledgeBase kb = buildKb();
        given(kbProvider.getKnowledgeBase()).willReturn(kb);

        FaqIntent crearReserva = buildIntent("crear_reserva", "Para reservar...");
        crearReserva.setThreshold(0.74);
        IntentMatcher.MatchResult matchResult = new IntentMatcher.MatchResult(crearReserva, 0.80);
        given(intentMatcher.findBestMatch(anyString(), anyList())).willReturn(matchResult);

        ChatbotAnswerResponse expected = ChatbotAnswerResponse.builder()
                .intentId("crear_reserva")
                .flow("crear_reserva")
                .step("SELECT_ROUTE")
                .awaitingUserInput(true)
                .build();
        given(reservationFlowService.handle(any(), anyDouble())).willReturn(expected);

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question("quiero reservar")
                .build();

        ChatbotAnswerResponse response = chatbotService.answer(request);

        verify(reservationFlowService).handle(any(ChatbotQueryRequest.class), anyDouble());
        assertThat(response.getFlow()).isEqualTo("crear_reserva");
        assertThat(response.isAwaitingUserInput()).isTrue();
    }

    @Test
    @DisplayName("Request con sessionId delega siempre al flujo sin clasificar intent")
    void cuandoHaySessionId_delega_directo_al_flujo() {
        ChatbotAnswerResponse expected = ChatbotAnswerResponse.builder()
                .intentId("crear_reserva")
                .flow("crear_reserva")
                .step("SELECT_DATE")
                .awaitingUserInput(true)
                .build();
        given(reservationFlowService.handle(any(), eq(1.0))).willReturn(expected);

        ChatbotQueryRequest request = ChatbotQueryRequest.builder()
                .question("2026-06-15")
                .sessionId("some-session-id")
                .payload(java.util.Map.of("date", "2026-06-15"))
                .build();

        ChatbotAnswerResponse response = chatbotService.answer(request);

        verify(kbProvider, never()).getKnowledgeBase();
        assertThat(response.getStep()).isEqualTo("SELECT_DATE");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FaqKnowledgeBase buildKb() {
        Fallback fallback = new Fallback();
        fallback.setMessage("No entendí tu pregunta. ¿Puedes reformularla?");
        fallback.setSuggestions(List.of("¿Cuáles son los horarios?", "¿Cómo reservo?"));

        FaqKnowledgeBase kb = new FaqKnowledgeBase();
        kb.setFallback(fallback);
        kb.setIntents(List.of());
        return kb;
    }

    private FaqIntent buildIntent(String id, String responseText) {
        FaqIntent intent = new FaqIntent();
        intent.setId(id);
        intent.setThreshold(0.70);
        FaqIntent.Response response = new FaqIntent.Response();
        response.setText(responseText);
        intent.setResponse(response);
        return intent;
    }
}