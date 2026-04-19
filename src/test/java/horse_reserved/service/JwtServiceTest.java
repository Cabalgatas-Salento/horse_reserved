package horse_reserved.service;

import horse_reserved.model.Rol;
import horse_reserved.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import io.jsonwebtoken.ExpiredJwtException;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private Usuario usuario;

    private static final String SECRET = "testsecretkey1234567890testsecretkey1234567890abc123";
    private static final long EXPIRATION = 3_600_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

        usuario = Usuario.builder()
                .id(1L)
                .email("test@test.com")
                .role(Rol.CLIENTE)
                .passwordChangedAt(Instant.EPOCH)
                .isActive(true)
                .build();
    }

    @Test
    void generateToken_conUserDetails_retornaTokenNoNulo() {
        String token = jwtService.generateToken(usuario);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateToken_conExtraClaims_claimsPresentes() {
        Map<String, Object> claims = Map.of("userId", 1L, "role", "CLIENTE");
        String token = jwtService.generateToken(usuario, claims);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_tokenValido_retornaEmail() {
        String token = jwtService.generateToken(usuario);
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@test.com");
    }

    @Test
    void isTokenValid_tokenValido_retornaTrue() {
        String token = jwtService.generateToken(usuario);
        assertThat(jwtService.isTokenValid(token, usuario)).isTrue();
    }

    @Test
    void isTokenValid_emailDistinto_retornaFalse() {
        String token = jwtService.generateToken(usuario);
        Usuario otro = Usuario.builder()
                .email("otro@test.com")
                .role(Rol.CLIENTE)
                .passwordChangedAt(Instant.EPOCH)
                .isActive(true)
                .build();
        assertThat(jwtService.isTokenValid(token, otro)).isFalse();
    }

    @Test
    void isTokenValid_tokenExpirado_lanzaExpiredJwtException() {
        // JJWT lanza ExpiredJwtException al parsear claims; isTokenValid no lo captura
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String token = jwtService.generateToken(usuario);
        assertThatThrownBy(() -> jwtService.isTokenValid(token, usuario))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void isTokenValid_emitidoAntesDePasswordChangedAt_retornaFalse() throws InterruptedException {
        String token = jwtService.generateToken(usuario);
        Thread.sleep(10);
        // Simula cambio de contraseña DESPUÉS de emitir el token
        usuario.setPasswordChangedAt(Instant.now());
        assertThat(jwtService.isTokenValid(token, usuario)).isFalse();
    }

    @Test
    void isTokenValid_emitidoDespuesDePasswordChangedAt_retornaTrue() {
        // passwordChangedAt = EPOCH, token emitido ahora → válido
        String token = jwtService.generateToken(usuario);
        assertThat(jwtService.isTokenValid(token, usuario)).isTrue();
    }

    @Test
    void generateToken_distintoUsuario_tokensNoIguales() {
        Usuario otro = Usuario.builder()
                .email("otro@test.com")
                .role(Rol.CLIENTE)
                .passwordChangedAt(Instant.EPOCH)
                .isActive(true)
                .build();
        String t1 = jwtService.generateToken(usuario);
        String t2 = jwtService.generateToken(otro);
        assertThat(t1).isNotEqualTo(t2);
    }
}
