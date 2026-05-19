package horse_reserved.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResendTwoFactorRequest {

    /*
     * Decisión de diseño: se recibe challengeId y NO email.
     * Razón: el cliente ya tiene el challengeId desde el Paso 1; usarlo evita
     * exponer si un email existe en el sistema y elimina una búsqueda extra.
     */
    @NotBlank(message = "El challengeId es requerido")
    @Size(max = 36, message = "challengeId inválido")
    private String challengeId;

    public String getChallengeId()        { return challengeId; }
    public void setChallengeId(String id) { this.challengeId = id; }
}