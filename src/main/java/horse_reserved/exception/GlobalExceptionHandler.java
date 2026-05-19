package horse_reserved.exception;

import horse_reserved.dto.response.ErrorResponse;
import horse_reserved.service.AuditLogService;
import horse_reserved.util.HttpRequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la aplicación.
 * Intercepta todas las excepciones y retorna respuestas HTTP apropiadas.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditLogService auditLogService;

    // =========================================================================
    // Autenticación general
    // =========================================================================

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            Exception ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Email o contraseña incorrectos", request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<ErrorResponse> handleUserInactive(
            UserInactiveException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    // =========================================================================
    // 2FA
    // =========================================================================

    @ExceptionHandler(InvalidOrExpiredTwoFactorCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTwoFactor(
            InvalidOrExpiredTwoFactorCodeException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler(TwoFactorChallengeBlockedException.class)
    public ResponseEntity<ErrorResponse> handleTwoFactorBlocked(
            TwoFactorChallengeBlockedException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(TwoFactorResendNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleTwoFactorResendNotAllowed(
            TwoFactorResendNotAllowedException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                ex.getMessage(), request);
    }

    // =========================================================================
    // Validación y recursos
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName     = ((FieldError) error).getField();
            String errorMessage  = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status",    HttpStatus.BAD_REQUEST.value());
        response.put("error",     "Bad Request");
        response.put("message",   "Error de validación");
        response.put("errors",    errors);
        response.put("path",      request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(RecaptchaVerificationException.class)
    public ResponseEntity<ErrorResponse> handleRecaptcha(
            RecaptchaVerificationException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessRuleException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Business Rule Violation",
                ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedBusinessException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedBusiness(
            AccessDeniedBusinessException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "Cuerpo de la solicitud inválido o ausente", request);
    }

    // =========================================================================
    // Excepciones de tipo genérico (mantener al final para no solapar)
    // =========================================================================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Error no controlado: {}", ex.getMessage(), ex);
        auditLogService.registrarErrorSistema(
                ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                HttpRequestUtil.obtenerIpCliente());

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Ha ocurrido un error interno en el servidor", request);
    }

    // =========================================================================
    // Helper privado compartido
    // =========================================================================

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String error, String message, WebRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(status).body(body);
    }
}