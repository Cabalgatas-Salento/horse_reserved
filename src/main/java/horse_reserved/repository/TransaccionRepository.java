package horse_reserved.repository;

import horse_reserved.model.PagoEstado;
import horse_reserved.model.Transaccion;
import horse_reserved.model.TipoMovimientoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    @Query("""
        SELECT COALESCE(SUM(t.monto), 0)
        FROM Transaccion t
        WHERE t.tipoMovimiento = :tipo
          AND t.estado = :estado
          AND t.fechaTransaccion BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarMontosPorTipoYEstado(
            @Param("tipo") TipoMovimientoTransaccion tipo,
            @Param("estado") PagoEstado estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT COUNT(t)
        FROM Transaccion t
        WHERE t.tipoMovimiento = :tipo
          AND t.estado = :estado
          AND t.fechaTransaccion BETWEEN :desde AND :hasta
        """)
    long contarPorTipoYEstado(
            @Param("tipo") TipoMovimientoTransaccion tipo,
            @Param("estado") PagoEstado estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}