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
import horse_reserved.model.AuditAccion;
import horse_reserved.model.AuditCategoria;
import horse_reserved.util.HttpRequestUtil;
import horse_reserved.util.LogMaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Registra un nuevo cliente en el sistema
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Intento de registro para: {}", LogMaskUtil.maskEmail(request.getEmail()));
        // Verificar si el email ya está en uso por una cuenta activa
        if (usuarioRepository.existsByEmailAndIsActive(request.getEmail(), true)) {
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

        log.info("Registro exitoso para: {}", LogMaskUtil.maskEmail(request.getEmail()));
        auditLogService.registrarExito(usuario.getId(), usuario.getEmail(),
                AuditCategoria.AUTENTICACION, AuditAccion.REGISTRO_EXITOSO,
                "USUARIO", usuario.getId(), HttpRequestUtil.obtenerIpCliente());

        // Retornar respuesta
        return AuthResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .expiresIn(jwtExpiration / 1000)
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
        log.info("Intento de login para: {}", LogMaskUtil.maskEmail(request.getEmail()));
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            log.warn("Login fallido para {}: {}", LogMaskUtil.maskEmail(request.getEmail()), e.getMessage());
            auditLogService.registrarFallo(null, request.getEmail(),
                    AuditCategoria.AUTENTICACION, AuditAccion.LOGIN_FALLIDO,
                    "Credenciales inválidas", HttpRequestUtil.obtenerIpCliente());
            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }

        // Buscar usuario activo (puede haber registros inactivos con el mismo email)
        Usuario usuario = usuarioRepository.findByEmailAndIsActive(request.getEmail(), true)
                .orElseThrow(() -> {
                    log.warn("Usuario activo no encontrado tras autenticación: {}", LogMaskUtil.maskEmail(request.getEmail()));
                    return new UserInactiveException("Esta cuenta ha sido dada de baja. Para volver a acceder, crea una nueva cuenta.");
                });

        // Generar token JWT con claims adicionales
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", usuario.getId());
        extraClaims.put("role", usuario.getRole());

        String jwtToken = jwtService.generateToken(usuario, extraClaims);

        log.info("Login exitoso para: {}", LogMaskUtil.maskEmail(request.getEmail()));
        auditLogService.registrarExito(usuario.getId(), usuario.getEmail(),
                AuditCategoria.AUTENTICACION, AuditAccion.LOGIN_EXITOSO,
                null, null, HttpRequestUtil.obtenerIpCliente());
        // Retornar respuesta
        return AuthResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .expiresIn(jwtExpiration / 1000)
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

        Usuario usuario = usuarioRepository.findByEmailAndIsActive(email, true)
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
        log.info("Solicitud de cambio de contraseña para usuario autenticado");

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Usuario usuario = usuarioRepository.findByEmailAndIsActive(email, true)
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
        log.info("Cambio de contraseña completado");
        auditLogService.registrarExito(usuario.getId(), usuario.getEmail(),
                AuditCategoria.CUENTA, AuditAccion.CAMBIO_PASSWORD,
                null, null, HttpRequestUtil.obtenerIpCliente());
    }

    /**
     * Desactiva la cuenta del usuario autenticado (baja voluntaria).
     * El usuario no podrá iniciar sesión con esta cuenta; podrá registrarse
     * nuevamente con el mismo email para obtener una cuenta completamente nueva.
     */
    @Transactional
    public void deleteAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Solicitud de baja de cuenta para: {}", LogMaskUtil.maskEmail(email));

        Usuario usuario = usuarioRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        usuario.setIsActive(false);
        usuarioRepository.save(usuario);

        log.info("Baja de cuenta completada para usuario id={}", usuario.getId());
        auditLogService.registrarExito(usuario.getId(), usuario.getEmail(),
                AuditCategoria.CUENTA, AuditAccion.BAJA_CUENTA,
                "USUARIO", usuario.getId(), HttpRequestUtil.obtenerIpCliente());
    }
}