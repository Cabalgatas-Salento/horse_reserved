package horse_reserved.dto.response;

public class ResendTwoFactorResponse {

    private final String message;
    private final int    remainingSeconds;

    public ResendTwoFactorResponse(String message, int remainingSeconds) {
        this.message          = message;
        this.remainingSeconds = remainingSeconds;
    }

    public String getMessage()         { return message; }
    public int    getRemainingSeconds() { return remainingSeconds; }
}