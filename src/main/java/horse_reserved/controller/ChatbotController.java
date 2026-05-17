package horse_reserved.controller;

import horse_reserved.dto.request.ChatbotQueryRequest;
import horse_reserved.dto.response.ChatbotAnswerResponse;
import horse_reserved.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot/faq")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Único endpoint del chatbot.
     * Acepta preguntas FAQ y, si el usuario está autenticado, inicia o continúa
     * el flujo conversacional de creación de reserva.
     *
     * La validación de {@link ChatbotQueryRequest#hasMessageOrPayload()} garantiza
     * que llegue al menos pregunta o payload (no ambos obligatorios).
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatbotAnswerResponse> ask(@Valid @RequestBody ChatbotQueryRequest request) {
        return ResponseEntity.ok(chatbotService.answer(request));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}