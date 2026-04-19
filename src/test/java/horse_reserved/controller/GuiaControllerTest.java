package horse_reserved.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import horse_reserved.dto.request.GuiaRequest;
import horse_reserved.dto.response.GuiaResponse;
import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.service.AuditLogService;
import horse_reserved.service.GuiaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GuiaControllerTest {

    @Mock GuiaService guiaService;
    @Mock AuditLogService auditLogService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        GuiaController controller = new GuiaController(guiaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .addPlaceholderValue("cors.allowed-origins", "*")
                .build();
        objectMapper = new ObjectMapper();
    }

    // ── GET /api/guias ────────────────────────────────────────────────────────

    @Test
    void listar_sinFiltro_retorna200() throws Exception {
        when(guiaService.listar(null)).thenReturn(List.of(guiaResponse()));

        mockMvc.perform(get("/api/guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Carlos"));
    }

    @Test
    void listar_filtroActivos_retorna200() throws Exception {
        when(guiaService.listar(true)).thenReturn(List.of(guiaResponse()));

        mockMvc.perform(get("/api/guias").param("activos", "true"))
                .andExpect(status().isOk());
    }

    // ── GET /api/guias/{id} ───────────────────────────────────────────────────

    @Test
    void obtener_idExistente_retorna200() throws Exception {
        when(guiaService.obtener(1L)).thenReturn(guiaResponse());

        mockMvc.perform(get("/api/guias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("carlos@guia.com"));
    }

    @Test
    void obtener_noExiste_retorna404() throws Exception {
        when(guiaService.obtener(99L))
                .thenThrow(new ResourceNotFoundException("Guía no encontrado: 99"));

        mockMvc.perform(get("/api/guias/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/guias ───────────────────────────────────────────────────────

    @Test
    void crear_requestValido_retorna201() throws Exception {
        when(guiaService.crear(any())).thenReturn(guiaResponse());

        mockMvc.perform(post("/api/guias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guiaRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Carlos"));
    }

    @Test
    void crear_sinNombre_retorna400() throws Exception {
        GuiaRequest request = new GuiaRequest(null, "3001234567", "carlos@guia.com", true);

        mockMvc.perform(post("/api/guias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/guias/{id} ───────────────────────────────────────────────────

    @Test
    void actualizar_idExistente_retorna200() throws Exception {
        when(guiaService.actualizar(eq(1L), any())).thenReturn(guiaResponse());

        mockMvc.perform(put("/api/guias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guiaRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_noExiste_retorna404() throws Exception {
        when(guiaService.actualizar(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Guía no encontrado: 99"));

        mockMvc.perform(put("/api/guias/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guiaRequest())))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/guias/{id}/estado ──────────────────────────────────────────

    @Test
    void cambiarEstado_desactiva_retorna200() throws Exception {
        GuiaResponse inactivo = GuiaResponse.builder()
                .id(1L).nombre("Carlos").telefono("3001234567")
                .email("carlos@guia.com").activo(false).build();
        when(guiaService.cambiarEstado(1L, false)).thenReturn(inactivo);

        mockMvc.perform(patch("/api/guias/1/estado").param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void cambiarEstado_sinParam_retorna400() throws Exception {
        mockMvc.perform(patch("/api/guias/1/estado"))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private GuiaResponse guiaResponse() {
        return GuiaResponse.builder()
                .id(1L).nombre("Carlos").telefono("3001234567")
                .email("carlos@guia.com").activo(true).build();
    }

    private GuiaRequest guiaRequest() {
        return new GuiaRequest("Carlos", "3001234567", "carlos@guia.com", true);
    }
}
