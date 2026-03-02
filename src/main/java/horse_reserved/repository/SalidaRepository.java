package horse_reserved.repository;

import horse_reserved.model.Salida;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalidaRepository extends JpaRepository<Salida, Long> {

    @EntityGraph(attributePaths = {"ruta", "caballos", "guias"})
    Optional<Salida> findWithRutaById(Long id);
}