package horse_reserved.dto.request;

import jakarta.validation.constraints.NotNull;

public record CrearPreferenciaMpRequest(
        @NotNull Long reservaId
) {}
