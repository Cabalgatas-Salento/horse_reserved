package horse_reserved.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DocumentoValidator implements ConstraintValidator<ValidDocumento, Object> {

    private static final String SOLO_NUMERICO = "^\\d+$";
    private static final String ALFANUMERICO   = "^[a-zA-Z0-9]+$";

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        String tipoDocumento;
        String documento;

        try {
            var clazz = value.getClass();
            tipoDocumento = (String) clazz.getMethod("getTipoDocumento").invoke(value);
            documento     = (String) clazz.getMethod("getDocumento").invoke(value);
        } catch (Exception e) {
            return true;
        }

        if (tipoDocumento == null || documento == null || documento.isBlank()) return true;

        String tipo = tipoDocumento.toUpperCase();
        boolean valid = switch (tipo) {
            case "CEDULA", "TARJETA_IDENTIDAD" -> documento.matches(SOLO_NUMERICO);
            case "PASAPORTE"                   -> documento.matches(ALFANUMERICO);
            default -> true;
        };

        if (!valid) {
            String msg = tipo.equals("PASAPORTE")
                    ? "El pasaporte solo puede contener letras y números"
                    : "El documento solo puede contener números";
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(msg)
                   .addPropertyNode("documento")
                   .addConstraintViolation();
        }

        return valid;
    }
}
