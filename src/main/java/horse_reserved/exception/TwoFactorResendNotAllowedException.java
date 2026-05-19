package horse_reserved.exception;

public class TwoFactorResendNotAllowedException extends RuntimeException {
    public TwoFactorResendNotAllowedException(String message) {
        super(message);
    }
}