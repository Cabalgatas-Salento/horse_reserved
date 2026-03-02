package horse_reserved.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Clase que sirve para tranformar los objetos en informacion necesario para
 * las peticiones
 */
public class ParticipanteRequest {

    @NotBlank
    @Size(max = 100)
    private String primerNombre;

    @NotBlank
    @Size(max = 100)
    private String primerApellido;

    @NotBlank
    @Size(max = 50)
    private String tipoDocumento; // CEDULA, PASAPORTE, TARJETA_IDENTIDAD

    @NotBlank
    @Size(max = 50)
    private String documento;

    @Min(1)
    @Max(119)
    private short edad;

    @Min(1)
    private short cmAltura;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal kgPeso;
}