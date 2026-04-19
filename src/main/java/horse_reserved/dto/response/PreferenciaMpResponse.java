package horse_reserved.dto.response;

public record PreferenciaMpResponse(
        Long intentoPagoId,
        String preferenceId,
        String initPoint,
        String sandboxInitPoint
) {}
