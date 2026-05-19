package horse_reserved.dto.response;

/**
 * Respuesta del Paso 1 de login.
 * Nunca incluye JWT; indica al frontend que debe solicitar el OTP.
 */
public class TwoFactorChallengeResponse {

    private final String  challengeId;
    private final String  message;
    private final int     expiresInSeconds;
    /** Siempre true; permite al frontend detectar este payload sin inspeccionar campos. */
    private final boolean requiresVerification = true;

    public TwoFactorChallengeResponse(String challengeId, String message, int expiresInSeconds) {
        this.challengeId      = challengeId;
        this.message          = message;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String  getChallengeId()        { return challengeId; }
    public String  getMessage()            { return message; }
    public int     getExpiresInSeconds()   { return expiresInSeconds; }
    public boolean isRequiresVerification(){ return requiresVerification; }
}