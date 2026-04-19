package horse_reserved.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalTime;

public class HoraInicioValidator implements ConstraintValidator<ValidHoraInicio, LocalTime> {

    private static final LocalTime HORA_MIN = LocalTime.of(8, 30);
    private static final LocalTime HORA_MAX = LocalTime.of(14, 30);

    @Override
    public boolean isValid(LocalTime value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return !value.isBefore(HORA_MIN) && !value.isAfter(HORA_MAX);
    }
}
