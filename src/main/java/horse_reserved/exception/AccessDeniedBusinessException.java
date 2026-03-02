package horse_reserved.exception;

/**
 * Excepcion que sirve para restricciones de accesos segun regla de negocio,
 * por ejemplo, obtener las informacion de una reservacion que no es del
 * usuario
 */
public class AccessDeniedBusinessException extends RuntimeException {
    public AccessDeniedBusinessException(String message) {
        super(message);
    }
}
