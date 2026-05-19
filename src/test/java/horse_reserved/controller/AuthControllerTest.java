package horse_reserved.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import horse_reserved.dto.request.ChangePasswordRequest;
import horse_reserved.dto.request.ForgotPasswordRequest;
import horse_reserved.dto.request.LoginRequest;
import horse_reserved.dto.request.RegisterRequest;
import horse_reserved.dto.request.ResendTwoFactorRequest;
import horse_reserved.dto.request.VerifyTwoFactorRequest;
import horse_reserved.dto.response.AuthResponse;
import horse_reserved.dto.response.ResendTwoFactorResponse;
import horse_reserved.dto.response.TwoFactorChallengeResponse;
import horse_reserved.dto.response.UserProfileResponse;
import horse_reserved.exception.EmailAlreadyExistsException;
import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.exception.InvalidCredentialsException;
import horse_reserved.exception.InvalidOrExpiredTwoFactorCodeException;
import horse_reserved.exception.TwoFactorChallengeBlockedException;
import horse_reserved.exception.TwoFactorResendNotAllowedException;
import horse_reserved.exception.UserInactiveException;
import horse_reserved.security.OAuth2TokenStore;
import horse_reserved.service.AuditLogService;
import horse_reserved.service.AuthService;
import horse_reserved.service.PasswordResetService;
import horse_reserved.service.RecaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Mock AuthService         authService;
    @Mock PasswordResetService passwordResetService;
    @Mock OAuth2TokenStore    oauth2TokenStore;
    @Mock RecaptchaService    recaptchaService;
    @Mock AuditLogService     auditLogService;

    MockMvc      mockMvc;
    ObjectMapper objectMapper;

    private static final String CHALLENGE_ID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                authService, passwordResetService, oauth2TokenStore, recaptchaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .addPlaceholderValue("cors.allowed-origins", "*")
                .build();
        objectMapper = new ObjectMapper();
    }

    // =========================================================================
    // POST /api/auth/register
    // =========================================================================

    @Test
    @DisplayName("register — request válido → 201 con token")
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
    @DisplayName("register — sin email → 400")
    void register_sinEmail_retorna400() throws Exception {
        RegisterRequest request = registerRequest();
        request.setEmail(null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register — email con formato inválido → 400")
    void register_emailInvalido_retorna400() throws Exception {
        RegisterRequest request = registerRequest();
        request.setEmail("no-es-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register — password débil → 400")
    void register_passwordDebil_retorna400() throws Exception {
        RegisterRequest request = registerRequest();
        request.setPassword("debil");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register — email duplicado → 409")
    void register_emailDuplicado_retorna409() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.register(any()))
                .thenThrow(new EmailAlreadyExistsException("Email ya registrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest())))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // POST /api/auth/login  —  Paso 1 del flujo 2FA
    // El endpoint ya NO devuelve JWT; devuelve challengeId con HTTP 202.
    // =========================================================================

    @Test
    @DisplayName("login — credenciales válidas → 202 con challengeId, SIN token JWT")
    void login_credencialesValidas_retorna202ConChallenge() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.loginStep1(any(), anyString()))
                .thenReturn(new TwoFactorChallengeResponse(CHALLENGE_ID, "OTP enviado", 300));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isAccepted())                          // 202
                .andExpect(jsonPath("$.challengeId").value(CHALLENGE_ID))
                .andExpect(jsonPath("$.requiresVerification").value(true))
                .andExpect(jsonPath("$.token").doesNotExist());            // CRÍTICO: sin JWT en paso 1
    }

    @Test
    @DisplayName("login — sin email → 400 (validación Bean Validation)")
    void login_sinEmail_retorna400() throws Exception {
        LoginRequest request = loginRequest();
        request.setEmail(null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login — credenciales inválidas → 401")
    void login_credencialesInvalidas_retorna401() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.loginStep1(any(), anyString()))
                .thenThrow(new InvalidCredentialsException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login — usuario inactivo → 403")
    void login_usuarioInactivo_retorna403() throws Exception {
        doNothing().when(recaptchaService).verify(anyString());
        when(authService.loginStep1(any(), anyString()))
                .thenThrow(new UserInactiveException("Usuario inactivo"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/auth/login/verify-2fa  —  Paso 2: OTP → JWT
    // =========================================================================

    @Test
    @DisplayName("verify-2fa — OTP correcto → 200 con JWT")
    void verifyTwoFactor_otpCorrecto_retorna200ConJwt() throws Exception {
        when(authService.verifyTwoFactor(any(), anyString())).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login/verify-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest(CHALLENGE_ID, "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    @DisplayName("verify-2fa — OTP inválido/expirado → 401")
    void verifyTwoFactor_otpInvalido_retorna401() throws Exception {
        when(authService.verifyTwoFactor(any(), anyString()))
                .thenThrow(new InvalidOrExpiredTwoFactorCodeException("Código inválido o expirado."));

        mockMvc.perform(post("/api/auth/login/verify-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest(CHALLENGE_ID, "000000"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Código inválido o expirado."));
    }

    @Test
    @DisplayName("verify-2fa — challenge bloqueado → 403")
    void verifyTwoFactor_challengeBloqueado_retorna403() throws Exception {
        when(authService.verifyTwoFactor(any(), anyString()))
                .thenThrow(new TwoFactorChallengeBlockedException("Desafío bloqueado."));

        mockMvc.perform(post("/api/auth/login/verify-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest(CHALLENGE_ID, "123456"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("verify-2fa — OTP con letras (falla @Pattern) → 400")
    void verifyTwoFactor_otpConLetras_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login/verify-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest(CHALLENGE_ID, "ABC123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("verify-2fa — OTP con menos de 6 dígitos → 400")
    void verifyTwoFactor_otpCorto_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login/verify-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest(CHALLENGE_ID, "123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("verify-2fa — challengeId ausente → 400")
    void verifyTwoFactor_sinChallengeId_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login/verify-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // POST /api/auth/login/resend-2fa  —  Reenvío de OTP
    // =========================================================================

    @Test
    @DisplayName("resend-2fa — reenvío exitoso → 200 con remainingSeconds")
    void resendTwoFactor_exitoso_retorna200() throws Exception {
        when(authService.resendTwoFactor(any(), anyString()))
                .thenReturn(new ResendTwoFactorResponse("Código reenviado.", 240));

        mockMvc.perform(post("/api/auth/login/resend-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest(CHALLENGE_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Código reenviado."))
                .andExpect(jsonPath("$.remainingSeconds").value(240));
    }

    @Test
    @DisplayName("resend-2fa — cooldown activo → 429")
    void resendTwoFactor_cooldownActivo_retorna429() throws Exception {
        when(authService.resendTwoFactor(any(), anyString()))
                .thenThrow(new TwoFactorResendNotAllowedException("Debe esperar 45 segundo(s)."));

        mockMvc.perform(post("/api/auth/login/resend-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest(CHALLENGE_ID))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("resend-2fa — límite de reenvíos alcanzado → 429")
    void resendTwoFactor_limiteAlcanzado_retorna429() throws Exception {
        when(authService.resendTwoFactor(any(), anyString()))
                .thenThrow(new TwoFactorResendNotAllowedException("Ha alcanzado el límite de reenvíos."));

        mockMvc.perform(post("/api/auth/login/resend-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest(CHALLENGE_ID))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("resend-2fa — challengeId ausente → 400")
    void resendTwoFactor_sinChallengeId_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login/resend-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET /api/auth/me
    // =========================================================================

    @Test
    @DisplayName("me — usuario autenticado → 200 con perfil")
    void me_retorna200ConPerfil() throws Exception {
        when(authService.getCurrentUser()).thenReturn(
                UserProfileResponse.builder().email("juan@test.com").role("CLIENTE").build());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@test.com"));
    }

    // =========================================================================
    // PUT /api/auth/change-password
    // =========================================================================

    @Test
    @DisplayName("changePassword — request válido → 200")
    void changePassword_requestValido_retorna200() throws Exception {
        doNothing().when(authService).changePassword(any());

        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("actual123", "Nueva$ecreta123!", "Nueva$ecreta123!"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("changePassword — sin passwordActual → 400")
    void changePassword_sinPasswordActual_retorna400() throws Exception {
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(null, "Nueva$ecreta123!", "Nueva$ecreta123!"))))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // POST /api/auth/forgot-password
    // =========================================================================

    @Test
    @DisplayName("forgotPassword — email válido → 200")
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
    @DisplayName("forgotPassword — email con formato inválido → 400")
    void forgotPassword_emailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("no-email", "recaptcha-token"))))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Builders de objetos de test
    // =========================================================================

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .token("jwt-token").type("Bearer")
                .email("juan@test.com").role("CLIENTE")
                .build();
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
                .recaptchaToken("token-recaptcha")
                .build();
    }

    private VerifyTwoFactorRequest verifyRequest(String challengeId, String otp) {
        VerifyTwoFactorRequest r = new VerifyTwoFactorRequest();
        r.setChallengeId(challengeId);
        r.setOtp(otp);
        return r;
    }

    private ResendTwoFactorRequest resendRequest(String challengeId) {
        ResendTwoFactorRequest r = new ResendTwoFactorRequest();
        r.setChallengeId(challengeId);
        return r;
    }
}