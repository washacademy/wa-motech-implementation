package org.motechproject.wa.region.domain.validation;

import org.motechproject.wa.region.domain.Panchayat;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


public class PanchayatValidator implements ConstraintValidator<ValidPanchayat, Panchayat> {

    @Override
    public void initialize(ValidPanchayat validPanchayat) {

    }

    @Override
    public boolean isValid(Panchayat panchayat, ConstraintValidatorContext constraintValidatorContext) {
        if (panchayat == null) {
            return true;
        }

        if (panchayat.getVcode() == 0 && panchayat.getSvid() == 0) {
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(constraintValidatorContext.getDefaultConstraintMessageTemplate())
                    .addNode("vcode").addConstraintViolation();
            return false;
        }

        return true;
    }
}
