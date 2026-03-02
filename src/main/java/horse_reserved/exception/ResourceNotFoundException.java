package horse_reserved.exception;

/**
 * Excepcion para manejar los errores de no haber encontrado un recurso especifico,
 * por ejemplo, el realizar una reserva y que no haya una salida seleccionada
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
