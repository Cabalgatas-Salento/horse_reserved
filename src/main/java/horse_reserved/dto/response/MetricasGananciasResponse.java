package horse_reserved.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta completa de métricas de ganancias.
 * <p>
 * El campo {@code datos} está formateado para consumo directo en librerías
 * de gráficos (Chart.js, Recharts, ApexCharts, etc.).
 * </p>
 *
 * <p>Ejemplo de uso en frontend:</p>
 * <pre>
 *   // Chart.js
 *   labels: response.datos.map(d => d.label)
 *   data:   response.datos.map(d => d.ganancias)
 * </pre>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricasGananciasResponse {

    /** Tipo de agrupación aplicado. */
    private RangoMetrica rango;

    /** Fecha de inicio del período consultado. */
    private LocalDate fechaInicio;

    /** Fecha de fin del período consultado (inclusive). */
    private LocalDate fechaFin;

    /** Suma total de ganancias (solo transacciones REALIZADAS) en el rango. */
    private BigDecimal totalGanancias;

    /** Total de transacciones REALIZADAS en el rango. */
    private Long totalTransacciones;

    /** Ganancia promedio por transacción en el rango. */
    private BigDecimal promedioGananciasPorTransaccion;

    /**
     * Puntos de datos agrupados según el rango.
     * Listo para alimentar el eje X (labels) y eje Y (data) de una gráfica.
     */
    private List<PuntoMetricaDTO> datos;

    // ─────────────────────────────────────────────────────────────────────────
    // Enum de rangos disponibles
    // ─────────────────────────────────────────────────────────────────────────

    public enum RangoMetrica {
        DIARIO,
        SEMANAL,
        MENSUAL,
        ANUAL
    }
}

