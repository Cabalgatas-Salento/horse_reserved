package horse_reserved.service;

import horse_reserved.model.Ruta;
import horse_reserved.repository.RutaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RutaService {

    private final RutaRepository rutaRepository;

    @Cacheable("rutas-activas")
    public List<Ruta> findActivas() {
        return rutaRepository.findByActivaTrue();
    }
}
