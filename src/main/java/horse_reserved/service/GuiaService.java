package horse_reserved.service;

import horse_reserved.dto.request.GuiaRequest;
import horse_reserved.dto.response.GuiaResponse;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.model.Guia;
import horse_reserved.repository.GuiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaService {

    private final GuiaRepository guiaRepository;

    @Transactional(readOnly = true)
    public List<GuiaResponse> listar(Boolean soloActivos) {
        List<Guia> todos = guiaRepository.findAll();
        if (soloActivos != null) {
            todos = todos.stream().filter(g -> g.isActivo() == soloActivos).toList();
        }
        return todos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GuiaResponse obtener(Long id) {
        return toResponse(guiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrada: " + id)));
    }

    @Transactional
    public GuiaResponse crear(GuiaRequest request) {
        log.info("Creando guía — nombre={}", request.getNombre());
        Guia guia = Guia.builder()
                .nombre(request.getNombre().trim())
                .telefono(request.getTelefono().trim())
                .email(request.getEmail().trim().toLowerCase())
                .activo(request.isActivo())
                .build();
        return toResponse(guiaRepository.save(guia));
    }

    @Transactional
    public GuiaResponse actualizar(Long id, GuiaRequest request) {
        log.info("Actualizando guía — id={}", id);
        Guia guia = guiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrada: " + id));
        guia.setNombre(request.getNombre().trim());
        guia.setTelefono(request.getTelefono().trim());
        guia.setEmail(request.getEmail().trim().toLowerCase());
        guia.setActivo(request.isActivo());
        return toResponse(guiaRepository.save(guia));
    }

    @Transactional
    public GuiaResponse cambiarEstado(Long id, boolean activo) {
        log.info("Cambiando estado guía — id={}, activo={}", id, activo);
        Guia guia = guiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrada: " + id));
        guia.setActivo(activo);
        return toResponse(guiaRepository.save(guia));
    }

    private GuiaResponse toResponse(Guia g) {
        return GuiaResponse.builder()
                .id(g.getId())
                .nombre(g.getNombre())
                .telefono(g.getTelefono())
                .email(g.getEmail())
                .activo(g.isActivo())
                .build();
    }
}
