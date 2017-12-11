package org.motechproject.nms.region.ut;

import org.junit.Before;
import org.junit.Test;
import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.domain.Panchayat;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class PanchayatUnitTest {
    Validator validator;

    @Before
    public void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testNameNull() {
        Panchayat panchayat = new Panchayat();

        Set<ConstraintViolation<Panchayat>> constraintViolations = validator
                .validateProperty(panchayat, "name");

        assertEquals(1, constraintViolations.size());
        assertEquals("may not be null", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testNameSize() {
        Panchayat panchayat = new Panchayat();
        panchayat.setName("AAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBXAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBXJGDJFGEJWGFWEJHGFJWEHGFJWEGFJHEGfdewfwefwefeweeee");

        Set<ConstraintViolation<Panchayat>> constraintViolations = validator
                .validateProperty(panchayat, "name");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 250", constraintViolations.iterator().next().getMessage());

        panchayat.setName("");

        constraintViolations = validator.validateProperty(panchayat, "name");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 250", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testRegionalNameToLong() {
        Panchayat panchayat = new Panchayat();
        panchayat.setRegionalName("AAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBXAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBXJGDJFGEJWGFWEJHGFJWEHGFJWEGFJHEGfdewfwefwefeweeee");

        Set<ConstraintViolation<Panchayat>> constraintViolations = validator
                .validateProperty(panchayat, "regionalName");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 250", constraintViolations.iterator().next().getMessage());

        panchayat.setRegionalName("");

        constraintViolations = validator.validateProperty(panchayat, "regionalName");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 250", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testTalukaNull() {
        Panchayat panchayat = new Panchayat();

        Set<ConstraintViolation<Panchayat>> constraintViolations = validator
                .validateProperty(panchayat, "block");

        assertEquals(1, constraintViolations.size());
        assertEquals("may not be null", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testVillageCodeInvalid() {
        Block block = new Block();
        block.setName("Block 1");
        block.setCode("0004");

        Panchayat panchayat = new Panchayat();
        panchayat.setName("Panchayat");
        panchayat.setRegionalName("Panchayat");
        panchayat.setBlock(block);

        Set<ConstraintViolation<Panchayat>> constraintViolations = validator.validate(panchayat);

        assertEquals(2, constraintViolations.size()); // We get the same message twice...
        for (ConstraintViolation<Panchayat> constraintViolation : constraintViolations) {
            assertEquals("At least one of vcode or svid must be set.", constraintViolation.getMessage());
        }
    }

    @Test
    public void testVillageCodeValid() {
        Block block = new Block();
        block.setName("Block 1");
        block.setCode("0004");

        Panchayat panchayat = new Panchayat();
        panchayat.setName("Panchayat");
        panchayat.setRegionalName("Panchayat");
        panchayat.setBlock(block);

        panchayat.setVcode(1L);

        Set<ConstraintViolation<Panchayat>> constraintViolations = validator.validate(panchayat);

        assertEquals(0, constraintViolations.size());

        panchayat.setVcode(0);
        panchayat.setSvid(1L);

        constraintViolations = validator.validate(panchayat);

        assertEquals(0, constraintViolations.size());

        panchayat.setVcode(1L);
        panchayat.setSvid(1L);

        constraintViolations = validator.validate(panchayat);

        assertEquals(0, constraintViolations.size());
    }
}
