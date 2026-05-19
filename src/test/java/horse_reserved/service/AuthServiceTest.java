package horse_reserved.service;

import horse_reserved.dto.request.ChangePasswordRequest;
import horse_reserved.dto.request.LoginRequest;
import horse_reserved.dto.request.RegisterRequest;
import horse_reserved.dto.request.ResendTwoFactorRequest;
import horse_reserved.dto.request.VerifyTwoFactorRequest;
import horse_reserved.dto.response.AuthResponse;
import horse_reserved.dto.response.ResendTwoFactorResponse;
import horse_reserved.dto.response.TwoFactorChallengeResponse;
import horse_reserved.dto.response.UserProfileResponse;
import horse_reserved.exception.EmailAlreadyExistsException;
import horse_reserved.exception.InvalidCredentialsException;
import horse_reserved.exception.InvalidOrExpiredTwoFactorCodeException;
import horse_reserved.exception.TwoFactorChallengeBlockedException;
import horse_reserved.exception.TwoFactorResendNotAllowedException;
import horse_reserved.exception.UserInactiveException;
import horse_reserved.model.AuditAccion;
import horse_reserved.model.AuditCategoria;
import horse_reserved.model.LoginTwoFactorChallenge;
import horse_reserved.model.Rol;
import horse_reserved.model.TipoDocumento;
import horse_reserved.model.TwoFactorChallengeStatus;
import horse_reserved.model.Usuario;
import horse_reserved.repository.LoginTwoFactorChallengeRepository;
import horse_reserved.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────────────

    @Mock UsuarioRepository                 usuarioRepository;
    @Mock PasswordEncoder                   passwordEncoder;
    @Mock JwtService                        jwtService;
    @Mock AuthenticationManager             authenticationManager;
    @Mock AuditLogService                   auditLogService;
    @Mock EmailService                      emailService;
    @Mock LoginTwoFactorChallengeRepository challengeRepository;

    @InjectMocks AuthService authService;

    // ── Constantes de test ───────────────────────────────────────────────────

    private static final String EMAIL        = "juan@test.com";
    private static final String CHALLENGE_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String HASHED_OTP   = "$2a$10$hashedOtp";
    private static final String CLIENT_IP    = "127.0.0.1";

    // ── Fixture ──────────────────────────────────────────────────────────────

    private Usuario usuarioCliente;

    @BeforeEach
    void setUp() {
        // Valores @Value que Spring no inyecta en tests unitarios
        ReflectionTestUtils.setField(authService, "jwtExpiration",          3_600_000L);
        ReflectionTestUtils.setField(authService, "otpLength",              6);
        ReflectionTestUtils.setField(authService, "otpTtlSeconds",          300);
        ReflectionTestUtils.setField(authService, "maxAttempts",            5);
        ReflectionTestUtils.setField(authService, "maxResends",             3);
        ReflectionTestUtils.setField(authService, "resendCooldownSeconds",  60);

        usuarioCliente = Usuario.builder()
                .id(1L)
                .primerNombre("Juan")
                .primerApellido("Pérez")
                .email(EMAIL)
                .passwordHash("$2a$12$hashedpassword")
                .role(Rol.CLIENTE)
                .isActive(true)
                .tipoDocumento(TipoDocumento.CEDULA)
                .documento("123456789")
                .passwordChangedAt(Instant.EPOCH)
                .build();

        // Contexto de seguridad para métodos que leen el principal
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // register
    // =========================================================================

    @Test
    @DisplayName("register — email nuevo → devuelve AuthResponse con token")
    void register_emailNuevo_retornaAuthResponse() {
        when(usuarioRepository.existsByEmailAndIsActive(EMAIL, true)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(usuarioRepository.save(any())).thenReturn(usuarioCliente);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt.token.here");

        AuthResponse resp = authService.register(registerRequest());

        assertThat(resp.getToken()).isEqualTo("jwt.token.here");
        assertThat(resp.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("register — email duplicado → EmailAlreadyExistsException, nunca guarda")
    void register_emailDuplicado_lanzaEmailAlreadyExists() {
        when(usuarioRepository.existsByEmailAndIsActive(EMAIL, true)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — rol asignado automáticamente es CLIENTE")
    void register_usuarioCreado_rolEsCliente() {
        when(usuarioRepository.existsByEmailAndIsActive(anyString(), anyBoolean())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenReturn(usuarioCliente);
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        authService.register(registerRequest());

        assertThat(captor.getValue().getRole()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    @DisplayName("register — password se guarda hasheada, nunca en texto plano")
    void register_passwordHasheado() {
        when(usuarioRepository.existsByEmailAndIsActive(anyString(), anyBoolean())).thenReturn(false);
        when(passwordEncoder.encode("Clave$ecreta123!")).thenReturn("$2a$12$hashed");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenReturn(usuarioCliente);
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        authService.register(registerRequest());

        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$12$hashed");
    }

    // =========================================================================
    // loginStep1 — Paso 1 del nuevo flujo 2FA
    // =========================================================================

    @Test
    @DisplayName("loginStep1 — credenciales válidas → devuelve challenge, NUNCA emite JWT")
    void loginStep1_credencialesValidas_retornaChallengeSinJwt() {
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true))
                .thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_OTP);
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TwoFactorChallengeResponse resp = authService.loginStep1(loginRequest(), CLIENT_IP);

        assertThat(resp.getChallengeId()).isNotBlank();
        assertThat(resp.isRequiresVerification()).isTrue();
        assertThat(resp.getExpiresInSeconds()).isEqualTo(300);
        // CRÍTICO: JwtService no debe invocarse en el Paso 1
        verifyNoInteractions(jwtService);
        verify(emailService).sendOtpLoginEmail(eq(EMAIL), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("loginStep1 — invalida challenges PENDING previos del usuario")
    void loginStep1_invalidaChallengesPrevios() {
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true))
                .thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_OTP);
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.loginStep1(loginRequest(), CLIENT_IP);

        verify(challengeRepository).invalidatePendingByUsuarioId(
                eq(1L),
                eq(TwoFactorChallengeStatus.PENDING),
                eq(TwoFactorChallengeStatus.EXPIRED));
    }

    @Test
    @DisplayName("loginStep1 — credenciales inválidas → InvalidCredentialsException + auditoría de fallo")
    void loginStep1_credencialesInvalidas_lanzaExcepcionYAudita() {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.loginStep1(loginRequest(), CLIENT_IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(auditLogService).registrarFallo(
                isNull(), eq(EMAIL),
                eq(AuditCategoria.AUTENTICACION), eq(AuditAccion.LOGIN_FALLIDO),
                anyString(), eq(CLIENT_IP));
        verifyNoInteractions(jwtService);
        verifyNoInteractions(challengeRepository);
    }

    @Test
    @DisplayName("loginStep1 — usuario inactivo → UserInactiveException")
    void loginStep1_usuarioInactivo_lanzaUserInactive() {
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginStep1(loginRequest(), CLIENT_IP))
                .isInstanceOf(UserInactiveException.class);

        verifyNoInteractions(jwtService);
        verifyNoInteractions(emailService);
    }

    // =========================================================================
    // verifyTwoFactor — Paso 2: verificar OTP y emitir JWT
    // =========================================================================

    @Test
    @DisplayName("verifyTwoFactor — OTP correcto → AuthResponse con JWT, challenge VERIFIED")
    void verifyTwoFactor_otpCorrecto_retornaJwt() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", HASHED_OTP)).thenReturn(true);
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCliente));
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        AuthResponse resp = authService.verifyTwoFactor(verifyRequest(CHALLENGE_ID, "123456"), CLIENT_IP);

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getType()).isEqualTo("Bearer");

        ArgumentCaptor<LoginTwoFactorChallenge> captor =
                ArgumentCaptor.forClass(LoginTwoFactorChallenge.class);
        verify(challengeRepository, atLeastOnce()).save(captor.capture());
        boolean verifiedSaved = captor.getAllValues().stream()
                .anyMatch(c -> c.getStatus() == TwoFactorChallengeStatus.VERIFIED
                        && c.getConsumedAt() != null
                        && CLIENT_IP.equals(c.getVerifiedIp()));
        assertThat(verifiedSaved).isTrue();
    }

    @Test
    @DisplayName("verifyTwoFactor — OTP incorrecto → incrementa attemptCount, sigue PENDING")
    void verifyTwoFactor_otpIncorrecto_incrementaIntentos() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setAttemptCount(0);
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCliente));

        assertThatThrownBy(() ->
                authService.verifyTwoFactor(verifyRequest(CHALLENGE_ID, "000000"), CLIENT_IP))
                .isInstanceOf(InvalidOrExpiredTwoFactorCodeException.class);

        verify(challengeRepository).save(argThat(c ->
                c.getAttemptCount() == 1 && c.getStatus() == TwoFactorChallengeStatus.PENDING));
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("verifyTwoFactor — máximo de intentos → challenge BLOCKED + excepción")
    void verifyTwoFactor_maximoIntentos_bloqueaChallenge() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setAttemptCount(4); // El próximo intento (5.º) dispara el bloqueo
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCliente));

        assertThatThrownBy(() ->
                authService.verifyTwoFactor(verifyRequest(CHALLENGE_ID, "000000"), CLIENT_IP))
                .isInstanceOf(TwoFactorChallengeBlockedException.class);

        verify(challengeRepository).save(argThat(c ->
                c.getStatus() == TwoFactorChallengeStatus.BLOCKED));
        verify(auditLogService, atLeastOnce()).registrarFallo(
                any(), any(),
                eq(AuditCategoria.AUTENTICACION),
                eq(AuditAccion.LOGIN_2FA_CHALLENGE_BLOQUEADO),
                anyString(), eq(CLIENT_IP));
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("verifyTwoFactor — challenge expirado (lazy) → EXPIRED + excepción")
    void verifyTwoFactor_challengeExpirado_marcaExpiredYLanzaExcepcion() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() ->
                authService.verifyTwoFactor(verifyRequest(CHALLENGE_ID, "123456"), CLIENT_IP))
                .isInstanceOf(InvalidOrExpiredTwoFactorCodeException.class);

        verify(challengeRepository).save(argThat(c ->
                c.getStatus() == TwoFactorChallengeStatus.EXPIRED));
        verifyNoInteractions(jwtService);
        verifyNoInteractions(passwordEncoder); // La validación de estado ocurre antes del matches()
    }

    @Test
    @DisplayName("verifyTwoFactor — challenge BLOCKED → TwoFactorChallengeBlockedException sin evaluar OTP")
    void verifyTwoFactor_challengeBloqueado_lanzaExcepcionSinEvaluarOtp() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setStatus(TwoFactorChallengeStatus.BLOCKED);
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() ->
                authService.verifyTwoFactor(verifyRequest(CHALLENGE_ID, "123456"), CLIENT_IP))
                .isInstanceOf(TwoFactorChallengeBlockedException.class);

        verifyNoInteractions(jwtService);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("verifyTwoFactor — challenge VERIFIED → no se puede reutilizar")
    void verifyTwoFactor_challengeYaVerificado_lanzaExcepcion() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setStatus(TwoFactorChallengeStatus.VERIFIED);
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() ->
                authService.verifyTwoFactor(verifyRequest(CHALLENGE_ID, "123456"), CLIENT_IP))
                .isInstanceOf(InvalidOrExpiredTwoFactorCodeException.class);

        verifyNoInteractions(jwtService);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("verifyTwoFactor — challengeId inexistente → InvalidOrExpiredTwoFactorCodeException")
    void verifyTwoFactor_challengeNoEncontrado_lanzaExcepcion() {
        when(challengeRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.verifyTwoFactor(verifyRequest("no-existe", "123456"), CLIENT_IP))
                .isInstanceOf(InvalidOrExpiredTwoFactorCodeException.class);
    }

    @Test
    @DisplayName("verifyTwoFactor — éxito → auditoría LOGIN_2FA_OTP_VERIFICADO")
    void verifyTwoFactor_exito_auditaVerificacion() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", HASHED_OTP)).thenReturn(true);
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCliente));
        when(jwtService.generateToken(any(), any())).thenReturn("jwt");

        authService.verifyTwoFactor(verifyRequest(CHALLENGE_ID, "123456"), CLIENT_IP);

        verify(auditLogService).registrarExito(
                eq(1L), eq(EMAIL),
                eq(AuditCategoria.AUTENTICACION), eq(AuditAccion.LOGIN_2FA_OTP_VERIFICADO),
                anyString(), any(), eq(CLIENT_IP));
    }

    // =========================================================================
    // resendTwoFactor — Reenvío de OTP
    // =========================================================================

    @Test
    @DisplayName("resendTwoFactor — dentro de límites → OTP regenerado, resendCount++, attemptCount=0")
    void resendTwoFactor_dentroLimites_regeneraOtpCorrectamente() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setLastSentAt(LocalDateTime.now().minusSeconds(90)); // cooldown superado
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.encode(anyString())).thenReturn("newHash");

        ResendTwoFactorResponse resp =
                authService.resendTwoFactor(resendRequest(CHALLENGE_ID), CLIENT_IP);

        assertThat(resp.getMessage()).isNotBlank();
        verify(challengeRepository).save(argThat(c ->
                c.getResendCount() == 1
                        && c.getAttemptCount() == 0
                        && "newHash".equals(c.getOtpHash())));
        verify(emailService).sendOtpLoginEmail(eq(EMAIL), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("resendTwoFactor — cooldown activo (< 60s) → TwoFactorResendNotAllowedException")
    void resendTwoFactor_cooldownActivo_lanzaExcepcion() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setLastSentAt(LocalDateTime.now().minusSeconds(10));
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() ->
                authService.resendTwoFactor(resendRequest(CHALLENGE_ID), CLIENT_IP))
                .isInstanceOf(TwoFactorResendNotAllowedException.class)
                .hasMessageContaining("esperar");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("resendTwoFactor — límite de reenvíos alcanzado → TwoFactorResendNotAllowedException")
    void resendTwoFactor_limiteAlcanzado_lanzaExcepcion() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setResendCount(3); // maxResends = 3
        challenge.setLastSentAt(LocalDateTime.now().minusSeconds(120));
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() ->
                authService.resendTwoFactor(resendRequest(CHALLENGE_ID), CLIENT_IP))
                .isInstanceOf(TwoFactorResendNotAllowedException.class)
                .hasMessageContaining("límite");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("resendTwoFactor — challenge expirado → InvalidOrExpiredTwoFactorCodeException")
    void resendTwoFactor_challengeExpirado_lanzaExcepcion() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() ->
                authService.resendTwoFactor(resendRequest(CHALLENGE_ID), CLIENT_IP))
                .isInstanceOf(InvalidOrExpiredTwoFactorCodeException.class);

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("resendTwoFactor — challenge no PENDING (ej. VERIFIED) → InvalidOrExpiredTwoFactorCodeException")
    void resendTwoFactor_noPending_lanzaExcepcion() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setStatus(TwoFactorChallengeStatus.VERIFIED);
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() ->
                authService.resendTwoFactor(resendRequest(CHALLENGE_ID), CLIENT_IP))
                .isInstanceOf(InvalidOrExpiredTwoFactorCodeException.class);

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("resendTwoFactor — éxito → auditoría LOGIN_2FA_OTP_REENVIADO")
    void resendTwoFactor_exito_auditaReenvio() {
        LoginTwoFactorChallenge challenge = pendingChallenge();
        challenge.setLastSentAt(LocalDateTime.now().minusSeconds(120));
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.encode(anyString())).thenReturn("newHash");

        authService.resendTwoFactor(resendRequest(CHALLENGE_ID), CLIENT_IP);

        verify(auditLogService).registrarExito(
                eq(1L), eq(EMAIL),
                eq(AuditCategoria.AUTENTICACION), eq(AuditAccion.LOGIN_2FA_OTP_REENVIADO),
                anyString(), any(), eq(CLIENT_IP));
    }

    // =========================================================================
    // getCurrentUser
    // =========================================================================

    @Test
    @DisplayName("getCurrentUser — usuario autenticado → devuelve perfil correcto")
    void getCurrentUser_usuarioAutenticado_retornaPerfil() {
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true))
                .thenReturn(Optional.of(usuarioCliente));

        UserProfileResponse profile = authService.getCurrentUser();

        assertThat(profile.getEmail()).isEqualTo(EMAIL);
        assertThat(profile.getRole()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("getCurrentUser — usuario no encontrado → InvalidCredentialsException")
    void getCurrentUser_emailNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // =========================================================================
    // changePassword
    // =========================================================================

    @Test
    @DisplayName("changePassword — password actual correcta → actualiza hash")
    void changePassword_passwordActualCorrecta_actualizaHash() {
        ChangePasswordRequest req = new ChangePasswordRequest(
                "actual123", "Nueva$ecreta123!", "Nueva$ecreta123!");
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true))
                .thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.matches("actual123", usuarioCliente.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("Nueva$ecreta123!", usuarioCliente.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("Nueva$ecreta123!")).thenReturn("$2a$12$newHash");
        when(usuarioRepository.save(any())).thenReturn(usuarioCliente);

        assertThatCode(() -> authService.changePassword(req)).doesNotThrowAnyException();
        verify(usuarioRepository).save(argThat(u -> u.getPasswordHash().equals("$2a$12$newHash")));
    }

    @Test
    @DisplayName("changePassword — password actual incorrecta → InvalidCredentialsException")
    void changePassword_passwordActualIncorrecta_lanzaExcepcion() {
        ChangePasswordRequest req = new ChangePasswordRequest(
                "wrongPass", "Nueva$ecreta123!", "Nueva$ecreta123!");
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true))
                .thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.matches("wrongPass", usuarioCliente.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("changePassword — confirmación no coincide → InvalidCredentialsException")
    void changePassword_confirmacionNoCoincide_lanzaExcepcion() {
        ChangePasswordRequest req = new ChangePasswordRequest(
                "actual123", "Nueva$ecreta123!", "Distinta$123!");
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true))
                .thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.matches("actual123", usuarioCliente.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("changePassword — nueva igual a actual → InvalidCredentialsException")
    void changePassword_nuevaIgualActual_lanzaExcepcion() {
        ChangePasswordRequest req = new ChangePasswordRequest(
                "Clave$ecreta123!", "Clave$ecreta123!", "Clave$ecreta123!");
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true))
                .thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.matches("Clave$ecreta123!", usuarioCliente.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("changePassword — usuario OAuth2 sin hash → InvalidCredentialsException")
    void changePassword_usuarioOAuth2SinHash_lanzaExcepcion() {
        usuarioCliente.setPasswordHash(null);
        ChangePasswordRequest req = new ChangePasswordRequest(
                "cualquiera", "Nueva$ecreta123!", "Nueva$ecreta123!");
        when(usuarioRepository.findByEmailAndIsActive(EMAIL, true))
                .thenReturn(Optional.of(usuarioCliente));

        assertThatThrownBy(() -> authService.changePassword(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // =========================================================================
    // Builders de objetos de test
    // =========================================================================

    private LoginRequest loginRequest() {
        return LoginRequest.builder()
                .email(EMAIL)
                .password("Clave$ecreta123!")
                .recaptchaToken("token-recaptcha")
                .build();
    }

    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .primerNombre("Juan")
                .primerApellido("Pérez")
                .tipoDocumento("cedula")
                .documento("123456789")
                .email(EMAIL)
                .password("Clave$ecreta123!")
                .recaptchaToken("token-recaptcha")
                .habeasDataConsent(true)
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

    private LoginTwoFactorChallenge pendingChallenge() {
        LoginTwoFactorChallenge c = new LoginTwoFactorChallenge();
        c.setId(CHALLENGE_ID);
        c.setUsuarioId(1L);
        c.setOtpHash(HASHED_OTP);
        c.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        c.setMaxAttempts(5);
        c.setAttemptCount(0);
        c.setMaxResends(3);
        c.setResendCount(0);
        c.setLastSentAt(LocalDateTime.now().minusMinutes(2)); // cooldown ya superado por defecto
        c.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        c.setCreatedIp(CLIENT_IP);
        c.setStatus(TwoFactorChallengeStatus.PENDING);
        return c;
    }
}