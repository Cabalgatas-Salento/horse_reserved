package horse_reserved.service;

import horse_reserved.dto.request.CaballoRequest;
import horse_reserved.dto.response.CaballoResponse;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.model.Caballo;
import horse_reserved.repository.CaballoRepository;
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
class CaballoServiceTest {

    @Mock CaballoRepository caballoRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks CaballoService caballoService;

    private Caballo caballo;

    @BeforeEach
    void setUp() {
        caballo = Caballo.builder().id(1L).nombre("Tornado").raza("Andaluz").activo(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.com", null));
    }

    // ── listar ────────────────────────────────────────────────────────────────

    @Test
    void listar_sinFiltro_retornaTodos() {
        when(caballoRepository.findAll()).thenReturn(List.of(caballo));

        List<CaballoResponse> result = caballoService.listar(null);
        assertThat(result).hasSize(1);
    }

    @Test
    void listar_soloActivos_filtraCorrectamente() {
        Caballo inactivo = Caballo.builder().id(2L).nombre("Luna").raza("Pinto").activo(false).build();
        when(caballoRepository.findAll()).thenReturn(List.of(caballo, inactivo));

        List<CaballoResponse> result = caballoService.listar(true);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Tornado");
    }

    @Test
    void listar_soloInactivos_filtraCorrectamente() {
        Caballo inactivo = Caballo.builder().id(2L).nombre("Luna").raza("Pinto").activo(false).build();
        when(caballoRepository.findAll()).thenReturn(List.of(caballo, inactivo));

        List<CaballoResponse> result = caballoService.listar(false);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Luna");
    }

    // ── obtener ───────────────────────────────────────────────────────────────

    @Test
    void obtener_idExistente_retornaResponse() {
        when(caballoRepository.findById(1L)).thenReturn(Optional.of(caballo));

        CaballoResponse resp = caballoService.obtener(1L);
        assertThat(resp.getNombre()).isEqualTo("Tornado");
        assertThat(resp.getRaza()).isEqualTo("Andaluz");
    }

    @Test
    void obtener_idNoExistente_lanzaResourceNotFound() {
        when(caballoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caballoService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── crear ─────────────────────────────────────────────────────────────────

    @Test
    void crear_datosValidos_guardaCaballo() {
        CaballoRequest request = new CaballoRequest("  Tornado  ", "Andaluz", true);
        when(caballoRepository.save(any())).thenReturn(caballo);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        CaballoResponse resp = caballoService.crear(request);
        assertThat(resp.getNombre()).isEqualTo("Tornado");
    }

    @Test
    void crear_nombreConEspacios_seHaceTrim() {
        CaballoRequest request = new CaballoRequest("  Tornado  ", "  Andaluz  ", true);
        ArgumentCaptor<Caballo> captor = ArgumentCaptor.forClass(Caballo.class);
        when(caballoRepository.save(captor.capture())).thenReturn(caballo);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        caballoService.crear(request);
        assertThat(captor.getValue().getNombre()).isEqualTo("Tornado");
        assertThat(captor.getValue().getRaza()).isEqualTo("Andaluz");
    }

    // ── actualizar ────────────────────────────────────────────────────────────

    @Test
    void actualizar_idExistente_actualizaDatos() {
        CaballoRequest request = new CaballoRequest("Nuevonombre", "Pinto", false);
        Caballo actualizado = Caballo.builder().id(1L).nombre("Nuevonombre").raza("Pinto").activo(false).build();
        when(caballoRepository.findById(1L)).thenReturn(Optional.of(caballo));
        when(caballoRepository.save(any())).thenReturn(actualizado);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        CaballoResponse resp = caballoService.actualizar(1L, request);
        assertThat(resp.getNombre()).isEqualTo("Nuevonombre");
        assertThat(resp.isActivo()).isFalse();
    }

    @Test
    void actualizar_idNoExistente_lanzaResourceNotFound() {
        CaballoRequest request = new CaballoRequest("Nombre", "Raza", true);
        when(caballoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caballoService.actualizar(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cambiarEstado ─────────────────────────────────────────────────────────

    @Test
    void cambiarEstado_desactiva() {
        Caballo inactivo = Caballo.builder().id(1L).nombre("Tornado").raza("Andaluz").activo(false).build();
        when(caballoRepository.findById(1L)).thenReturn(Optional.of(caballo));
        when(caballoRepository.save(any())).thenReturn(inactivo);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        CaballoResponse resp = caballoService.cambiarEstado(1L, false);
        assertThat(resp.isActivo()).isFalse();
    }

    @Test
    void cambiarEstado_activa() {
        caballo.setActivo(false);
        Caballo activo = Caballo.builder().id(1L).nombre("Tornado").raza("Andaluz").activo(true).build();
        when(caballoRepository.findById(1L)).thenReturn(Optional.of(caballo));
        when(caballoRepository.save(any())).thenReturn(activo);
        doNothing().when(auditLogService).registrarExito(any(), any(), any(), any(), any(), any(), any());

        CaballoResponse resp = caballoService.cambiarEstado(1L, true);
        assertThat(resp.isActivo()).isTrue();
    }

    @Test
    void cambiarEstado_idNoExistente_lanzaResourceNotFound() {
        when(caballoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caballoService.cambiarEstado(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
