package horse_reserved.exception;

public class InvalidOrExpiredTwoFactorCodeException extends RuntimeException {
    public InvalidOrExpiredTwoFactorCodeException(String message) {
        super(message);
    }
}