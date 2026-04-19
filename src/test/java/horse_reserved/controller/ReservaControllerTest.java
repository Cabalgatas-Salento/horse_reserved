package horse_reserved.controller;

import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.exception.AccessDeniedBusinessException;
import horse_reserved.exception.BusinessRuleException;
import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.service.AuditLogService;
import horse_reserved.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    @Mock ReservaService reservaService;
    @Mock AuditLogService auditLogService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReservaController controller = new ReservaController(reservaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .addPlaceholderValue("cors.allowed-origins", "*")
                .build();
    }

    // ── GET /api/reservaciones ────────────────────────────────────────────────

    @Test
    void listarTodas_retornaLista() throws Exception {
        when(reservaService.listarTodas()).thenReturn(java.util.List.of(reservaResponse()));

        mockMvc.perform(get("/api/reservaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ── GET /api/reservaciones/mias ───────────────────────────────────────────

    @Test
    void misReservas_retornaLista() throws Exception {
        when(reservaService.listarMisReservas()).thenReturn(java.util.List.of(reservaResponse()));

        mockMvc.perform(get("/api/reservaciones/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("reservado"));
    }

    // ── GET /api/reservaciones/{id} ───────────────────────────────────────────

    @Test
    void obtenerPorId_encontrada_retorna200() throws Exception {
        when(reservaService.obtenerPorId(1L)).thenReturn(reservaResponse());

        mockMvc.perform(get("/api/reservaciones/1"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorId_noExiste_retorna404() throws Exception {
        when(reservaService.obtenerPorId(99L))
                .thenThrow(new ResourceNotFoundException("Reserva no encontrada: 99"));

        mockMvc.perform(get("/api/reservaciones/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerPorId_accessDenied_retorna403() throws Exception {
        when(reservaService.obtenerPorId(1L))
                .thenThrow(new AccessDeniedBusinessException("No tienes permisos"));

        mockMvc.perform(get("/api/reservaciones/1"))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/reservaciones ───────────────────────────────────────────────

    @Test
    void crearReserva_requestValido_retorna201() throws Exception {
        when(reservaService.crearReserva(any())).thenReturn(reservaResponse());

        mockMvc.perform(post("/api/reservaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void crearReserva_sinRutaId_retorna400() throws Exception {
        String json = createRequestJson().replace("\"rutaId\":1,", "");

        mockMvc.perform(post("/api/reservaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearReserva_reglaDeNegocio_retorna400() throws Exception {
        when(reservaService.crearReserva(any()))
                .thenThrow(new BusinessRuleException("La fecha debe ser futura"));

        mockMvc.perform(post("/api/reservaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson()))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/reservaciones/{id}/cancelar ────────────────────────────────

    @Test
    void cancelar_exitoso_retorna200() throws Exception {
        when(reservaService.cancelarReserva(1L)).thenReturn(reservaResponse());

        mockMvc.perform(patch("/api/reservaciones/1/cancelar"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelar_yaEstaCandelada_retorna400() throws Exception {
        when(reservaService.cancelarReserva(1L))
                .thenThrow(new BusinessRuleException("La reserva ya está cancelada"));

        mockMvc.perform(patch("/api/reservaciones/1/cancelar"))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ReservaResponse reservaResponse() {
        return ReservaResponse.builder()
                .id(1L).estado("reservado").cantPersonas(1).build();
    }

    private String createRequestJson() {
        String fecha = LocalDate.now().plusDays(2).toString();
        return """
                {
                  "rutaId":1,
                  "fecha":"%s",
                  "horaInicio":"08:30:00",
                  "cantPersonas":1,
                  "participantes":[{
                    "primerNombre":"Ana","primerApellido":"García",
                    "tipoDocumento":"cedula","documento":"987654321",
                    "edad":25,"cmAltura":165,"kgPeso":60
                  }]
                }""".formatted(fecha);
    }
}
