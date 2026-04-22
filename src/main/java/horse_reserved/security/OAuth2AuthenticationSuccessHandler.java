package horse_reserved.security;

import horse_reserved.dto.response.AuthResponse;
import horse_reserved.model.Usuario;
import horse_reserved.repository.UsuarioRepository;
import horse_reserved.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final OAuth2TokenStore tokenStore;

    @Value("${app.oauth2.authorized-redirect-uris:http://localhost:4200/auth/oauth2-redirect}")
    private String redirectUri;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + redirectUri);
            return;
        }

        String targetUrl = determineTargetUrl(request, response, authentication);
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Usuario usuario = usuarioRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", usuario.getId());
        extraClaims.put("role", usuario.getRole());

        String token = jwtService.generateToken(usuario, extraClaims);

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .primerNombre(usuario.getPrimerNombre())
                .primerApellido(usuario.getPrimerApellido())
                .role(usuario.getRole().name())
                .build();

        String code = tokenStore.store(authResponse);

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code)
                .build()
                .toUriString();
    }
}
