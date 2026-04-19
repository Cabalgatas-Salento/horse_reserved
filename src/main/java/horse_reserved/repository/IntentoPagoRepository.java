package horse_reserved.repository;

import horse_reserved.model.IntentoPago;
import horse_reserved.model.PagoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IntentoPagoRepository extends JpaRepository<IntentoPago, Long> {

    boolean existsByReservaIdAndEstado(Long reservaId, PagoEstado estado);

    Optional<IntentoPago> findByMpPaymentId(String mpPaymentId);

    Optional<IntentoPago> findByMpPreferenceId(String mpPreferenceId);

    Optional<IntentoPago> findByReservaIdAndEstado(Long reservaId, PagoEstado estado);

    @Query("""
        SELECT COUNT(i) FROM IntentoPago i
        WHERE i.reserva.id = :reservaId AND i.estado = 'REALIZADO'
        """)
    long countRealizadosPorReserva(@Param("reservaId") Long reservaId);
}