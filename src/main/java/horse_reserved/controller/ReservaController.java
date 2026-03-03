package horse_reserved.controller;

import horse_reserved.dto.request.CreateReservaRequest;
import horse_reserved.dto.request.UpdateReservaRequest;
import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
/**
 * Clase creada para definir los endpoints relacionados con reservaciones
 */
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * Solo ADMINISTRADOR puede listar todas las reservas del sistema
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<List<ReservaResponse>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    /**
     * CLIENTE ve sus propias reservas; OPERADOR ve las reservas que gestionó
     */
    @GetMapping("/mias")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR')")
    public ResponseEntity<List<ReservaResponse>> misReservas() {
        return ResponseEntity.ok(reservaService.listarMisReservas());
    }

    /**
     * Cualquier rol autenticado puede consultar una reserva por id (el servicio valida el acceso)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    /**
     * CLIENTE crea su propia reserva; OPERADOR crea una reserva para un cliente especificado en clienteId
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR')")
    public ResponseEntity<ReservaResponse> crearReserva(@Valid @RequestBody CreateReservaRequest request) {
        ReservaResponse response = reservaService.crearReserva(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CLIENTE actualiza solo sus reservas; OPERADOR actualiza reservas de cualquier cliente.
     * Permite cambiar ruta, fecha/hora y participantes. Solo reservas en estado "reservado".
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR')")
    public ResponseEntity<ReservaResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateReservaRequest request) {
        return ResponseEntity.ok(reservaService.actualizarReserva(id, request));
    }

    /**
     * CLIENTE cancela solo sus reservas; OPERADOR cancela reservas de cualquier cliente
     */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR')")
    public ResponseEntity<ReservaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelarReserva(id));
    }
}
