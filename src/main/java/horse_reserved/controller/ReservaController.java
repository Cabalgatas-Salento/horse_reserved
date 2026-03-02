package horse_reserved.controller;

import horse_reserved.dto.request.CreateReservaRequest;
import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
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
     * Metodo que llama al servicio de reservaciones para crear una reserva
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<ReservaResponse> crearReserva(@Valid @RequestBody CreateReservaRequest request) {
        ReservaResponse response = reservaService.crearReserva(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Metodo que llama al servicio de reservaciones para obtener las reservaciones del usuario
     * @return
     */
    @GetMapping("/mias")
    public ResponseEntity<List<ReservaResponse>> misReservas() {
        return ResponseEntity.ok(reservaService.listarMisReservas());
    }

    /**
     * Metodo que llama al servicio de reservaciones para obtener una reserva por su id
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    /**
     * Metodo que llama al servicio de reservaciones para cancelar una reserva
     * @param id
     * @return
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelarReserva(id));
    }
}
