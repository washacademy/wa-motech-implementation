package org.motechproject.wa.swc.domain.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { SwcValidator.class })
@Documented
public @interface ValidSwachchagrahi {

    String message() default "Active SWCs must have Location set.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
