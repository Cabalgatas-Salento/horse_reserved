package horse_reserved.exception;

/**
 * Excepcion para manejar errores relacionados con las reglas de negocio,
 * por ejemplo, no se puede cancelar una reserva de una salida que ya
 * sucedio o que las reservas necesitan al menos 1 participante
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
