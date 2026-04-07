package horse_reserved.util;

/**
 * Utilidad para enmascarar datos sensibles en logs.
 * Nunca registrar: contraseñas, tokens JWT completos, secrets.
 */
public final class LogMaskUtil {

    private LogMaskUtil() {}

    /**
     * Enmascara email: bryan@example.com → br***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String visible = local.length() > 2 ? local.substring(0, 2) : local.substring(0, 1);
        return visible + "***@" + parts[1];
    }

    /**
     * Muestra solo los últimos 6 caracteres de un token JWT para correlación.
     * Ejemplo: "...xYz123"
     */
    public static String maskToken(String token) {
        if (token == null || token.length() < 6) return "***";
        return "..." + token.substring(token.length() - 6);
    }

    /**
     * Enmascara número de documento: 1234567890 → 123***890
     */
    public static String maskDocumento(String documento) {
        if (documento == null || documento.length() < 6) return "***";
        return documento.substring(0, 3) + "***" + documento.substring(documento.length() - 3);
    }
}