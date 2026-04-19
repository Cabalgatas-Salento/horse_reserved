package horse_reserved.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO usado para obtener las metricas economicas de un determinado periodo de tiempo
 */
public record MetricasPagosResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal ingresosBrutos,
        BigDecimal totalReembolsos,
        BigDecimal ingresosNetos,
        BigDecimal ticketPromedio,
        long cantidadPagosRealizados
) {}