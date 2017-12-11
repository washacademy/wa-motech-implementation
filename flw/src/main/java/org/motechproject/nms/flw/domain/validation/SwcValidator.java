package org.motechproject.nms.flw.domain.validation;

import org.motechproject.nms.flw.domain.Swachchagrahi;
import org.motechproject.nms.flw.domain.SwachchagrahiStatus;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SwcValidator implements ConstraintValidator<ValidSwachchagrahi, Swachchagrahi> {

    @Override
    public void initialize(ValidSwachchagrahi validSwachchagrahi) {

    }

    @Override
    public boolean isValid(Swachchagrahi swc, ConstraintValidatorContext constraintValidatorContext) {
        if (swc == null) {
            return true;
        }

        // An active FLW must have a state and district
        if (swc.getCourseStatus() == SwachchagrahiStatus.ACTIVE &&
                (swc.getState() == null || swc.getDistrict() == null)) {
            return false;
        }

        // Contact Number must be set unless the status is invalid
        if (swc.getContactNumber() == null && swc.getCourseStatus() != SwachchagrahiStatus.INVALID) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(
                    String.format("Contact Number can not be null for SWC with status %s", swc.getCourseStatus()))
                            .addConstraintViolation();
            return false;
        }

        // invalid FLWs can't have a contact number
        if (swc.getCourseStatus() == SwachchagrahiStatus.INVALID && swc.getContactNumber() != null) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(
                    String.format("Invalid SWCs can not have a contact number set.", swc.getCourseStatus()))
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
