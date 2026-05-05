package horse_reserved.service;

import horse_reserved.dto.response.MetricasGananciasResponse;
import horse_reserved.dto.response.MetricasGananciasResponse.RangoMetrica;
import horse_reserved.dto.response.PuntoMetricaDTO;
import horse_reserved.model.EstadoTransaccion;
import horse_reserved.model.TipoMovimientoTransaccion;
import horse_reserved.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

/**
 * Servicio de métricas de ganancias.
 *
 * <p>Regla de negocio fundamental: <strong>solo se contabilizan las
 * transacciones con {@code estado = REALIZADO}</strong>. Las transacciones
 * PENDIENTES, FALLIDAS o CANCELADAS (reintentos de pago) se ignoran.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricasService {

    private static final Locale LOCALE_ES = Locale.of("es", "CO");

    private final TransaccionRepository transaccionRepository;

    /**
     * Ganancias agrupadas <b>por día</b>.
     *
     * @param inicio primer día del rango (inclusive)
     * @param fin    último día del rango  (inclusive)
     */
    public MetricasGananciasResponse getGananciasDiarias(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        LocalDateTime dtInicio = inicio.atStartOfDay();
        LocalDateTime dtFin    = fin.plusDays(1).atStartOfDay();   // fin exclusivo

        List<Object[]> rows = transaccionRepository.findGananciasAgrupadasPorDia(dtInicio, dtFin, TipoMovimientoTransaccion.PAGO);

        List<PuntoMetricaDTO> datos = rows.stream()
                .map(row -> {
                    String periodo = (String) row[0];            // YYYY-MM-DD
                    BigDecimal ganancias = toBigDecimal(row[1]);
                    Long cantidad = toLong(row[2]);

                    LocalDate fecha = LocalDate.parse(periodo);
                    String label = fecha.getDayOfMonth()
                            + "/" + String.format("%02d", fecha.getMonthValue())
                            + "/" + fecha.getYear();

                    return PuntoMetricaDTO.builder()
                            .periodo(periodo)
                            .label(label)
                            .ganancias(ganancias)
                            .cantidadTransacciones(cantidad)
                            .build();
                })
                .toList();

        return buildResponse(RangoMetrica.DIARIO, inicio, fin, datos);
    }

    /**
     * Ganancias agrupadas <b>por semana ISO</b> (lunes → domingo).
     *
     * @param inicio primer día del rango (inclusive)
     * @param fin    último día del rango  (inclusive)
     */
    public MetricasGananciasResponse getGananciasSemanales(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        LocalDateTime dtInicio = inicio.atStartOfDay();
        LocalDateTime dtFin    = fin.plusDays(1).atStartOfDay();

        List<Object[]> rows = transaccionRepository.findGananciasAgrupadasPorSemana(dtInicio, dtFin, TipoMovimientoTransaccion.PAGO);

        List<PuntoMetricaDTO> datos = rows.stream()
                .map(row -> {
                    String periodo = (String) row[0];            // YYYY-MM-DD (lunes de esa semana)
                    BigDecimal ganancias = toBigDecimal(row[1]);
                    Long cantidad = toLong(row[2]);

                    LocalDate lunesSemanana = LocalDate.parse(periodo);
                    LocalDate domingo       = lunesSemanana.plusDays(6);
                    int numSemana = lunesSemanana.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

                    String label = String.format("Sem. %d (%02d/%02d – %02d/%02d)",
                            numSemana,
                            lunesSemanana.getDayOfMonth(), lunesSemanana.getMonthValue(),
                            domingo.getDayOfMonth(),       domingo.getMonthValue());

                    // periodo para semanas: usar formato "YYYY-WNN"
                    String periodoIso = lunesSemanana.getYear() + "-W"
                            + String.format("%02d", numSemana);

                    return PuntoMetricaDTO.builder()
                            .periodo(periodoIso)
                            .label(label)
                            .ganancias(ganancias)
                            .cantidadTransacciones(cantidad)
                            .build();
                })
                .toList();

        return buildResponse(RangoMetrica.SEMANAL, inicio, fin, datos);
    }

    /**
     * Ganancias agrupadas <b>por mes</b>.
     *
     * @param inicio primer día del rango (inclusive)
     * @param fin    último día del rango  (inclusive)
     */
    public MetricasGananciasResponse getGananciasMenuales(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        LocalDateTime dtInicio = inicio.atStartOfDay();
        LocalDateTime dtFin    = fin.plusDays(1).atStartOfDay();

        List<Object[]> rows = transaccionRepository.findGananciasAgrupadasPorMes(dtInicio, dtFin, TipoMovimientoTransaccion.PAGO);

        List<PuntoMetricaDTO> datos = rows.stream()
                .map(row -> {
                    String periodo = (String) row[0];            // YYYY-MM
                    BigDecimal ganancias = toBigDecimal(row[1]);
                    Long cantidad = toLong(row[2]);

                    YearMonth ym = YearMonth.parse(periodo);
                    String nombreMes = ym.getMonth()
                            .getDisplayName(TextStyle.FULL, LOCALE_ES);
                    // Capitalizar primera letra
                    nombreMes = Character.toUpperCase(nombreMes.charAt(0))
                            + nombreMes.substring(1);
                    String label = nombreMes + " " + ym.getYear();

                    return PuntoMetricaDTO.builder()
                            .periodo(periodo)
                            .label(label)
                            .ganancias(ganancias)
                            .cantidadTransacciones(cantidad)
                            .build();
                })
                .toList();

        return buildResponse(RangoMetrica.MENSUAL, inicio, fin, datos);
    }

    /**
     * Ganancias agrupadas <b>por año</b>.
     *
     * @param inicio primer día del rango (inclusive)
     * @param fin    último día del rango  (inclusive)
     */
    public MetricasGananciasResponse getGananciasAnuales(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        LocalDateTime dtInicio = inicio.atStartOfDay();
        LocalDateTime dtFin    = fin.plusDays(1).atStartOfDay();

        List<Object[]> rows = transaccionRepository.findGananciasAgrupadasPorAnio(dtInicio, dtFin, TipoMovimientoTransaccion.PAGO);

        List<PuntoMetricaDTO> datos = rows.stream()
                .map(row -> {
                    String periodo = (String) row[0];            // YYYY
                    BigDecimal ganancias = toBigDecimal(row[1]);
                    Long cantidad = toLong(row[2]);

                    return PuntoMetricaDTO.builder()
                            .periodo(periodo)
                            .label(periodo)
                            .ganancias(ganancias)
                            .cantidadTransacciones(cantidad)
                            .build();
                })
                .toList();

        return buildResponse(RangoMetrica.ANUAL, inicio, fin, datos);
    }


    /**
     * Construye la respuesta calculando totales y promedio a partir de los puntos.
     */
    private MetricasGananciasResponse buildResponse(
            RangoMetrica rango,
            LocalDate inicio,
            LocalDate fin,
            List<PuntoMetricaDTO> datos) {

        BigDecimal totalGanancias = datos.stream()
                .map(PuntoMetricaDTO::getGanancias)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalTransacciones = datos.stream()
                .mapToLong(PuntoMetricaDTO::getCantidadTransacciones)
                .sum();

        BigDecimal promedio = (totalTransacciones > 0)
                ? totalGanancias.divide(BigDecimal.valueOf(totalTransacciones), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return MetricasGananciasResponse.builder()
                .rango(rango)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .totalGanancias(totalGanancias)
                .totalTransacciones(totalTransacciones)
                .promedioGananciasPorTransaccion(promedio)
                .datos(datos)
                .build();
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        if (Period.between(inicio, fin).getYears() > 5) {
            throw new IllegalArgumentException(
                    "El rango máximo permitido es de 5 años.");
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }
}