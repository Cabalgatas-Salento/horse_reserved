package horse_reserved.model;

public enum AuditAccion {
    // AUTENTICACION
    LOGIN_FALLIDO,
    LOGOUT,
    REGISTRO_EXITOSO,
    LOGIN_PASO1_EXITOSO,          // Credenciales correctas; OTP enviado
    LOGIN_2FA_OTP_ENVIADO,        // Email con OTP despachado
    LOGIN_2FA_OTP_VERIFICADO,     // OTP correcto; JWT emitido (login completo)
    LOGIN_2FA_OTP_FALLIDO,        // OTP incorrecto; se incrementaron intentos
    LOGIN_2FA_CHALLENGE_BLOQUEADO,// Máximo de intentos alcanzado
    LOGIN_2FA_CHALLENGE_EXPIRADO, // Challenge TTL vencido
    LOGIN_2FA_OTP_REENVIADO,       // Nuevo OTP generado y enviado

    // RESERVA
    RESERVA_CREADA,
    RESERVA_ACTUALIZADA,
    RESERVA_CANCELADA,

    // RECURSO_ADMIN
    RUTA_CREADA,
    RUTA_ACTUALIZADA,
    CABALLO_CREADO,
    CABALLO_ACTUALIZADO,
    CABALLO_ESTADO_CAMBIADO,
    GUIA_CREADO,
    GUIA_ACTUALIZADO,
    GUIA_ESTADO_CAMBIADO,

    // CUENTA
    CAMBIO_PASSWORD,
    RESET_PASSWORD_SOLICITADO,
    RESET_PASSWORD_COMPLETADO,
    BAJA_CUENTA,

    // SISTEMA
    ERROR_INTERNO
}
