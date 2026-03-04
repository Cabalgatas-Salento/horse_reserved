package horse_reserved.repository;

import horse_reserved.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {
    List<Ruta> findByActivaTrue();
}
