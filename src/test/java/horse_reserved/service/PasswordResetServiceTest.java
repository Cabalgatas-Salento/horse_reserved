package horse_reserved.service;

import horse_reserved.exception.InvalidTokenException;
import horse_reserved.model.PasswordResetToken;
import horse_reserved.model.Rol;
import horse_reserved.model.Usuario;
import horse_reserved.repository.PasswordResetTokenRepository;
import horse_reserved.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @Mock AuditLogService auditLogService;

    @InjectMocks PasswordResetService passwordResetService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L).email("juan@test.com")
                .passwordHash("$2a$12$hashed")
                .role(Rol.CLIENTE)
                .isActive(true)
                .passwordChangedAt(Instant.EPOCH)
                .build();
    }

    // ── processForgotPassword ─────────────────────────────────────────────────

    @Test
    void processForgotPassword_emailNoExiste_retornaSilenciosamente() {
        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThatCode(() -> passwordResetService.processForgotPassword("noexiste@test.com"))
                .doesNotThrowAnyException();
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void processForgotPassword_usuarioOAuth2SinHash_retornaSilenciosamente() {
        usuario.setPasswordHash(null);
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.of(usuario));

        assertThatCode(() -> passwordResetService.processForgotPassword(usuario.getEmail()))
                .doesNotThrowAnyException();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void processForgotPassword_emailValido_guardaToken() {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        doNothing().when(tokenRepository).invalidatePreviousTokens(usuario);
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(any(), any(), any());
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        passwordResetService.processForgotPassword(usuario.getEmail());

        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void processForgotPassword_emailValido_invalidaTokenPrevio() {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        doNothing().when(tokenRepository).invalidatePreviousTokens(usuario);
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(any(), any(), any());
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        passwordResetService.processForgotPassword(usuario.getEmail());

        verify(tokenRepository).invalidatePreviousTokens(usuario);
    }

    @Test
    void processForgotPassword_emailValido_enviaEmail() {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        doNothing().when(tokenRepository).invalidatePreviousTokens(usuario);
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(any(), any(), any());
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        passwordResetService.processForgotPassword(usuario.getEmail());

        verify(emailService).sendPasswordResetEmail(eq(usuario.getEmail()), eq(usuario.getPrimerNombre()), anyString());
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_tokenValido_actualizaPassword() {
        PasswordResetToken token = tokenValido();
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaClave$123")).thenReturn("$2a$12$newHash");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(tokenRepository.save(any())).thenReturn(token);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        assertThatCode(() -> passwordResetService.resetPassword("valid-token", "NuevaClave$123"))
                .doesNotThrowAnyException();
        assertThat(token.getUsed()).isTrue();
    }

    @Test
    void resetPassword_tokenNoExiste_lanzaInvalidToken() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bad-token", "cualquier"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_tokenExpirado_lanzaInvalidToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("expired-token")
                .usuario(usuario)
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .used(false)
                .build();
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "cualquier"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_tokenYaUsado_lanzaInvalidToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("used-token")
                .usuario(usuario)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(true)
                .build();
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "cualquier"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PasswordResetToken tokenValido() {
        return PasswordResetToken.builder()
                .token("valid-token")
                .usuario(usuario)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
    }
}
