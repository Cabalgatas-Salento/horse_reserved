package horse_reserved.dto.response;

import horse_reserved.model.IntentoPago;
import horse_reserved.model.MetodoPago;
import horse_reserved.model.PagoEstado;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *DTO para la respuesta a la solicitud de creacion de intento de pago
 * usado para mapear los datos de la respuesta a la solicitud de creacion de intento de pago
 */
public record IntentoPagoResponse(
        Long id,
        Long reservaId,
        PagoEstado estado,
        MetodoPago metodoPago,
        BigDecimal monto,
        String moneda,
        Long pagadoPorUsuarioId,
        Long pagadoPorOperadorId,
        String referenciaSimulada,
        LocalDateTime fechaIntento
) {
    public static IntentoPagoResponse from(IntentoPago i) {
        return new IntentoPagoResponse(
                i.getId(),
                i.getReserva().getId(),
                i.getEstado(),
                i.getMetodoPago(),
                i.getMonto(),
                i.getMoneda(),
                i.getPagadoPorUsuario() != null ? i.getPagadoPorUsuario().getId() : null,
                i.getPagadoPorOperador() != null ? i.getPagadoPorOperador().getId() : null,
                i.getReferenciaSimulada(),
                i.getFechaIntento()
        );
    }
}