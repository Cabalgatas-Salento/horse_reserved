package horse_reserved.exception;

public class TwoFactorChallengeBlockedException extends RuntimeException {
    public TwoFactorChallengeBlockedException(String message) {
        super(message);
    }
}