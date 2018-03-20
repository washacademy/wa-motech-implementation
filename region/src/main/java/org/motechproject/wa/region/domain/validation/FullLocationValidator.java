package org.motechproject.wa.region.domain.validation;

import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.FullLocation;
import org.motechproject.wa.region.domain.Panchayat;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class FullLocationValidator implements ConstraintValidator<ValidFullLocation, FullLocation> {

    @Override
    public void initialize(ValidFullLocation validFullLocation) {

    }

    @Override
    public boolean isValid(FullLocation location, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;

        // A location hierarchy is valid if a value is set at the district level or lower and the chain
        // of locations is unbroken (with correct parent child relationships), or if no location is provided

        if (allNull(location)) {
            return true;
        }

        if (!validateLocationHierarchy(location, constraintValidatorContext)) {
            isValid = false;
        }

        return isValid;
    }

    private boolean allNull(FullLocation location) {
        return location.getState() == null && location.getDistrict() == null && // NO CHECKSTYLE Boolean expression complexity
                location.getBlock() == null &&  location.getPanchayat() == null;
    }

    private boolean validateLocationHierarchy(FullLocation location, ConstraintValidatorContext constraintValidatorContext) { // NO CHECKSTYLE Cyclomatic Complexity
        boolean isValid = true;
        boolean locationAtOrBelowDistrict = false;

        if (location.getPanchayat() != null) {
            locationAtOrBelowDistrict = true;
            Panchayat panchayat = location.getPanchayat();

            if (location.getBlock() == null) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate("Block must be set if panchayat " +
                                                                         "is provided").addConstraintViolation();
                isValid = false;
            } else if (panchayat.getBlock() == null ||
                    !panchayat.getBlock().getId().equals(location.getBlock().getId())) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate("Panchayat is not a child of " +
                                                                          "the Block").addConstraintViolation();
                isValid = false;
            }
        }

        if (location.getBlock() != null) {
            locationAtOrBelowDistrict = true;
            Block block = location.getBlock();

            if (location.getDistrict() == null) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate("District must be set if " +
                                                                  "block is provided").addConstraintViolation();
                isValid = false;
            } else
                if (block.getDistrict() == null ||
                        !block.getDistrict().getId().equals(location.getDistrict().getId())) {
                    constraintValidatorContext.disableDefaultConstraintViolation();
                    constraintValidatorContext.buildConstraintViolationWithTemplate("Block is not a child of " +
                                                                        "the District").addConstraintViolation();
                    isValid = false;
                }
        }

        if (location.getDistrict() != null) {
            locationAtOrBelowDistrict = true;
            District district = location.getDistrict();

            if (location.getState() == null) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate("State must be set if district " +
                        "is provided").addConstraintViolation();
                isValid = false;
            } else
                if (district.getState() == null ||
                        !district.getState().getId().equals(location.getState().getId())) {
                    constraintValidatorContext.disableDefaultConstraintViolation();
                    constraintValidatorContext.buildConstraintViolationWithTemplate("District is not a child " +
                                                                        "of the State").addConstraintViolation();
                    isValid = false;
                }
        }

        if (isValid && !locationAtOrBelowDistrict) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("A location at District or below " +
                                                                    "must be provided").addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }

}
