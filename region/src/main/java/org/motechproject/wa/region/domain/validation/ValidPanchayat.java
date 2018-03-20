package org.motechproject.wa.region.domain.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { PanchayatValidator.class })
@Documented
public @interface ValidPanchayat {

    String message() default "At least one of vcode or svid must be set.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
