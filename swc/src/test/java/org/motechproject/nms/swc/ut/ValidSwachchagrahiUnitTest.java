package org.motechproject.nms.swc.ut;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.motechproject.nms.swc.domain.Swachchagrahi;
import org.motechproject.nms.swc.domain.SwachchagrahiStatus;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.State;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ValidSwachchagrahiUnitTest {

    Validator validator;
    State state;
    District district;

    @Before
    public void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        state = new State();
        state.setId(1L);
        district = new District();
        district.setId(2L);

        state.getDistricts().add(district);
        district.setState(state);
    }

    // Test Valid
    @Test
    public void testActiveValid() {
        Swachchagrahi swc = new Swachchagrahi(1111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swc.setState(state);
        swc.setDistrict(district);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(swc);

        assertEquals(0, constraintViolations.size());
    }

    // Test Active no state
    @Test
    public void testActiveNoStateInValid() {
        Swachchagrahi swc = new Swachchagrahi(1111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swc.setDistrict(district);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(swc);

        // One violation is Active without state and district the other is from @FullLocatinValidator
        assertEquals(2, constraintViolations.size());
    }

    // Test Active no district
    @Test
    public void testActiveNoDistrictInValid() {
        Swachchagrahi swc = new Swachchagrahi(1111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swc.setState(state);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(swc);

        // One violation is Active without state and district the other is from @FullLocatinValidator
        assertEquals(2, constraintViolations.size());
    }

    // Test Active no district and state
    @Test
    public void testActiveNoDistrictNoStateInValid() {
        Swachchagrahi swc = new Swachchagrahi(1111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(swc);

        assertEquals(1, constraintViolations.size());
        assertEquals("Active SWCs must have Location set.", constraintViolations.iterator().next().getMessage());
    }

    // Test ANONYMOUS no location
    @Test
    public void testANONYMOUSValid() {
        Swachchagrahi swc = new Swachchagrahi(1111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(swc);

        assertEquals(0, constraintViolations.size());
    }

    // Test INACTIVE no location
    @Test
    public void testINACTIVEValid() {
        Swachchagrahi swc = new Swachchagrahi(1111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(swc);

        assertEquals(0, constraintViolations.size());
    }

    // Test INVALID no location
    @Test
    @Ignore
    public void testINVALIDValid() {
        Swachchagrahi swc = new Swachchagrahi(1111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(swc);

        assertEquals(1, constraintViolations.size());
    }
}
