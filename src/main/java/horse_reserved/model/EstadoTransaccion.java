package horse_reserved.model;

/**
 * Estados posibles de una transacción de pago.
 * Solo las transacciones con estado {@code REALIZADO}
 * se contabilizan como ganancias efectivas.
 */
public enum EstadoTransaccion {

    /** Intento de pago iniciado, pendiente de confirmación. */
    PENDIENTE,

    /** Pago confirmado y acreditado. Cuenta como ganancia. */
    REALIZADO,

    /** Pago rechazado por la pasarela o el banco. */
    FALLIDO,

    /** Cancelado por el usuario o el sistema antes de completarse. */
    CANCELADO
}
