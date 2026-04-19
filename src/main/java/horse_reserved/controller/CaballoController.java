package horse_reserved.controller;

import horse_reserved.dto.request.CaballoRequest;
import horse_reserved.dto.response.CaballoResponse;
import horse_reserved.service.CaballoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caballos")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CaballoController {

    private final CaballoService caballoService;

    /**
     * Lista todos los caballos. Query param opcional: activos=true|false (sin param = todos)
     */
    @GetMapping
    public ResponseEntity<List<CaballoResponse>> listar(
            @RequestParam(required = false) Boolean activos) {
        return ResponseEntity.ok(caballoService.listar(activos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaballoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(caballoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<CaballoResponse> crear(@Valid @RequestBody CaballoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caballoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<CaballoResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody CaballoRequest request) {
        return ResponseEntity.ok(caballoService.actualizar(id, request));
    }

    /**
     * Activa o desactiva un caballo (borrado lógico). Query param: activo=true|false
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<CaballoResponse> cambiarEstado(@PathVariable Long id,
                                                         @RequestParam boolean activo) {
        return ResponseEntity.ok(caballoService.cambiarEstado(id, activo));
    }
}
