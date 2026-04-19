package horse_reserved.controller;

import horse_reserved.dto.request.CrearPreferenciaMpRequest;
import horse_reserved.dto.response.IntentoPagoResponse;
import horse_reserved.dto.response.PreferenciaMpResponse;
import horse_reserved.service.PagoMercadoPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pagos/mp")
@RequiredArgsConstructor
public class MercadoPagoController {

    private final PagoMercadoPagoService mpService;

    @PostMapping("/preferencia")
    @PreAuthorize("hasAuthority('CLIENTE')")
    public ResponseEntity<PreferenciaMpResponse> crearPreferencia(
            @Valid @RequestBody CrearPreferenciaMpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mpService.crearPreferencia(request));
    }

    // Endpoint público: MP llama desde sus servidores sin JWT
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "id", required = false) String id) {

        log.info("Webhook MP recibido | type={} id={}", type, id);

        if ("payment".equals(type) && id != null) {
            mpService.procesarWebhook(id);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/estado/{intentoId}")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR', 'ADMINISTRADOR')")
    public ResponseEntity<IntentoPagoResponse> consultarEstado(@PathVariable Long intentoId) {
        return ResponseEntity.ok(mpService.consultarEstado(intentoId));
    }

    @PostMapping("/asociar-payment")
    @PreAuthorize("hasAuthority('CLIENTE')")
    public ResponseEntity<Void> asociarPaymentId(
            @RequestParam Long intentoId,
            @RequestParam String mpPaymentId) {
        mpService.asociarPaymentId(intentoId, mpPaymentId);
        return ResponseEntity.ok().build();
    }
}
