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
import horse_reserved.util.HttpRequestUtil;
import horse_reserved.util.LogMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // ── Dependencias (inyección por constructor vía @RequiredArgsConstructor) ──

    private final UsuarioRepository                  usuarioRepository;
    private final PasswordEncoder                    passwordEncoder;
    private final JwtService                         jwtService;
    private final AuthenticationManager              authenticationManager;
    private final AuditLogService                    auditLogService;
    private final EmailService                       emailService;
    private final LoginTwoFactorChallengeRepository  challengeRepository;

    // ── Configuración general ──────────────────────────────────────────────────

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    // ── Configuración 2FA ─────────────────────────────────────────────────────

    @Value("${auth.2fa.otp.length:6}")
    private int otpLength;

    @Value("${auth.2fa.otp.ttl-seconds:300}")
    private int otpTtlSeconds;

    @Value("${auth.2fa.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${auth.2fa.otp.max-resends:3}")
    private int maxResends;

    @Value("${auth.2fa.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    // SecureRandom es thread-safe; una sola instancia estática es correcto.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // =========================================================================
    // AUTENTICACIÓN — PASO 1: Validar credenciales y emitir challenge 2FA
    // =========================================================================

    /**
     * Autentica email/password. Si son correctos NO emite JWT; en su lugar genera
     * un desafío 2FA, envía el OTP por email y devuelve el challengeId al frontend.
     */
    @Transactional
    public TwoFactorChallengeResponse loginStep1(LoginRequest request, String ip) {
        log.info("Intento de login (paso 1) para: {}", LogMaskUtil.maskEmail(request.getEmail()));

        // 1. Autenticar credenciales
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            log.warn("Login fallido para {}: {}",
                    LogMaskUtil.maskEmail(request.getEmail()), e.getMessage());
            auditLogService.registrarFallo(
                    null, request.getEmail(),
                    AuditCategoria.AUTENTICACION, AuditAccion.LOGIN_FALLIDO,
                    "Credenciales inválidas", ip);
            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }

        // 2. Verificar que el usuario esté activo
        Usuario usuario = usuarioRepository.findByEmailAndIsActive(request.getEmail(), true)
                .orElseThrow(() -> {
                    log.warn("Usuario activo no encontrado tras autenticación: {}",
                            LogMaskUtil.maskEmail(request.getEmail()));
                    auditLogService.registrarFallo(
                            null, request.getEmail(),
                            AuditCategoria.AUTENTICACION, AuditAccion.LOGIN_FALLIDO,
                            "Cuenta inactiva", ip);
                    return new UserInactiveException(
                            "Esta cuenta ha sido dada de baja. Para volver a acceder, crea una nueva cuenta.");
                });

        // 3. Auditar éxito de Paso 1
        auditLogService.registrarExito(
                usuario.getId(), usuario.getEmail(),
                AuditCategoria.AUTENTICACION, AuditAccion.LOGIN_PASO1_EXITOSO,
                "USUARIO", usuario.getId(), ip);

        // 4. Invalidar challenges PENDING previos del mismo usuario
        challengeRepository.invalidatePendingByUsuarioId(
                usuario.getId(),
                TwoFactorChallengeStatus.PENDING,
                TwoFactorChallengeStatus.EXPIRED);

        // 5. Generar OTP y persistir challenge
        String otp     = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        LocalDateTime now = LocalDateTime.now();
        LoginTwoFactorChallenge challenge = new LoginTwoFactorChallenge();
        challenge.setId(UUID.randomUUID().toString());
        challenge.setUsuarioId(usuario.getId());
        challenge.setOtpHash(otpHash);
        challenge.setExpiresAt(now.plusSeconds(otpTtlSeconds));
        challenge.setMaxAttempts(maxAttempts);
        challenge.setMaxResends(maxResends);
        challenge.setLastSentAt(now);
        challenge.setCreatedAt(now);
        challenge.setCreatedIp(ip);
        challengeRepository.save(challenge);

        // 6. Enviar OTP por email (OTP en texto plano solo aquí, nunca se persiste)
        emailService.sendOtpLoginEmail(
                usuario.getEmail(),
                usuario.getPrimerNombre(),
                otp,
                otpTtlSeconds / 60);

        // 7. Auditar envío
        auditLogService.registrarExito(
                usuario.getId(), usuario.getEmail(),
                AuditCategoria.AUTENTICACION, AuditAccion.LOGIN_2FA_OTP_ENVIADO,
                "LoginTwoFactorChallenge", null, ip);

        log.info("Challenge 2FA creado para: {}", LogMaskUtil.maskEmail(request.getEmail()));
        return new TwoFactorChallengeResponse(
                challenge.getId(),
                "Código de verificación enviado a su correo electrónico.",
                otpTtlSeconds);
    }

    // =========================================================================
    // AUTENTICACIÓN — PASO 2: Verificar OTP y emitir JWT
    // =========================================================================

    /**
     * Valida el OTP del challenge. Si es correcto emite el JWT final con la misma
     * estructura de AuthResponse que usaba el login anterior.
     */
    @Transactional
    public AuthResponse verifyTwoFactor(VerifyTwoFactorRequest request, String ip) {

        // 1. Buscar el challenge — mensaje genérico para no revelar existencia
        LoginTwoFactorChallenge challenge = challengeRepository
                .findById(request.getChallengeId())
                .orElseThrow(() -> new InvalidOrExpiredTwoFactorCodeException(
                        "Código inválido o expirado."));

        // 2. Validar estado y expiración (lanza excepción si no está operativo)
        validateChallengeForVerification(challenge, ip);

        // 3. Verificar OTP
        if (!passwordEncoder.matches(request.getOtp(), challenge.getOtpHash())) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);

            if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
                challenge.setStatus(TwoFactorChallengeStatus.BLOCKED);
                challengeRepository.save(challenge);
                loadUserForAudit(challenge.getUsuarioId()).ifPresent(u ->
                        auditLogService.registrarFallo(
                                u.getId(), u.getEmail(),
                                AuditCategoria.AUTENTICACION,
                                AuditAccion.LOGIN_2FA_CHALLENGE_BLOQUEADO,
                                "Máximo de intentos superado", ip));
                throw new TwoFactorChallengeBlockedException(
                        "Demasiados intentos fallidos. El desafío ha sido bloqueado.");
            }

            challengeRepository.save(challenge);
            loadUserForAudit(challenge.getUsuarioId()).ifPresent(u ->
                    auditLogService.registrarFallo(
                            u.getId(), u.getEmail(),
                            AuditCategoria.AUTENTICACION,
                            AuditAccion.LOGIN_2FA_OTP_FALLIDO,
                            "OTP incorrecto, intento #" + challenge.getAttemptCount(), ip));
            throw new InvalidOrExpiredTwoFactorCodeException("Código inválido o expirado.");
        }

        // 4. OTP correcto: consumir el challenge
        challenge.setStatus(TwoFactorChallengeStatus.VERIFIED);
        challenge.setConsumedAt(LocalDateTime.now());
        challenge.setVerifiedIp(ip);
        challengeRepository.save(challenge);

        // 5. Cargar usuario y emitir JWT con los mismos claims del login original
        Usuario usuario = usuarioRepository.findById(challenge.getUsuarioId())
                .orElseThrow(() -> new InvalidOrExpiredTwoFactorCodeException(
                        "Código inválido o expirado."));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", usuario.getId());
        extraClaims.put("role",   usuario.getRole());
        String jwt = jwtService.generateToken(usuario, extraClaims);

        // 6. Auditar login completo
        log.info("Login 2FA completado para usuario id={}", usuario.getId());
        auditLogService.registrarExito(
                usuario.getId(), usuario.getEmail(),
                AuditCategoria.AUTENTICACION, AuditAccion.LOGIN_2FA_OTP_VERIFICADO,
                "USUARIO", usuario.getId(), ip);

        return AuthResponse.builder()
                .token(jwt)
                .type("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .primerNombre(usuario.getPrimerNombre())
                .primerApellido(usuario.getPrimerApellido())
                .role(usuario.getRole().name())
                .build();
    }

    // =========================================================================
    // AUTENTICACIÓN — REENVÍO OTP
    // =========================================================================

    /**
     * Regenera el OTP del challenge activo, invalida el anterior y lo reenvía
     * por email, respetando límites de cooldown y máximo de reenvíos.
     */
    @Transactional
    public ResendTwoFactorResponse resendTwoFactor(ResendTwoFactorRequest request, String ip) {

        LoginTwoFactorChallenge challenge = challengeRepository
                .findById(request.getChallengeId())
                .orElseThrow(() -> new InvalidOrExpiredTwoFactorCodeException(
                        "Desafío no encontrado o ya procesado."));

        validateChallengeForResend(challenge);

        // Nuevo OTP invalida el anterior; attemptCount se resetea porque el OTP cambió
        String newOtp     = generateOtp();
        LocalDateTime now = LocalDateTime.now();

        challenge.setOtpHash(passwordEncoder.encode(newOtp));
        challenge.setLastSentAt(now);
        challenge.setResendCount(challenge.getResendCount() + 1);
        challenge.setAttemptCount(0); // Reset: OTP anterior ya es inválido
        // expiresAt NO se extiende: evita que reenvíos mantengan el desafío vivo indefinidamente
        challengeRepository.save(challenge);

        Usuario usuario = usuarioRepository.findById(challenge.getUsuarioId())
                .orElseThrow(() -> new InvalidOrExpiredTwoFactorCodeException(
                        "Desafío no encontrado o ya procesado."));

        int remainingSeconds = (int) Math.max(0,
                Duration.between(now, challenge.getExpiresAt()).toSeconds());

        emailService.sendOtpLoginEmail(
                usuario.getEmail(),
                usuario.getPrimerNombre(),
                newOtp,
                (int) Math.max(1, Duration.between(now, challenge.getExpiresAt()).toMinutes()));

        auditLogService.registrarExito(
                usuario.getId(), usuario.getEmail(),
                AuditCategoria.AUTENTICACION, AuditAccion.LOGIN_2FA_OTP_REENVIADO,
                "LoginTwoFactorChallenge", null, ip);

        return new ResendTwoFactorResponse(
                "Código de verificación reenviado a su correo electrónico.",
                remainingSeconds);
    }

    // =========================================================================
    // REGISTRO
    // =========================================================================

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Intento de registro para: {}", LogMaskUtil.maskEmail(request.getEmail()));

        if (usuarioRepository.existsByEmailAndIsActive(request.getEmail(), true)) {
            throw new EmailAlreadyExistsException(
                    "El email " + request.getEmail() + " ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .primerNombre(request.getPrimerNombre())
                .primerApellido(request.getPrimerApellido())
                .tipoDocumento(TipoDocumento.fromString(request.getTipoDocumento()))
                .documento(request.getDocumento())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(request.getTelefono())
                .role(Rol.CLIENTE)
                .isActive(true)
                .habeasDataConsented(request.getHabeasDataConsent())
                .habeasDataConsentedAt(Instant.now())
                .build();

        usuario = usuarioRepository.save(usuario);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", usuario.getId());
        extraClaims.put("role",   usuario.getRole());
        String jwtToken = jwtService.generateToken(usuario, extraClaims);

        log.info("Registro exitoso para: {}", LogMaskUtil.maskEmail(request.getEmail()));
        auditLogService.registrarExito(
                usuario.getId(), usuario.getEmail(),
                AuditCategoria.AUTENTICACION, AuditAccion.REGISTRO_EXITOSO,
                "USUARIO", usuario.getId(), HttpRequestUtil.obtenerIpCliente());

        return AuthResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .primerNombre(usuario.getPrimerNombre())
                .primerApellido(usuario.getPrimerApellido())
                .role(usuario.getRole().name())
                .build();
    }

    // =========================================================================
    // PERFIL Y CUENTA
    // =========================================================================

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = usuarioRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        return UserProfileResponse.builder()
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .primerNombre(usuario.getPrimerNombre())
                .primerApellido(usuario.getPrimerApellido())
                .tipoDocumento(usuario.getTipoDocumento().name())
                .documento(usuario.getDocumento())
                .telefono(usuario.getTelefono())
                .role(usuario.getRole().name())
                .isActive(usuario.getIsActive())
                .habeasDataConsented(usuario.getHabeasDataConsented())
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        log.info("Solicitud de cambio de contraseña para usuario autenticado");

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = usuarioRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isEmpty()) {
            throw new InvalidCredentialsException(
                    "Los usuarios registrados con Google no pueden cambiar la contraseña desde aquí");
        }

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        if (!request.getPasswordNueva().equals(request.getConfirmarPassword())) {
            throw new InvalidCredentialsException(
                    "La nueva contraseña y la confirmación no coinciden");
        }

        if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException(
                    "La nueva contraseña debe ser diferente a la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuario.setPasswordChangedAt(Instant.now());
        usuarioRepository.save(usuario);

        log.info("Cambio de contraseña completado");
        auditLogService.registrarExito(
                usuario.getId(), usuario.getEmail(),
                AuditCategoria.CUENTA, AuditAccion.CAMBIO_PASSWORD,
                null, null, HttpRequestUtil.obtenerIpCliente());
    }

    @Transactional
    public void deleteAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Solicitud de baja de cuenta para: {}", LogMaskUtil.maskEmail(email));

        Usuario usuario = usuarioRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        usuario.setIsActive(false);
        usuarioRepository.save(usuario);

        log.info("Baja de cuenta completada para usuario id={}", usuario.getId());
        auditLogService.registrarExito(
                usuario.getId(), usuario.getEmail(),
                AuditCategoria.CUENTA, AuditAccion.BAJA_CUENTA,
                "USUARIO", usuario.getId(), HttpRequestUtil.obtenerIpCliente());
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /**
     * Valida que el challenge esté en estado correcto para ser verificado.
     * Aplica expiración lazy: si el TTL venció pero el status sigue PENDING,
     * lo actualiza a EXPIRED en la misma transacción antes de lanzar la excepción.
     */
    private void validateChallengeForVerification(LoginTwoFactorChallenge challenge, String ip) {
        switch (challenge.getStatus()) {
            case VERIFIED -> throw new InvalidOrExpiredTwoFactorCodeException(
                    "Este código ya fue utilizado.");

            case BLOCKED -> {
                auditLogService.registrarFallo(
                        challenge.getUsuarioId(), null,
                        AuditCategoria.AUTENTICACION,
                        AuditAccion.LOGIN_2FA_CHALLENGE_BLOQUEADO,
                        "Intento sobre desafío bloqueado", ip);
                throw new TwoFactorChallengeBlockedException(
                        "El desafío ha sido bloqueado por múltiples intentos fallidos.");
            }

            case EXPIRED -> {
                auditLogService.registrarFallo(
                        challenge.getUsuarioId(), null,
                        AuditCategoria.AUTENTICACION,
                        AuditAccion.LOGIN_2FA_CHALLENGE_EXPIRADO,
                        "Intento sobre desafío expirado", ip);
                throw new InvalidOrExpiredTwoFactorCodeException(
                        "El código ha expirado. Inicie sesión nuevamente.");
            }

            case PENDING -> {
                // Expiración lazy: el status puede seguir PENDING aunque el TTL haya vencido
                if (LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
                    challenge.setStatus(TwoFactorChallengeStatus.EXPIRED);
                    challengeRepository.save(challenge);
                    auditLogService.registrarFallo(
                            challenge.getUsuarioId(), null,
                            AuditCategoria.AUTENTICACION,
                            AuditAccion.LOGIN_2FA_CHALLENGE_EXPIRADO,
                            "TTL vencido en verificación", ip);
                    throw new InvalidOrExpiredTwoFactorCodeException(
                            "El código ha expirado. Inicie sesión nuevamente.");
                }
            }
        }
    }

    private void validateChallengeForResend(LoginTwoFactorChallenge challenge) {
        if (challenge.getStatus() != TwoFactorChallengeStatus.PENDING) {
            throw new InvalidOrExpiredTwoFactorCodeException("El desafío no está activo.");
        }
        if (LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
            challenge.setStatus(TwoFactorChallengeStatus.EXPIRED);
            challengeRepository.save(challenge);
            throw new InvalidOrExpiredTwoFactorCodeException(
                    "El desafío ha expirado. Inicie sesión nuevamente.");
        }
        if (challenge.getResendCount() >= challenge.getMaxResends()) {
            throw new TwoFactorResendNotAllowedException(
                    "Ha alcanzado el límite de reenvíos permitidos.");
        }
        if (challenge.getLastSentAt() != null) {
            LocalDateTime allowedAt = challenge.getLastSentAt().plusSeconds(resendCooldownSeconds);
            if (LocalDateTime.now().isBefore(allowedAt)) {
                long wait = Duration.between(LocalDateTime.now(), allowedAt).toSeconds();
                throw new TwoFactorResendNotAllowedException(
                        "Debe esperar " + wait + " segundo(s) antes de solicitar un nuevo código.");
            }
        }
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        return String.valueOf(min + SECURE_RANDOM.nextInt(max - min + 1));
    }

    /** Carga usuario para auditoría sin propagar excepción si no existe. */
    private Optional<Usuario> loadUserForAudit(Long usuarioId) {
        return usuarioRepository.findById(usuarioId);
    }
}