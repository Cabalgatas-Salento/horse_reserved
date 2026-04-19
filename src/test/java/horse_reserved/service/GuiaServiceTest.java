package horse_reserved.service;

import horse_reserved.dto.request.GuiaRequest;
import horse_reserved.dto.response.GuiaResponse;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.model.Guia;
import horse_reserved.repository.GuiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuiaServiceTest {

    @Mock GuiaRepository guiaRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks GuiaService guiaService;

    private Guia guia;

    @BeforeEach
    void setUp() {
        guia = Guia.builder().id(1L).nombre("Carlos").telefono("3001234567")
                .email("carlos@guia.com").activo(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.com", null));
    }

    // ── listar ────────────────────────────────────────────────────────────────

    @Test
    void listar_sinFiltro_retornaTodos() {
        when(guiaRepository.findAll()).thenReturn(List.of(guia));

        List<GuiaResponse> result = guiaService.listar(null);
        assertThat(result).hasSize(1);
    }

    @Test
    void listar_soloActivos_filtraCorrectamente() {
        Guia inactivo = Guia.builder().id(2L).nombre("Pedro").telefono("3009876543")
                .email("pedro@guia.com").activo(false).build();
        when(guiaRepository.findAll()).thenReturn(List.of(guia, inactivo));

        List<GuiaResponse> result = guiaService.listar(true);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Carlos");
    }

    @Test
    void listar_soloInactivos_filtraCorrectamente() {
        Guia inactivo = Guia.builder().id(2L).nombre("Pedro").telefono("3009876543")
                .email("pedro@guia.com").activo(false).build();
        when(guiaRepository.findAll()).thenReturn(List.of(guia, inactivo));

        List<GuiaResponse> result = guiaService.listar(false);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Pedro");
    }

    // ── obtener ───────────────────────────────────────────────────────────────

    @Test
    void obtener_idExistente_retornaResponse() {
        when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia));

        GuiaResponse resp = guiaService.obtener(1L);
        assertThat(resp.getNombre()).isEqualTo("Carlos");
        assertThat(resp.getEmail()).isEqualTo("carlos@guia.com");
    }

    @Test
    void obtener_idNoExistente_lanzaResourceNotFound() {
        when(guiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guiaService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── crear ─────────────────────────────────────────────────────────────────

    @Test
    void crear_datosValidos_guardaGuia() {
        GuiaRequest request = new GuiaRequest("Carlos", "3001234567", "Carlos@Guia.Com", true);
        when(guiaRepository.save(any())).thenReturn(guia);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        GuiaResponse resp = guiaService.crear(request);
        assertThat(resp.getNombre()).isEqualTo("Carlos");
    }

    @Test
    void crear_emailSeConvierteAMinusculas() {
        GuiaRequest request = new GuiaRequest("Carlos", "3001234567", "  CARLOS@GUIA.COM  ", true);
        ArgumentCaptor<Guia> captor = ArgumentCaptor.forClass(Guia.class);
        when(guiaRepository.save(captor.capture())).thenReturn(guia);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        guiaService.crear(request);
        assertThat(captor.getValue().getEmail()).isEqualTo("carlos@guia.com");
    }

    // ── actualizar ────────────────────────────────────────────────────────────

    @Test
    void actualizar_idExistente_actualizaDatos() {
        GuiaRequest request = new GuiaRequest("NuevoNombre", "3119876543", "nuevo@guia.com", false);
        Guia actualizado = Guia.builder().id(1L).nombre("NuevoNombre").telefono("3119876543")
                .email("nuevo@guia.com").activo(false).build();
        when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia));
        when(guiaRepository.save(any())).thenReturn(actualizado);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        GuiaResponse resp = guiaService.actualizar(1L, request);
        assertThat(resp.getNombre()).isEqualTo("NuevoNombre");
        assertThat(resp.isActivo()).isFalse();
    }

    @Test
    void actualizar_idNoExistente_lanzaResourceNotFound() {
        GuiaRequest request = new GuiaRequest("Nombre", "3001234567", "test@test.com", true);
        when(guiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guiaService.actualizar(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cambiarEstado ─────────────────────────────────────────────────────────

    @Test
    void cambiarEstado_desactiva() {
        Guia inactivo = Guia.builder().id(1L).nombre("Carlos").telefono("3001234567")
                .email("carlos@guia.com").activo(false).build();
        when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia));
        when(guiaRepository.save(any())).thenReturn(inactivo);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        GuiaResponse resp = guiaService.cambiarEstado(1L, false);
        assertThat(resp.isActivo()).isFalse();
    }

    @Test
    void cambiarEstado_activa() {
        guia.setActivo(false);
        Guia activo = Guia.builder().id(1L).nombre("Carlos").telefono("3001234567")
                .email("carlos@guia.com").activo(true).build();
        when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia));
        when(guiaRepository.save(any())).thenReturn(activo);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        GuiaResponse resp = guiaService.cambiarEstado(1L, true);
        assertThat(resp.isActivo()).isTrue();
    }

    @Test
    void cambiarEstado_idNoExistente_lanzaResourceNotFound() {
        when(guiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guiaService.cambiarEstado(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
