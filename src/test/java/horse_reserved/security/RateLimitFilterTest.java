package horse_reserved.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    // =========================================================
    // POST /api/auth/login — límite: 5 intentos / minuto
    // =========================================================

    @Test
    void login_allowsFirstFiveRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            var response = doRequest("/api/auth/login");
            assertThat(response.getStatus())
                    .as("Intento %d debería pasar", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    void login_blocksOnSixthRequest() throws Exception {
        exhaust("/api/auth/login", 5);
        var response = doRequest("/api/auth/login");
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void login_retryAfterHeaderIs60Seconds() throws Exception {
        exhaust("/api/auth/login", 5);
        var response = doRequest("/api/auth/login");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    // =========================================================
    // POST /api/auth/register — límite: 3 intentos / 10 minutos
    // =========================================================

    @Test
    void register_allowsFirstThreeRequests() throws Exception {
        for (int i = 0; i < 3; i++) {
            var response = doRequest("/api/auth/register");
            assertThat(response.getStatus())
                    .as("Intento %d debería pasar", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    void register_blocksOnFourthRequest() throws Exception {
        exhaust("/api/auth/register", 3);
        var response = doRequest("/api/auth/register");
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void register_retryAfterHeaderIs600Seconds() throws Exception {
        exhaust("/api/auth/register", 3);
        var response = doRequest("/api/auth/register");
        assertThat(response.getHeader("Retry-After")).isEqualTo("600");
    }

    // =========================================================
    // POST /api/auth/forgot-password — límite: 3 intentos / 10 minutos
    // =========================================================

    @Test
    void forgotPassword_allowsFirstThreeRequests() throws Exception {
        for (int i = 0; i < 3; i++) {
            var response = doRequest("/api/auth/forgot-password");
            assertThat(response.getStatus())
                    .as("Intento %d debería pasar", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    void forgotPassword_blocksOnFourthRequest() throws Exception {
        exhaust("/api/auth/forgot-password", 3);
        var response = doRequest("/api/auth/forgot-password");
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void forgotPassword_retryAfterHeaderIs600Seconds() throws Exception {
        exhaust("/api/auth/forgot-password", 3);
        var response = doRequest("/api/auth/forgot-password");
        assertThat(response.getHeader("Retry-After")).isEqualTo("600");
    }

    // =========================================================
    // POST /api/auth/reset-password — límite: 5 intentos / 10 minutos
    // =========================================================

    @Test
    void resetPassword_allowsFirstFiveRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            var response = doRequest("/api/auth/reset-password");
            assertThat(response.getStatus())
                    .as("Intento %d debería pasar", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    void resetPassword_blocksOnSixthRequest() throws Exception {
        exhaust("/api/auth/reset-password", 5);
        var response = doRequest("/api/auth/reset-password");
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void resetPassword_retryAfterHeaderIs600Seconds() throws Exception {
        exhaust("/api/auth/reset-password", 5);
        var response = doRequest("/api/auth/reset-password");
        assertThat(response.getHeader("Retry-After")).isEqualTo("600");
    }

    // =========================================================
    // Endpoints NO limitados
    // =========================================================

    @Test
    void unlistedEndpoint_neverReturns429() throws Exception {
        for (int i = 0; i < 20; i++) {
            var response = doRequest("/api/auth/me");
            assertThat(response.getStatus())
                    .as("Intento %d en endpoint no limitado no debe bloquearse", i + 1)
                    .isNotEqualTo(429);
        }
    }

    // =========================================================
    // Respuesta 429 — formato
    // =========================================================

    @Test
    void blockedResponse_hasJsonContentType() throws Exception {
        exhaust("/api/auth/login", 5);
        var response = doRequest("/api/auth/login");
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    void blockedResponse_bodyContainsExpectedFields() throws Exception {
        exhaust("/api/auth/login", 5);
        var response = doRequest("/api/auth/login");
        String body = response.getContentAsString();
        assertThat(body)
                .contains("\"status\":429")
                .contains("\"error\":\"Too Many Requests\"")
                .contains("\"path\":\"/api/auth/login\"")
                .contains("\"timestamp\"")
                .contains("\"message\"");
    }

    // =========================================================
    // Aislamiento por IP
    // =========================================================

    @Test
    void rateLimitIsPerIp_differentIpsAreIndependent() throws Exception {
        exhaust("/api/auth/login", 5, "10.0.0.1");

        // Una IP distinta no debe verse afectada
        var response = doRequest("/api/auth/login", "10.0.0.2");
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void xForwardedFor_firstIpIsUsedAsClientIp() throws Exception {
        // Agotar el límite con una IP vía X-Forwarded-For
        for (int i = 0; i < 5; i++) {
            doRequestWithForwardedIp("/api/auth/login", "192.168.1.1, 10.0.0.1");
        }

        // La misma IP forwarded debe ser bloqueada
        var blocked = doRequestWithForwardedIp("/api/auth/login", "192.168.1.1, 10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);

        // Una IP forwarded distinta debe seguir pasando
        var allowed = doRequestWithForwardedIp("/api/auth/login", "192.168.1.2");
        assertThat(allowed.getStatus()).isNotEqualTo(429);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private MockHttpServletResponse doRequest(String path) throws ServletException, IOException {
        return doRequest(path, "127.0.0.1");
    }

    private MockHttpServletResponse doRequest(String path, String remoteAddr) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletResponse doRequestWithForwardedIp(String path, String forwardedFor)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("10.10.10.10"); // IP del proxy
        request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private void exhaust(String path, int times) throws ServletException, IOException {
        exhaust(path, times, "127.0.0.1");
    }

    private void exhaust(String path, int times, String remoteAddr) throws ServletException, IOException {
        for (int i = 0; i < times; i++) {
            doRequest(path, remoteAddr);
        }
    }
}
