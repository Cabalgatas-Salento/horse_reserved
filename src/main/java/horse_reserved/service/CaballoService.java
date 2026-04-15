package horse_reserved.service;

import horse_reserved.dto.request.CaballoRequest;
import horse_reserved.dto.response.CaballoResponse;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.model.Caballo;
import horse_reserved.repository.CaballoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaballoService {

    private final CaballoRepository caballoRepository;

    @Transactional(readOnly = true)
    public List<CaballoResponse> listar(Boolean soloActivos) {
        List<Caballo> todos = caballoRepository.findAll();
        if (soloActivos != null) {
            todos = todos.stream().filter(c -> c.isActivo() == soloActivos).toList();
        }
        return todos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CaballoResponse obtener(Long id) {
        return toResponse(caballoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caballo no encontrado: " + id)));
    }

    @Transactional
    public CaballoResponse crear(CaballoRequest request) {
        log.info("Creando caballo — nombre={}", request.getNombre());
        Caballo caballo = Caballo.builder()
                .nombre(request.getNombre().trim())
                .raza(request.getRaza().trim())
                .activo(request.isActivo())
                .build();
        return toResponse(caballoRepository.save(caballo));
    }

    @Transactional
    public CaballoResponse actualizar(Long id, CaballoRequest request) {
        log.info("Actualizando caballo — id={}", id);
        Caballo caballo = caballoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caballo no encontrado: " + id));
        caballo.setNombre(request.getNombre().trim());
        caballo.setRaza(request.getRaza().trim());
        caballo.setActivo(request.isActivo());
        return toResponse(caballoRepository.save(caballo));
    }

    @Transactional
    public CaballoResponse cambiarEstado(Long id, boolean activo) {
        log.info("Cambiando estado caballo — id={}, activo={}", id, activo);
        Caballo caballo = caballoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caballo no encontrado: " + id));
        caballo.setActivo(activo);
        return toResponse(caballoRepository.save(caballo));
    }

    private CaballoResponse toResponse(Caballo c) {
        return CaballoResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .raza(c.getRaza())
                .activo(c.isActivo())
                .build();
    }
}
