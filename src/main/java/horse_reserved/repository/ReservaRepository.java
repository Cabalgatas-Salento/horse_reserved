package horse_reserved.repository;

import horse_reserved.model.Reserva;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @EntityGraph(attributePaths = {"salida", "salida.ruta", "cliente", "operador", "participantes"})
    List<Reserva> findByClienteIdOrderByIdDesc(Long clienteId);

    @EntityGraph(attributePaths = {"salida", "salida.ruta", "cliente", "operador", "participantes"})
    List<Reserva> findBySalidaIdOrderByIdDesc(Long salidaId);

    @EntityGraph(attributePaths = {"salida", "salida.ruta", "cliente", "operador", "participantes"})
    java.util.Optional<Reserva> findDetailedById(Long id);

    @Query("""
        select coalesce(sum(r.cantPersonas), 0)
        from Reserva r
        where r.salida.id = :salidaId
          and r.estado <> 'cancelado'
    """)
    long sumPersonasReservadasActivasBySalida(@Param("salidaId") Long salidaId);
}