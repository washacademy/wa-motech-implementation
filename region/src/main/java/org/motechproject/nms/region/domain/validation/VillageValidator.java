package org.motechproject.nms.region.domain.validation;

import org.motechproject.nms.region.domain.Panchayat;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


public class VillageValidator implements ConstraintValidator<ValidVillage, Panchayat> {

    @Override
    public void initialize(ValidVillage validVillage) {

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
