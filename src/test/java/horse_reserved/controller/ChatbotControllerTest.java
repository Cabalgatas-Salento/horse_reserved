package horse_reserved.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import horse_reserved.dto.request.ChatbotQueryRequest;
import horse_reserved.dto.response.ChatbotAnswerResponse;
import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.service.AuditLogService;
import horse_reserved.service.ChatbotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatbotControllerTest {

    @Mock ChatbotService chatbotService;
    @Mock AuditLogService auditLogService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ChatbotController controller = new ChatbotController(chatbotService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .addPlaceholderValue("cors.allowed-origins", "*")
                .build();
        objectMapper = new ObjectMapper();
    }

    // ── POST /api/chatbot/faq/ask ─────────────────────────────────────────────

    @Test
    void ask_preguntaValida_retorna200() throws Exception {
        when(chatbotService.answer(anyString()))
                .thenReturn(ChatbotAnswerResponse.builder().answer("Para reservar debes...").build());

        mockMvc.perform(post("/api/chatbot/faq/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatbotQueryRequest("¿Cómo reservo una cabalgata?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Para reservar debes..."));
    }

    @Test
    void ask_preguntaVacia_retorna400() throws Exception {
        mockMvc.perform(post("/api/chatbot/faq/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatbotQueryRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ask_sinBody_retorna400() throws Exception {
        mockMvc.perform(post("/api/chatbot/faq/ask")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/chatbot/faq/health ───────────────────────────────────────────

    @Test
    void health_retorna200Ok() throws Exception {
        mockMvc.perform(get("/api/chatbot/faq/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }
}
