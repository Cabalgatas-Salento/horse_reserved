package horse_reserved.repository;

import horse_reserved.model.Salida;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalidaRepository extends JpaRepository<Salida, Long> {

    @EntityGraph(attributePaths = {"ruta", "caballos", "guias"})
    Optional<Salida> findWithRutaById(Long id);

    @EntityGraph(attributePaths = {"ruta", "caballos"})
    @Query("""
            SELECT s FROM Salida s
            WHERE s.ruta.id        = :rutaId
              AND s.fechaProgramada = :fecha
              AND s.tiempoInicio    = :horaInicio
              AND s.estado          = 'programado'
            """)
    Optional<Salida> findProgramadaByRutaAndFechaAndHora(@Param("rutaId") Long rutaId,
                                                          @Param("fecha") LocalDate fecha,
                                                          @Param("horaInicio") LocalTime horaInicio);

    @Query("SELECT s FROM Salida s WHERE s.estado = 'programado' AND s.fechaProgramada <= :today")
    List<Salida> findProgramadasHastaHoy(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE Salida s SET s.estado = 'completado' WHERE s.id IN :ids")
    int completarPorIds(@Param("ids") List<Long> ids);
}