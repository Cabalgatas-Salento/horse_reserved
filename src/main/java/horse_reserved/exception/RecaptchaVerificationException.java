package horse_reserved.exception;

public class RecaptchaVerificationException extends RuntimeException {
    public RecaptchaVerificationException(String message) {
        super(message);
    }
}
