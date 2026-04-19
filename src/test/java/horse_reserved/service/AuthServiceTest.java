package horse_reserved.service;

import horse_reserved.dto.request.ChangePasswordRequest;
import horse_reserved.dto.request.LoginRequest;
import horse_reserved.dto.request.RegisterRequest;
import horse_reserved.dto.response.AuthResponse;
import horse_reserved.dto.response.UserProfileResponse;
import horse_reserved.exception.EmailAlreadyExistsException;
import horse_reserved.exception.InvalidCredentialsException;
import horse_reserved.exception.UserInactiveException;
import horse_reserved.model.Rol;
import horse_reserved.model.TipoDocumento;
import horse_reserved.model.Usuario;
import horse_reserved.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock AuditLogService auditLogService;

    @InjectMocks AuthService authService;

    private Usuario usuarioCliente;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 3_600_000L);

        usuarioCliente = Usuario.builder()
                .id(1L)
                .primerNombre("Juan")
                .primerApellido("Pérez")
                .email("juan@test.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(Rol.CLIENTE)
                .isActive(true)
                .tipoDocumento(TipoDocumento.CEDULA)
                .documento("123456789")
                .passwordChangedAt(Instant.EPOCH)
                .build();

        // Poner autenticación en contexto para los métodos que la necesitan
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan@test.com", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_emailNuevo_retornaAuthResponse() {
        RegisterRequest request = registerRequest();
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(usuarioRepository.save(any())).thenReturn(usuarioCliente);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt.token.here");
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        AuthResponse resp = authService.register(request);

        assertThat(resp.getToken()).isEqualTo("jwt.token.here");
        assertThat(resp.getEmail()).isEqualTo(usuarioCliente.getEmail());
    }

    @Test
    void register_emailDuplicado_lanzaEmailAlreadyExists() {
        RegisterRequest request = registerRequest();
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void register_usuarioCreado_rolEsCliente() {
        RegisterRequest request = registerRequest();
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenReturn(usuarioCliente);
        when(jwtService.generateToken(any(), any())).thenReturn("token");
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        authService.register(request);

        assertThat(captor.getValue().getRole()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    void register_passwordHasheado() {
        RegisterRequest request = registerRequest();
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Clave$ecreta123!")).thenReturn("$2a$12$hashed");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenReturn(usuarioCliente);
        when(jwtService.generateToken(any(), any())).thenReturn("token");
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        authService.register(request);

        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$12$hashed");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_credencialesValidas_retornaAuthResponseConToken() {
        LoginRequest request = loginRequest();
        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuarioCliente));
        when(jwtService.generateToken(any(), any())).thenReturn("jwt.token.here");
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        AuthResponse resp = authService.login(request);

        assertThat(resp.getToken()).isEqualTo("jwt.token.here");
        assertThat(resp.getRole()).isEqualTo("CLIENTE");
    }

    @Test
    void login_credencialesInvalidas_lanzaInvalidCredentials() {
        LoginRequest request = loginRequest();
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());
        doNothing().when(auditLogService).registrarFallo(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_usuarioInactivo_lanzaUserInactive() {
        LoginRequest request = loginRequest();
        usuarioCliente.setIsActive(false);
        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuarioCliente));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserInactiveException.class);
    }

    // ── getCurrentUser ────────────────────────────────────────────────────────

    @Test
    void getCurrentUser_usuarioAutenticado_retornaPerfil() {
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioCliente));

        UserProfileResponse profile = authService.getCurrentUser();

        assertThat(profile.getEmail()).isEqualTo("juan@test.com");
        assertThat(profile.getRole()).isEqualTo("CLIENTE");
    }

    @Test
    void getCurrentUser_emailNoExiste_lanzaInvalidCredentials() {
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    void changePassword_passwordActualCorrecta_actualizaHash() {
        ChangePasswordRequest request = new ChangePasswordRequest("actual123", "Nueva$ecreta123!", "Nueva$ecreta123!");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.matches("actual123", usuarioCliente.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("Nueva$ecreta123!", usuarioCliente.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("Nueva$ecreta123!")).thenReturn("$2a$12$newHash");
        when(usuarioRepository.save(any())).thenReturn(usuarioCliente);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        assertThatCode(() -> authService.changePassword(request)).doesNotThrowAnyException();
        verify(usuarioRepository).save(argThat(u -> u.getPasswordHash().equals("$2a$12$newHash")));
    }

    @Test
    void changePassword_passwordActualIncorrecta_lanzaExcepcion() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "Nueva$ecreta123!", "Nueva$ecreta123!");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.matches("wrongPass", usuarioCliente.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePassword_confirmacionNoCoincide_lanzaExcepcion() {
        ChangePasswordRequest request = new ChangePasswordRequest("actual123", "Nueva$ecreta123!", "Distinta$123!");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.matches("actual123", usuarioCliente.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePassword_nuevaIgualActual_lanzaExcepcion() {
        ChangePasswordRequest request = new ChangePasswordRequest("Clave$ecreta123!", "Clave$ecreta123!", "Clave$ecreta123!");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioCliente));
        when(passwordEncoder.matches("Clave$ecreta123!", usuarioCliente.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePassword_usuarioOAuth2SinHash_lanzaExcepcion() {
        usuarioCliente.setPasswordHash(null);
        ChangePasswordRequest request = new ChangePasswordRequest("cualquiera", "Nueva$ecreta123!", "Nueva$ecreta123!");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioCliente));

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .primerNombre("Juan")
                .primerApellido("Pérez")
                .tipoDocumento("cedula")
                .documento("123456789")
                .email("juan@test.com")
                .password("Clave$ecreta123!")
                .recaptchaToken("token-recaptcha")
                .habeasDataConsent(true)
                .build();
    }

    private LoginRequest loginRequest() {
        return LoginRequest.builder()
                .email("juan@test.com")
                .password("Clave$ecreta123!")
                .recaptchaToken("token-recaptcha")
                .build();
    }
}
