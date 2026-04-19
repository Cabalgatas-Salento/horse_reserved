package horse_reserved.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import horse_reserved.validation.ValidDocumento;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ValidDocumento
public class RegisterRequest {

    @NotBlank(message = "El primer nombre es obligatorio")
    @Size(max = 100, message = "El primer nombre no puede exceder 100 caracteres")
    private String primerNombre;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(max = 100, message = "El primer apellido no puede exceder 100 caracteres")
    private String primerApellido;

    @NotNull(message = "El tipo de documento es obligatorio")
    private String tipoDocumento; // cedula, pasaporte, tarjeta_identidad

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 50, message = "El número de documento no puede exceder 50 caracteres")
    private String documento;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 200, message = "El email no puede exceder 200 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, message = "La contraseña debe tener al menos 12 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
        message = "La contraseña debe contener al menos una mayúscula, un número y un carácter especial"
    )
    private String password;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @NotBlank(message = "El token de reCAPTCHA es obligatorio")
    private String recaptchaToken;

    @NotNull(message = "Debe aceptar la política de tratamiento de datos")
    @AssertTrue(message = "Debe aceptar la política de tratamiento de datos")
    private Boolean habeasDataConsent;
}