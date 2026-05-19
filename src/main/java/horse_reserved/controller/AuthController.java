package horse_reserved.controller;

import horse_reserved.dto.request.ChangePasswordRequest;
import horse_reserved.dto.request.ForgotPasswordRequest;
import horse_reserved.dto.request.LoginRequest;
import horse_reserved.dto.request.OAuth2TokenRequest;
import horse_reserved.dto.request.RegisterRequest;
import horse_reserved.dto.request.ResetPasswordRequest;
import horse_reserved.dto.response.AuthResponse;
import horse_reserved.dto.response.UserProfileResponse;
import horse_reserved.security.OAuth2TokenStore;
import horse_reserved.service.AuthService;
import horse_reserved.service.PasswordResetService;
import horse_reserved.service.RecaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import horse_reserved.dto.request.ResendTwoFactorRequest;
import horse_reserved.dto.request.VerifyTwoFactorRequest;
import horse_reserved.dto.response.ResendTwoFactorResponse;
import horse_reserved.dto.response.TwoFactorChallengeResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final OAuth2TokenStore oauth2TokenStore;
    private final RecaptchaService recaptchaService;

    /**
     * Endpoint para registrar un nuevo cliente
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        recaptchaService.verify(request.getRecaptchaToken());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para autenticar un usuario
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<TwoFactorChallengeResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = resolveClientIp(httpRequest);
        TwoFactorChallengeResponse response = authService.loginStep1(request, ip);
        // 202 Accepted: la autenticación está en progreso, se requiere acción adicional
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * endpoint para la verificacion en 2 pasos
     * @param request
     * @param httpRequest
     * @return
     */
    @PostMapping("/login/verify-2fa")
    public ResponseEntity<AuthResponse> verifyTwoFactor(
            @Valid @RequestBody VerifyTwoFactorRequest request,
            HttpServletRequest httpRequest) {

        String ip = resolveClientIp(httpRequest);
        AuthResponse response = authService.verifyTwoFactor(request, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * endpoint para el reenvio del codigo de verificacion en 2 pasos
     * @param request
     * @param httpRequest
     * @return
     */
    @PostMapping("/login/resend-2fa")
    public ResponseEntity<ResendTwoFactorResponse> resendTwoFactor(
            @Valid @RequestBody ResendTwoFactorRequest request,
            HttpServletRequest httpRequest) {

        String ip = resolveClientIp(httpRequest);
        ResendTwoFactorResponse response = authService.resendTwoFactor(request, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * Extrae la IP real del cliente respetando proxies inversos.
     * Toma solo el primer valor de X-Forwarded-For para evitar spoofing.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Retorna el perfil del usuario autenticado
     * GET /api/auth/me
     * Requiere: Bearer token válido en el header Authorization
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        UserProfileResponse profile = authService.getCurrentUser();
        return ResponseEntity.ok(profile);
    }

    /**
     * Cambia la contraseña del usuario autenticado
     * PUT /api/auth/change-password
     * Requiere: Bearer token válido
     */
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok("Contraseña actualizada correctamente");
    }

    /**
     * Inicia el flujo de recuperación de contraseña.
     * Siempre responde 200 para no revelar si el email existe.
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        recaptchaService.verify(request.getRecaptchaToken());
        passwordResetService.processForgotPassword(request.getEmail());
        return ResponseEntity.ok("Si el correo está registrado, recibirás un enlace para restablecer tu contraseña");
    }

    /**
     * Valida el token y establece la nueva contraseña.
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNuevaPassword());
        return ResponseEntity.ok("Contraseña restablecida correctamente. Ya puedes iniciar sesión");
    }

    /**
     * Intercambia el código OAuth2 de un solo uso por el AuthResponse con JWT.
     * POST /api/auth/oauth2/token
     */
    @PostMapping("/oauth2/token")
    public ResponseEntity<AuthResponse> exchangeOAuth2Token(@Valid @RequestBody OAuth2TokenRequest request) {
        return oauth2TokenStore.consume(request.getCode())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    /**
     * Desactiva la cuenta del usuario autenticado (baja voluntaria).
     * El usuario puede volver a registrarse con el mismo email en cualquier momento.
     * DELETE /api/auth/delete-account
     * Requiere: Bearer token válido
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount() {
        authService.deleteAccount();
        return ResponseEntity.noContent().build();
    }
}
