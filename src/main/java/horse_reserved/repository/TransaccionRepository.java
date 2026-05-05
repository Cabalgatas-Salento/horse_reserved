package horse_reserved.repository;

import horse_reserved.model.PagoEstado;
import horse_reserved.model.Transaccion;
import horse_reserved.model.TipoMovimientoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * Ganancias agrupadas por día.
     * Retorna: [fecha (DATE como String), suma, conteo]
     */
    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('day', t.fecha_transaccion), 'YYYY-MM-DD') AS periodo,
                SUM(t.monto)                                                   AS ganancias,
                COUNT(t.id)                                                    AS cantidad
            FROM transacciones t
            WHERE t.tipoMovimiento = :tipo
              AND t.estado = 'REALIZADO'
              AND t.fecha_transaccion >= :inicio
              AND t.fecha_transaccion < :fin
            GROUP BY DATE_TRUNC('day', t.fecha_transaccion)
            ORDER BY DATE_TRUNC('day', t.fecha_transaccion)
            """, nativeQuery = true)
    List<Object[]> findGananciasAgrupadasPorDia(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin,
            @Param("tipo")   TipoMovimientoTransaccion tipo
    );

    /**
     * Ganancias agrupadas por semana ISO (inicio de semana = lunes).
     * Retorna: [inicio de semana (YYYY-MM-DD), suma, conteo]
     */
    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('week', t.fecha_transaccion), 'YYYY-MM-DD') AS periodo,
                SUM(t.monto)                                                    AS ganancias,
                COUNT(t.id)                                                     AS cantidad
            FROM transacciones t
            WHERE t.tipoMovimiento = :tipo
              AND t.estado = 'REALIZADO'
              AND t.fecha_transaccion >= :inicio
              AND t.fecha_transaccion < :fin
            GROUP BY DATE_TRUNC('week', t.fecha_transaccion)
            ORDER BY DATE_TRUNC('week', t.fecha_transaccion)
            """, nativeQuery = true)
    List<Object[]> findGananciasAgrupadasPorSemana(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin,
            @Param("tipo")   TipoMovimientoTransaccion tipo
    );

    /**
     * Ganancias agrupadas por mes.
     * Retorna: [mes (YYYY-MM), suma, conteo]
     */
    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('month', t.fecha_transaccion), 'YYYY-MM') AS periodo,
                SUM(t.monto)                                                  AS ganancias,
                COUNT(t.id)                                                   AS cantidad
            FROM transacciones t
            WHERE t.tipoMovimiento = :tipo
              AND t.estado = 'REALIZADO'
              AND t.fecha_transaccion >= :inicio
              AND t.fecha_transaccion < :fin
            GROUP BY DATE_TRUNC('month', t.fecha_transaccion)
            ORDER BY DATE_TRUNC('month', t.fecha_transaccion)
            """, nativeQuery = true)
    List<Object[]> findGananciasAgrupadasPorMes(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin,
            @Param("tipo")   TipoMovimientoTransaccion tipo
    );

    /**
     * Ganancias agrupadas por año.
     * Retorna: [año (YYYY), suma, conteo]
     */
    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('year', t.fecha_transaccion), 'YYYY') AS periodo,
                SUM(t.monto)                                              AS ganancias,
                COUNT(t.id)                                               AS cantidad
            FROM transacciones t
            WHERE t.tipoMovimiento = :tipo
              AND t.estado = 'REALIZADO'
              AND t.fecha_transaccion >= :inicio
              AND t.fecha_transaccion < :fin
            GROUP BY DATE_TRUNC('year', t.fecha_transaccion)
            ORDER BY DATE_TRUNC('year', t.fecha_transaccion)
            """, nativeQuery = true)
    List<Object[]> findGananciasAgrupadasPorAnio(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin,
            @Param("tipo")   TipoMovimientoTransaccion tipo
    );
}