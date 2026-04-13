package horse_reserved.dto.response;

import horse_reserved.model.PagoEstado;
import horse_reserved.model.Transaccion;
import horse_reserved.model.TipoMovimientoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO usado para mapear los datos despues de la creacion de una transaccion
 */
public record TransaccionResponse(
        Long id,
        Long intentoPagoId,
        TipoMovimientoTransaccion tipoMovimiento,
        PagoEstado estado,
        BigDecimal monto,
        String moneda,
        String detalle,
        LocalDateTime fechaTransaccion
) {
    public static TransaccionResponse from(Transaccion t) {
        return new TransaccionResponse(
                t.getId(),
                t.getIntentoPago().getId(),
                t.getTipoMovimiento(),
                t.getEstado(),
                t.getMonto(),
                t.getMoneda(),
                t.getDetalle(),
                t.getFechaTransaccion()
        );
    }
}