package horse_reserved.service;

import horse_reserved.dto.request.LoginRequest;
import horse_reserved.dto.request.RegisterRequest;
import horse_reserved.dto.response.AuthResponse;
import horse_reserved.exception.EmailAlreadyExistsException;
import horse_reserved.exception.InvalidCredentialsException;
import horse_reserved.exception.UserInactiveException;
import horse_reserved.model.Rol;
import horse_reserved.model.TipoDocumento;
import horse_reserved.model.Usuario;
import horse_reserved.repository.UsuarioRepository;
import horse_reserved.dto.response.UserProfileResponse;
import horse_reserved.dto.request.ChangePasswordRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registra un nuevo cliente en el sistema
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Verificar si el email ya existe
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("El email " + request.getEmail() + " ya está registrado");
        }

        // Crear nuevo usuario
        Usuario usuario = Usuario.builder()
                .primerNombre(request.getPrimerNombre())
                .primerApellido(request.getPrimerApellido())
                .tipoDocumento(TipoDocumento.fromString(request.getTipoDocumento()))
                .documento(request.getDocumento())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(request.getTelefono())
                .role(Rol.CLIENTE) // Por defecto todos los registros son clientes
                .isActive(true)
                .habeasDataConsented(request.getHabeasDataConsent())
                .habeasDataConsentedAt(Instant.now())
                .build();

        // Guardar usuario
        usuario = usuarioRepository.save(usuario);

        // Generar token JWT
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", usuario.getId());
        extraClaims.put("role", usuario.getRole());

        String jwtToken = jwtService.generateToken(usuario, extraClaims);

        // Retornar respuesta
        return AuthResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .expiresIn(86400L) // 24 horas
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .primerNombre(usuario.getPrimerNombre())
                .primerApellido(usuario.getPrimerApellido())
                .role(usuario.getRole().name())
                .build();
    }

    /**
     * Autentica un usuario y genera un token JWT
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Intentar autenticar
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }

        // Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        // Verificar si el usuario está activo
        if (!usuario.getIsActive()) {
            throw new UserInactiveException("El usuario está inactivo. Contacte al administrador.");
        }

        // Generar token JWT con claims adicionales
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", usuario.getId());
        extraClaims.put("role", usuario.getRole());

        String jwtToken = jwtService.generateToken(usuario, extraClaims);

        // Retornar respuesta
        return AuthResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .expiresIn(86400L)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .primerNombre(usuario.getPrimerNombre())
                .primerApellido(usuario.getPrimerApellido())
                .role(usuario.getRole().name())
                .build();
    }

    /**
     * Retorna el perfil del usuario actualmente autenticado
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        return UserProfileResponse.builder()
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .primerNombre(usuario.getPrimerNombre())
                .primerApellido(usuario.getPrimerApellido())
                .tipoDocumento(usuario.getTipoDocumento().name())
                .documento(usuario.getDocumento())
                .telefono(usuario.getTelefono())
                .role(usuario.getRole().name())
                .isActive(usuario.getIsActive())
                .habeasDataConsented(usuario.getHabeasDataConsented())
                .build();
    }

    /**
     * Cambia la contraseña del usuario autenticado
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        // Validación para usuarios registrados con Google OAuth2
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isEmpty()) {
            throw new InvalidCredentialsException(
                    "Los usuarios registrados con Google no pueden cambiar la contraseña desde aquí"
            );
        }

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        if (!request.getPasswordNueva().equals(request.getConfirmarPassword())) {
            throw new InvalidCredentialsException("La nueva contraseña y la confirmación no coinciden");
        }

        if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException("La nueva contraseña debe ser diferente a la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuario.setPasswordChangedAt(Instant.now());
        usuarioRepository.save(usuario);
    }
}