package horse_reserved.controller;

import horse_reserved.dto.response.RutaResponse;
import horse_reserved.model.Ruta;
import horse_reserved.repository.RutaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final RutaRepository rutaRepository;

    @GetMapping("/public")
    public ResponseEntity<List<RutaResponse>> listarActivas() {
        List<RutaResponse> rutas = rutaRepository.findByActivaTrue().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(rutas);
    }

    private RutaResponse toResponse(Ruta ruta) {
        return RutaResponse.builder()
                .id(ruta.getId())
                .nombre(ruta.getNombre())
                .descripcion(ruta.getDescripcion())
                .dificultad(ruta.getDificultad())
                .duracionMinutos(ruta.getDuracionMinutos())
                .urlImagen(ruta.getUrlImagen())
                .build();
    }
}
