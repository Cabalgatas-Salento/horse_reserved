package horse_reserved.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para solicitud de reembolso
 * usado para mapear una solicitud de reembolso
 */
public record ReembolsarPagoRequest(
        @NotNull Long intentoPagoId,
        String motivo
) {}