package horse_reserved.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import horse_reserved.dto.request.ChangePasswordRequest;
import horse_reserved.dto.request.ForgotPasswordRequest;
import horse_reserved.dto.request.LoginRequest;
import horse_reserved.dto.request.RegisterRequest;
import horse_reserved.dto.response.AuthResponse;
import horse_reserved.dto.response.UserProfileResponse;
import horse_reserved.exception.EmailAlreadyExistsException;
import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.exception.InvalidCredentialsException;
import horse_reserved.exception.UserInactiveException;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService authService;
    @Mock PasswordResetService passwordResetService;
    @Mock OAuth2TokenStore oauth2TokenStore;
    @Mock RecaptchaService recaptchaService;
    @Mock AuditLogService auditLogService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService, passwordResetService, oauth2TokenStore, recaptchaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .addPlaceholderValue("cors.allowed-origins", "*")
                .build();
        objectMapper = new ObjectMapper();
    }

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Test
    void register_requestValido_retorna201() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.register(any())).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("CLIENTE"));
    }

    @Test
    void register_sinEmail_retorna400() throws Exception {
        RegisterRequest request = registerRequest();
        request.setEmail(null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emailInvalido_retorna400() throws Exception {
        RegisterRequest request = registerRequest();
        request.setEmail("no-es-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordDebil_retorna400() throws Exception {
        RegisterRequest request = registerRequest();
        request.setPassword("debil");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emailDuplicado_retorna409() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("Email ya registrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest())))
                .andExpect(status().isConflict());
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Test
    void login_credencialesValidas_retorna200() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.login(any())).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_sinEmail_retorna400() throws Exception {
        LoginRequest request = loginRequest();
        request.setEmail(null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_usuarioInactivo_retorna403() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.login(any())).thenThrow(new UserInactiveException("Usuario inactivo"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    void me_retorna200ConPerfil() throws Exception {
        when(authService.getCurrentUser()).thenReturn(
                UserProfileResponse.builder().email("juan@test.com").role("CLIENTE").build());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@test.com"));
    }

    // ── PUT /api/auth/change-password ─────────────────────────────────────────

    @Test
    void changePassword_requestValido_retorna200() throws Exception {
        doNothing().when(authService).changePassword(any());

        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("actual123", "Nueva$ecreta123!", "Nueva$ecreta123!"))))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_sinPasswordActual_retorna400() throws Exception {
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(null, "Nueva$ecreta123!", "Nueva$ecreta123!"))))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/forgot-password ────────────────────────────────────────

    @Test
    void forgotPassword_emailValido_retorna200() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        doNothing().when(passwordResetService).processForgotPassword(anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("juan@test.com", "recaptcha-token"))))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_emailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("no-email", "recaptcha-token"))))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .token("jwt-token").type("Bearer").email("juan@test.com").role("CLIENTE").build();
    }

    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .primerNombre("Juan").primerApellido("Pérez")
                .tipoDocumento("cedula").documento("123456789")
                .email("juan@test.com").password("Clave$ecreta123!")
                .recaptchaToken("token-recaptcha").habeasDataConsent(true)
                .build();
    }

    private LoginRequest loginRequest() {
        return LoginRequest.builder()
                .email("juan@test.com").password("Clave$ecreta123!")
                .recaptchaToken("token-recaptcha").build();
    }
}
