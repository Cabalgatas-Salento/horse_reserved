package horse_reserved.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import horse_reserved.dto.request.CaballoRequest;
import horse_reserved.dto.response.CaballoResponse;
import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.service.AuditLogService;
import horse_reserved.service.CaballoService;
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
class CaballoControllerTest {

    @Mock CaballoService caballoService;
    @Mock AuditLogService auditLogService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CaballoController controller = new CaballoController(caballoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .addPlaceholderValue("cors.allowed-origins", "*")
                .build();
        objectMapper = new ObjectMapper();
    }

    // ── GET /api/caballos ─────────────────────────────────────────────────────

    @Test
    void listar_sinFiltro_retorna200() throws Exception {
        when(caballoService.listar(null)).thenReturn(List.of(caballoResponse()));

        mockMvc.perform(get("/api/caballos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Tornado"));
    }

    @Test
    void listar_filtroActivos_retorna200() throws Exception {
        when(caballoService.listar(true)).thenReturn(List.of(caballoResponse()));

        mockMvc.perform(get("/api/caballos").param("activos", "true"))
                .andExpect(status().isOk());
    }

    // ── GET /api/caballos/{id} ────────────────────────────────────────────────

    @Test
    void obtener_idExistente_retorna200() throws Exception {
        when(caballoService.obtener(1L)).thenReturn(caballoResponse());

        mockMvc.perform(get("/api/caballos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void obtener_noExiste_retorna404() throws Exception {
        when(caballoService.obtener(99L))
                .thenThrow(new ResourceNotFoundException("Caballo no encontrado: 99"));

        mockMvc.perform(get("/api/caballos/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/caballos ────────────────────────────────────────────────────

    @Test
    void crear_requestValido_retorna201() throws Exception {
        when(caballoService.crear(any())).thenReturn(caballoResponse());

        mockMvc.perform(post("/api/caballos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caballoRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Tornado"));
    }

    @Test
    void crear_sinNombre_retorna400() throws Exception {
        CaballoRequest request = new CaballoRequest(null, "Árabe", true);

        mockMvc.perform(post("/api/caballos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/caballos/{id} ────────────────────────────────────────────────

    @Test
    void actualizar_idExistente_retorna200() throws Exception {
        when(caballoService.actualizar(eq(1L), any())).thenReturn(caballoResponse());

        mockMvc.perform(put("/api/caballos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caballoRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_noExiste_retorna404() throws Exception {
        when(caballoService.actualizar(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Caballo no encontrado: 99"));

        mockMvc.perform(put("/api/caballos/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caballoRequest())))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/caballos/{id}/estado ──────────────────────────────────────

    @Test
    void cambiarEstado_desactiva_retorna200() throws Exception {
        CaballoResponse inactivo = CaballoResponse.builder()
                .id(1L).nombre("Tornado").raza("Árabe").activo(false).build();
        when(caballoService.cambiarEstado(1L, false)).thenReturn(inactivo);

        mockMvc.perform(patch("/api/caballos/1/estado").param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void cambiarEstado_sinParam_retorna400() throws Exception {
        mockMvc.perform(patch("/api/caballos/1/estado"))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CaballoResponse caballoResponse() {
        return CaballoResponse.builder().id(1L).nombre("Tornado").raza("Árabe").activo(true).build();
    }

    private CaballoRequest caballoRequest() {
        return new CaballoRequest("Tornado", "Árabe", true);
    }
}
