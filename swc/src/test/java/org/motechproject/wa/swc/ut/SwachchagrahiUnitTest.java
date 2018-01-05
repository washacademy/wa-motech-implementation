package org.motechproject.wa.swc.ut;

import org.junit.Before;
import org.junit.Test;
import org.motechproject.wa.swc.domain.Swachchagrahi;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SwachchagrahiUnitTest {
    Validator validator;

    @Before
    public void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testContactNumberTooShort() {
        Swachchagrahi swc = new Swachchagrahi(111111111L);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator
                .validateProperty(swc, "contactNumber");

        assertEquals(1, constraintViolations.size());
        assertEquals("contactNumber must be 10 digits", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testContactNumberTooLong() {
        Swachchagrahi swc = new Swachchagrahi(11111111111L);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator
                .validateProperty(swc, "contactNumber");

        assertEquals(1, constraintViolations.size());
        assertEquals("contactNumber must be 10 digits", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testContactNumberValid() {
        Swachchagrahi swc = new Swachchagrahi(1111111111L);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator
                .validateProperty(swc, "contactNumber");

        assertEquals(0, constraintViolations.size());
    }
}
