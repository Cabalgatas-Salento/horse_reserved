package horse_reserved.service;

import horse_reserved.exception.InvalidTokenException;
import horse_reserved.model.PasswordResetToken;
import horse_reserved.model.Usuario;
import horse_reserved.repository.PasswordResetTokenRepository;
import horse_reserved.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquesta el flujo completo de recuperación de contraseña:
 * generación de token, envío de correo y validación/actualización.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Procesa la solicitud de restablecimiento de contraseña.
     * Siempre retorna sin excepción para no revelar si el email existe.
     *
     * @param email Dirección de correo enviada por el cliente
     */
    @Transactional
    public void processForgotPassword(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        // Salida silenciosa si el email no existe
        if (usuarioOpt.isEmpty()) {
            log.debug("Solicitud de restablecimiento para email no registrado");
            return;
        }

        Usuario usuario = usuarioOpt.get();

        // Salida silenciosa si es un usuario OAuth2 (sin contraseña local)
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isEmpty()) {
            log.debug("Solicitud de restablecimiento para usuario OAuth2, id={}", usuario.getId());
            return;
        }

        // Invalidar tokens previos no usados para este usuario
        tokenRepository.invalidatePreviousTokens(usuario);

        // Generar nuevo token
        String tokenValue = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(tokenValue)
                .usuario(usuario)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .build();

        tokenRepository.save(resetToken);

        // Enviar correo (asíncrono — no bloquea ni lanza excepción al llamador)
        emailService.sendPasswordResetEmail(
                usuario.getEmail(),
                usuario.getPrimerNombre(),
                tokenValue
        );

        log.info("Token de restablecimiento generado para usuario id={}", usuario.getId());
    }

    /**
     * Valida el token y actualiza la contraseña del usuario.
     *
     * @param token         UUID del token recibido desde el frontend
     * @param nuevaPassword Nueva contraseña en texto plano
     * @throws InvalidTokenException si el token no existe, ha expirado o ya fue usado
     */
    @Transactional
    public void resetPassword(String token, String nuevaPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "El enlace de restablecimiento no es válido o ha expirado"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException(
                    "El enlace de restablecimiento no es válido o ha expirado");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setPasswordChangedAt(Instant.now());
        usuarioRepository.save(usuario);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Contraseña restablecida exitosamente para usuario id={}", usuario.getId());
    }
}
