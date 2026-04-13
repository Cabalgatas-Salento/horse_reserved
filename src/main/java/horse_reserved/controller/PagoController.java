package horse_reserved.controller;

import horse_reserved.dto.request.CrearIntentoPagoRequest;
import horse_reserved.dto.request.ReembolsarPagoRequest;
import horse_reserved.dto.response.IntentoPagoResponse;
import horse_reserved.dto.response.MetricasPagosResponse;
import horse_reserved.dto.response.TransaccionResponse;
import horse_reserved.service.PagoSimuladoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controlador REST para la gestión de pagos simulados.
 * Expone operaciones para crear intentos de pago, procesar reembolsos
 * y consultar información relacionada.
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoSimuladoService pagoService;

    /**
     * Crea un nuevo intento de pago.
     */
    @PostMapping("/intentos")
    public ResponseEntity<IntentoPagoResponse> crearIntento(
            @Valid @RequestBody CrearIntentoPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.crearIntento(request));
    }

    /**
     * Procesa el reembolso de un pago existente.
     */
    @PostMapping("/reembolsos")
    public ResponseEntity<IntentoPagoResponse> reembolsar(
            @Valid @RequestBody ReembolsarPagoRequest request) {
        return ResponseEntity.ok(pagoService.reembolsar(request));
    }

    /**
     * Obtiene un intento de pago por ID.
     */
    @GetMapping("/intentos/{id}")
    public ResponseEntity<IntentoPagoResponse> obtenerIntento(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerIntento(id));
    }

    /**
     * Obtiene una transacción por ID.
     */
    @GetMapping("/transacciones/{id}")
    public ResponseEntity<TransaccionResponse> obtenerTransaccion(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerTransaccion(id));
    }

    /**
     * Consulta métricas de pagos en un rango de fechas.
     */
    @GetMapping("/metricas")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<MetricasPagosResponse> metricas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(pagoService.calcularMetricas(desde, hasta));
    }
}