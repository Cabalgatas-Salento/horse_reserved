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
     * Endpoint principal para consultar el chatbot.
     * Recibe una pregunta y retorna la respuesta generada.
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatbotAnswerResponse> ask(@Valid @RequestBody ChatbotQueryRequest request) {
        return ResponseEntity.ok(chatbotService.answer(request.getQuestion()));
    }

    /**
     * Endpoint de verificación de estado del servicio.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}