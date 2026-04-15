package horse_reserved.controller;

import horse_reserved.dto.request.GuiaRequest;
import horse_reserved.dto.response.GuiaResponse;
import horse_reserved.service.GuiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class GuiaController {

    private final GuiaService guiaService;

    /**
     * Lista todos los guías. Query param opcional: activos=true|false (sin param = todos)
     */
    @GetMapping
    public ResponseEntity<List<GuiaResponse>> listar(
            @RequestParam(required = false) Boolean activos) {
        return ResponseEntity.ok(guiaService.listar(activos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuiaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<GuiaResponse> crear(@Valid @RequestBody GuiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guiaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<GuiaResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody GuiaRequest request) {
        return ResponseEntity.ok(guiaService.actualizar(id, request));
    }

    /**
     * Activa o desactiva un guía (borrado lógico). Query param: activo=true|false
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<GuiaResponse> cambiarEstado(@PathVariable Long id,
                                                      @RequestParam boolean activo) {
        return ResponseEntity.ok(guiaService.cambiarEstado(id, activo));
    }
}
