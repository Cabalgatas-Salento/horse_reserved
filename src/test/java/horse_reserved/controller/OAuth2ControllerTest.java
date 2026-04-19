package horse_reserved.controller;

import horse_reserved.dto.response.AuthResponse;
import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.security.OAuth2TokenStore;
import horse_reserved.service.AuditLogService;
import horse_reserved.service.AuthService;
import horse_reserved.service.PasswordResetService;
import horse_reserved.service.RecaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ControllerTest {

    @Mock AuthService authService;
    @Mock PasswordResetService passwordResetService;
    @Mock OAuth2TokenStore oauth2TokenStore;
    @Mock RecaptchaService recaptchaService;
    @Mock AuditLogService auditLogService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService, passwordResetService, oauth2TokenStore, recaptchaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .addPlaceholderValue("cors.allowed-origins", "*")
                .build();
    }

    // ── POST /api/auth/oauth2/token ───────────────────────────────────────────

    @Test
    void exchangeToken_codigoValido_retorna200() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token-de-prueba")
                .type("Bearer")
                .expiresIn(86400L)
                .userId(1L)
                .email("usuario@test.com")
                .role("CLIENTE")
                .build();
        when(oauth2TokenStore.consume(anyString())).thenReturn(Optional.of(authResponse));

        mockMvc.perform(post("/api/auth/oauth2/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"codigo-valido-uuid\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-de-prueba"))
                .andExpect(jsonPath("$.role").value("CLIENTE"));
    }

    @Test
    void exchangeToken_codigoInvalido_retorna400() throws Exception {
        when(oauth2TokenStore.consume(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/oauth2/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"codigo-inexistente-o-expirado\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exchangeToken_sinBody_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/oauth2/token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
