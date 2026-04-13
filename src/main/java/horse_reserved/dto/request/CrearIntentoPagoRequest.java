package horse_reserved.dto.request;

import horse_reserved.model.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO para solicitud de creacion de intento de pago
 * se usa para mapear los datos de la creacion del intento de partir de la solicitud
 */
public record CrearIntentoPagoRequest(

        @NotNull Long reservaId,

        @NotNull MetodoPago metodoPago,

        @NotNull @Positive BigDecimal monto,

        /** ID del usuario dueño de la reserva (exclusivo con operadorId) */
        Long usuarioId,

        /** ID del operador que paga a nombre del usuario (exclusivo con usuarioId) */
        Long operadorId
) {}