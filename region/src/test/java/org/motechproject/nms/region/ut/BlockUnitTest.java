package org.motechproject.nms.region.ut;

import org.junit.Before;
import org.junit.Test;
import org.motechproject.nms.region.domain.Block;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Created by rob on 5/4/15.
 */
public class BlockUnitTest {
    Validator validator;

    @Before
    public void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testNameNull() {
        Block block = new Block();

        Set<ConstraintViolation<Block>> constraintViolations = validator
                .validateProperty(block, "name");

        assertEquals(1, constraintViolations.size());
        assertEquals("may not be null", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testNameSize() {
        Block block = new Block();
        block.setName("AAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBXAAAAsAAsssssssAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAA");

        Set<ConstraintViolation<Block>> constraintViolations = validator
                .validateProperty(block, "name");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 150", constraintViolations.iterator().next().getMessage());

        block.setName("");

        constraintViolations = validator.validateProperty(block, "name");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 150", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testRegionalNameSize() {
        Block block = new Block();
        block.setRegionalName("AAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBXAAAAsAAsssssssAAAABBBBBBBBBBAAAAAAAAAABBBBBBBBBBAA");

        Set<ConstraintViolation<Block>> constraintViolations = validator
                .validateProperty(block, "regionalName");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 150", constraintViolations.iterator().next().getMessage());

        block.setRegionalName("");

        constraintViolations = validator.validateProperty(block, "regionalName");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 150", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testCodeNull() {
        Block block = new Block();

        Set<ConstraintViolation<Block>> constraintViolations = validator
                .validateProperty(block, "code");

        assertEquals(1, constraintViolations.size());
        assertEquals("may not be null", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testCodeSize() {
        Block block = new Block();
        block.setCode((long)00000001);

        Set<ConstraintViolation<Block>> constraintViolations = validator
                .validateProperty(block, "code");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 7", constraintViolations.iterator().next().getMessage());

        block.setCode(null);

        constraintViolations = validator.validateProperty(block, "code");

        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 7", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testDistrictNull() {
        Block block = new Block();

        Set<ConstraintViolation<Block>> constraintViolations = validator
                .validateProperty(block, "district");

        assertEquals(1, constraintViolations.size());
        assertEquals("may not be null", constraintViolations.iterator().next().getMessage());
    }
}
