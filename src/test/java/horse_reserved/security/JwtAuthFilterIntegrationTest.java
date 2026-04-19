package horse_reserved.security;

import horse_reserved.model.Rol;
import horse_reserved.model.Usuario;
import horse_reserved.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterIntegrationTest {

    private static final String SECRET = "testsecretkey1234567890testsecretkey1234567890abc123";
    private static final long EXPIRATION = 3_600_000L;

    @Mock UserDetailsService userDetailsService;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        usuario = Usuario.builder()
                .id(1L)
                .email("usuario@test.com")
                .role(Rol.CLIENTE)
                .isActive(true)
                .passwordChangedAt(Instant.EPOCH)
                .build();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinToken_continuaFiltroSinAutenticar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void tokenValido_estableceAutenticacionEnContext() throws Exception {
        String token = jwtService.generateToken(usuario);
        when(userDetailsService.loadUserByUsername("usuario@test.com")).thenReturn(usuario);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("usuario@test.com");
    }

    @Test
    void tokenExpirado_continuaFiltroSinAutenticar() throws Exception {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String tokenExpirado = jwtService.generateToken(usuario);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenExpirado);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void tokenMalformado_continuaFiltroSinAutenticar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer esto.no.es.un.jwt.valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void bearerPrefixIncorrecto_continuaFiltroSinAutenticar() throws Exception {
        String token = jwtService.generateToken(usuario);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
