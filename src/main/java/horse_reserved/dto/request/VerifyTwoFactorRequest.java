package horse_reserved.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VerifyTwoFactorRequest {

    @NotBlank(message = "El challengeId es requerido")
    @Size(max = 36, message = "challengeId inválido")
    private String challengeId;

    @NotBlank(message = "El código OTP es requerido")
    @Pattern(regexp = "\\d{6}", message = "El OTP debe ser de exactamente 6 dígitos numéricos")
    private String otp;

    public String getChallengeId()              { return challengeId; }
    public void setChallengeId(String id)       { this.challengeId = id; }
    public String getOtp()                      { return otp; }
    public void setOtp(String otp)              { this.otp = otp; }
}