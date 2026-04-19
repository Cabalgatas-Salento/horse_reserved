package horse_reserved.controller;

import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.model.Dificultad;
import horse_reserved.model.Ruta;
import horse_reserved.repository.RutaRepository;
import horse_reserved.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RutaControllerTest {

    @Mock RutaRepository rutaRepository;
    @Mock AuditLogService auditLogService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RutaController controller = new RutaController(rutaRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .build();
    }

    // ── GET /api/rutas/public ─────────────────────────────────────────────────

    @Test
    void listarActivas_retornaLista() throws Exception {
        when(rutaRepository.findByActivaTrue()).thenReturn(List.of(rutaActiva()));

        mockMvc.perform(get("/api/rutas/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ruta Bosque"));
    }

    @Test
    void listarActivas_sinRutas_retornaListaVacia() throws Exception {
        when(rutaRepository.findByActivaTrue()).thenReturn(List.of());

        mockMvc.perform(get("/api/rutas/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/rutas/public/{id} ────────────────────────────────────────────

    @Test
    void obtenerPorId_rutaActiva_retorna200() throws Exception {
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(rutaActiva()));

        mockMvc.perform(get("/api/rutas/public/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ruta Bosque"));
    }

    @Test
    void obtenerPorId_rutaInactiva_retorna404() throws Exception {
        Ruta inactiva = rutaActiva();
        inactiva.setActiva(false);
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(inactiva));

        mockMvc.perform(get("/api/rutas/public/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerPorId_noExiste_retorna404() throws Exception {
        when(rutaRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rutas/public/99"))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Ruta rutaActiva() {
        return Ruta.builder()
                .id(1L).nombre("Ruta Bosque")
                .precio(BigDecimal.valueOf(50_000))
                .dificultad(Dificultad.MEDIA)
                .duracionMinutos(60)
                .activa(true)
                .build();
    }
}
