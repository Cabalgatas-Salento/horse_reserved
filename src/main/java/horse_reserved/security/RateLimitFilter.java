package horse_reserved.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Configuración por endpoint: [capacity, refillTokens, windowSeconds]
    private static final Map<String, long[]> RATE_LIMITS = Map.of(
            "/api/auth/login",           new long[]{5, 5, 60},    // 5 intentos / minuto
            "/api/auth/register",        new long[]{3, 3, 600},   // 3 intentos / 10 minutos
            "/api/auth/forgot-password", new long[]{3, 3, 600},   // 3 intentos / 10 minutos
            "/api/auth/reset-password",  new long[]{5, 5, 600}    // 5 intentos / 10 minutos
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        long[] config = RATE_LIMITS.get(path);

        if (config == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);
        String bucketKey = ip + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> buildBucket(config));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(config[2]));
            response.getWriter().write(buildErrorJson(path));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket buildBucket(long[] config) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(config[0])
                .refillIntervally(config[1], Duration.ofSeconds(config[2]))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String buildErrorJson(String path) {
        return String.format(
                "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"Demasiadas solicitudes. Por favor, intenta de nuevo más tarde.\",\"path\":\"%s\"}",
                LocalDateTime.now(), path
        );
    }
}
