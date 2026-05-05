package horse_reserved.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

/**
 * Punto de dato individual dentro de una serie de métricas.
 * <p>
 * {@code periodo} es el identificador técnico ISO (ej: "2026-05", "2026-W18").
 * {@code label} es la representación legible para mostrar en el eje X de la gráfica.
 * </p>
 *
 * <h3>Formatos por rango:</h3>
 * <pre>
 *   DIARIO   → periodo: "2026-05-05"  | label: "05/05/2026"
 *   SEMANAL  → periodo: "2026-04-27"  | label: "Sem. 18 (27/04 – 03/05)"
 *   MENSUAL  → periodo: "2026-05"     | label: "Mayo 2026"
 *   ANUAL    → periodo: "2026"        | label: "2026"
 * </pre>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PuntoMetricaDTO {

    /**
     * Identificador técnico del período (formato ISO).
     * Útil para ordenar, comparar y hacer joins en el frontend.
     */
    private String periodo;

    /**
     * Etiqueta legible para mostrar en el eje X de la gráfica.
     */
    private String label;

    /**
     * Suma de ganancias del período (solo transacciones REALIZADAS).
     */
    private BigDecimal ganancias;

    /**
     * Número de transacciones REALIZADAS en el período.
     */
    private Long cantidadTransacciones;
}