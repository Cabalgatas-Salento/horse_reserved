package horse_reserved.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import horse_reserved.dto.request.CrearIntentoPagoRequest;
import horse_reserved.dto.request.ReembolsarPagoRequest;
import horse_reserved.dto.response.IntentoPagoResponse;
import horse_reserved.dto.response.MetricasPagosResponse;
import horse_reserved.exception.BusinessRuleException;
import horse_reserved.exception.GlobalExceptionHandler;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.model.MetodoPago;
import horse_reserved.model.PagoEstado;
import horse_reserved.service.AuditLogService;
import horse_reserved.service.PagoSimuladoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    @Mock PagoSimuladoService pagoService;
    @Mock AuditLogService auditLogService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PagoController controller = new PagoController(pagoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogService))
                .build();
        objectMapper = new ObjectMapper();
    }

    // ── POST /api/pagos/intentos ──────────────────────────────────────────────

    @Test
    void crearIntento_requestValido_retorna201() throws Exception {
        when(pagoService.crearIntento(any())).thenReturn(intentoResponse());

        mockMvc.perform(post("/api/pagos/intentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearIntentoPagoRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crearIntento_sinReservaId_retorna400() throws Exception {
        String json = "{\"metodoPago\":\"EFECTIVO\",\"monto\":50000}";

        mockMvc.perform(post("/api/pagos/intentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearIntento_reglaDeNegocio_retorna400() throws Exception {
        when(pagoService.crearIntento(any()))
                .thenThrow(new BusinessRuleException("Reserva ya pagada"));

        mockMvc.perform(post("/api/pagos/intentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearIntentoPagoRequest())))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/pagos/reembolsos ────────────────────────────────────────────

    @Test
    void reembolsar_requestValido_retorna200() throws Exception {
        when(pagoService.reembolsar(any())).thenReturn(intentoResponse());

        mockMvc.perform(post("/api/pagos/reembolsos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReembolsarPagoRequest(1L, "cliente solicitó cancelación"))))
                .andExpect(status().isOk());
    }

    @Test
    void reembolsar_sinIntentoPagoId_retorna400() throws Exception {
        mockMvc.perform(post("/api/pagos/reembolsos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"cancelación\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reembolsar_pagoNoExiste_retorna404() throws Exception {
        when(pagoService.reembolsar(any()))
                .thenThrow(new ResourceNotFoundException("Intento no encontrado: 99"));

        mockMvc.perform(post("/api/pagos/reembolsos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReembolsarPagoRequest(99L, null))))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/pagos/intentos/{id} ──────────────────────────────────────────

    @Test
    void obtenerIntento_encontrado_retorna200() throws Exception {
        when(pagoService.obtenerIntento(1L)).thenReturn(intentoResponse());

        mockMvc.perform(get("/api/pagos/intentos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void obtenerIntento_noExiste_retorna404() throws Exception {
        when(pagoService.obtenerIntento(99L))
                .thenThrow(new ResourceNotFoundException("Intento no encontrado: 99"));

        mockMvc.perform(get("/api/pagos/intentos/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/pagos/metricas ───────────────────────────────────────────────

    @Test
    void metricas_paramValidos_retorna200() throws Exception {
        when(pagoService.calcularMetricas(any(), any())).thenReturn(metricasResponse());

        mockMvc.perform(get("/api/pagos/metricas")
                        .param("desde", "2025-01-01")
                        .param("hasta", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadPagosRealizados").value(5));
    }

    @Test
    void metricas_sinParams_retorna400() throws Exception {
        mockMvc.perform(get("/api/pagos/metricas"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/pagos/transacciones/{id} ─────────────────────────────────────

    @Test
    void obtenerTransaccion_encontrada_retorna200() throws Exception {
        when(pagoService.obtenerTransaccion(1L)).thenReturn(transaccionResponse());

        mockMvc.perform(get("/api/pagos/transacciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void obtenerTransaccion_noExiste_retorna404() throws Exception {
        when(pagoService.obtenerTransaccion(99L))
                .thenThrow(new ResourceNotFoundException("Transacción no encontrada: 99"));

        mockMvc.perform(get("/api/pagos/transacciones/99"))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private IntentoPagoResponse intentoResponse() {
        return new IntentoPagoResponse(
                1L, 1L, PagoEstado.PENDIENTE, MetodoPago.EFECTIVO,
                BigDecimal.valueOf(50_000), "COP", 1L, null,
                "REF-001", LocalDateTime.now(), null, null, null);
    }

    private MetricasPagosResponse metricasResponse() {
        return new MetricasPagosResponse(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                BigDecimal.valueOf(250_000), BigDecimal.ZERO,
                BigDecimal.valueOf(250_000), BigDecimal.valueOf(50_000), 5L);
    }

    private CrearIntentoPagoRequest crearIntentoPagoRequest() {
        return new CrearIntentoPagoRequest(1L, MetodoPago.EFECTIVO, BigDecimal.valueOf(50_000), 1L, null);
    }

    private horse_reserved.dto.response.TransaccionResponse transaccionResponse() {
        return new horse_reserved.dto.response.TransaccionResponse(
                1L, 1L,
                horse_reserved.model.TipoMovimientoTransaccion.PAGO,
                PagoEstado.PENDIENTE,
                BigDecimal.valueOf(50_000), "COP",
                "Pago simulado", LocalDateTime.now());
    }
}
