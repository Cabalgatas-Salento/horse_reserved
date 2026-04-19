package horse_reserved.security;

import horse_reserved.service.JwtService;
import horse_reserved.util.LogMaskUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Obtener el header Authorization
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Si no hay header o no empieza con "Bearer ", continuar con la cadena de filtros
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Request sin Bearer token — path={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token JWT del header
        jwt = authHeader.substring(7);

        try {
            // Extraer el email del token
            userEmail = jwtService.extractUsername(jwt);
            log.debug("Token procesado para: {}", LogMaskUtil.maskEmail(userEmail));

            // Si el email no es nulo y no hay autenticación en el contexto
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Cargar los detalles del usuario
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Validar el token
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Crear el token de autenticación
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Establecer detalles adicionales
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Establecer la autenticación en el contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Autenticación establecida para: {}", LogMaskUtil.maskEmail(userEmail));
                }
            }
        } catch (Exception e) {
            log.warn("Token JWT inválido o expirado — ref={}, causa={}", LogMaskUtil.maskToken(jwt), e.getMessage());
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}