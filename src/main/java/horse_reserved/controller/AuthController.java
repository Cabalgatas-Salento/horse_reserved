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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        recaptchaService.verify(request.getRecaptchaToken());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
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
}
