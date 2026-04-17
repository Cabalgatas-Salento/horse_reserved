package horse_reserved.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = HoraInicioValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidHoraInicio {
    String message() default "La hora de inicio debe estar entre las 8:30 a.m. y las 2:30 p.m.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
