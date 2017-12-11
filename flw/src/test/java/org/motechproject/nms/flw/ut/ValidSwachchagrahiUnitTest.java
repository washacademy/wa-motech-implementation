package org.motechproject.nms.flw.ut;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.motechproject.nms.flw.domain.Swachchagrahi;
import org.motechproject.nms.flw.domain.SwachchagrahiStatus;
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
        Swachchagrahi flw = new Swachchagrahi(1111111111L);
        flw.setStatus(SwachchagrahiStatus.ACTIVE);
        flw.setState(state);
        flw.setDistrict(district);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(flw);

        assertEquals(0, constraintViolations.size());
    }

    // Test Active no state
    @Test
    public void testActiveNoStateInValid() {
        Swachchagrahi flw = new Swachchagrahi(1111111111L);
        flw.setStatus(SwachchagrahiStatus.ACTIVE);
        flw.setDistrict(district);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(flw);

        // One violation is Active without state and district the other is from @FullLocatinValidator
        assertEquals(2, constraintViolations.size());
    }

    // Test Active no district
    @Test
    public void testActiveNoDistrictInValid() {
        Swachchagrahi flw = new Swachchagrahi(1111111111L);
        flw.setStatus(SwachchagrahiStatus.ACTIVE);
        flw.setState(state);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(flw);

        // One violation is Active without state and district the other is from @FullLocatinValidator
        assertEquals(2, constraintViolations.size());
    }

    // Test Active no district and state
    @Test
    public void testActiveNoDistrictNoStateInValid() {
        Swachchagrahi flw = new Swachchagrahi(1111111111L);
        flw.setStatus(SwachchagrahiStatus.ACTIVE);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(flw);

        assertEquals(1, constraintViolations.size());
        assertEquals("Active FLWs must have State and District set.", constraintViolations.iterator().next().getMessage());
    }

    // Test ANONYMOUS no location
    @Test
    public void testANONYMOUSValid() {
        Swachchagrahi flw = new Swachchagrahi(1111111111L);
        flw.setStatus(SwachchagrahiStatus.ANONYMOUS);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(flw);

        assertEquals(0, constraintViolations.size());
    }

    // Test INACTIVE no location
    @Test
    public void testINACTIVEValid() {
        Swachchagrahi flw = new Swachchagrahi(1111111111L);
        flw.setStatus(SwachchagrahiStatus.INACTIVE);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(flw);

        assertEquals(0, constraintViolations.size());
    }

    // Test INVALID no location
    @Test
    @Ignore
    public void testINVALIDValid() {
        Swachchagrahi flw = new Swachchagrahi(1111111111L);
        flw.setStatus(SwachchagrahiStatus.INVALID);

        Set<ConstraintViolation<Swachchagrahi>> constraintViolations = validator.validate(flw);

        assertEquals(1, constraintViolations.size());
    }
}
