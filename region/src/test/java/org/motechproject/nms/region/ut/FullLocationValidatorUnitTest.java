package org.motechproject.nms.region.ut;

import org.junit.Before;
import org.junit.Test;

import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.Panchayat;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.domain.FullLocation;
import org.motechproject.nms.region.domain.validation.ValidFullLocation;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class FullLocationValidatorUnitTest {

    Validator validator;

    @Before
    public void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // Test No location
    @Test
    public void testNoLocation() {
        TestLocation testLocation = new TestLocation();

        Set<ConstraintViolation<TestLocation>> constraintViolations = validator.validate(testLocation);

        assertEquals(0, constraintViolations.size());
    }

    // Test only state
    @Test
    public void testOnlyState() {
        TestLocation testLocation = new TestLocation();
        testLocation.setState(new State("State", 1l));

        Set<ConstraintViolation<TestLocation>> constraintViolations = validator.validate(testLocation);

        assertEquals(1, constraintViolations.size());
        assertEquals("A location at District or below must be provided", constraintViolations.iterator().next().getMessage());
    }

    public void buildValidFullLocation(TestLocation testLocation) {
        State state = new State();
        state.setId(1L);
        District district = new District();
        district.setId(2L);
        Block block = new Block();
        block.setId(3L);
        Panchayat panchayat = new Panchayat();
        panchayat.setId(4L);

        state.getDistricts().add(district);
        district.setState(state);
        district.getBlocks().add(block);
        block.setDistrict(district);
        block.getPanchayats().add(panchayat);
        panchayat.setBlock(block);
       
        testLocation.setState(state);
        testLocation.setDistrict(district);
        testLocation.setBlock(block);
        testLocation.setPanchayat(panchayat);
    }

    // Valid FullLocation
    @Test
    public void testValidFullLocation() {
        TestLocation testLocation = new TestLocation();
        buildValidFullLocation(testLocation);

        Set<ConstraintViolation<TestLocation>> constraintViolations = validator.validate(testLocation);

        assertEquals(0, constraintViolations.size());
    }

    // Broken link in the chain tests:
    //  Test all but district
    @Test
    public void testBrokenChainNoDistrict() {
        TestLocation testLocation = new TestLocation();

        buildValidFullLocation(testLocation);
        testLocation.setDistrict(null);

        Set<ConstraintViolation<TestLocation>> constraintViolations = validator.validate(testLocation);

        assertEquals(1, constraintViolations.size());
        assertEquals("District must be set if block is provided", constraintViolations.iterator().next().getMessage());
    }

    //  Test all but block with panchayat
    @Test
    public void testBrokenChainNoBlockWithPanchayat() {
        TestLocation testLocation = new TestLocation();

        buildValidFullLocation(testLocation);
        testLocation.setBlock(null);

        Set<ConstraintViolation<TestLocation>> constraintViolations = validator.validate(testLocation);

        assertEquals(1, constraintViolations.size());
        assertEquals("Block must be set if panchayat is provided", constraintViolations.iterator().next().getMessage());
    }

    //  Test all but health block

    //  Test all but block with health block

    // Test child not in parent
    //   Test district not in state
    @Test
    public void testDistrictNotInState() {
        TestLocation testLocation = new TestLocation();
        buildValidFullLocation(testLocation);

        testLocation.getState().setDistricts(Collections.<District>emptySet());
        testLocation.getDistrict().setState(null);

        Set<ConstraintViolation<TestLocation>> constraintViolations = validator.validate(testLocation);

        assertEquals(1, constraintViolations.size());
        assertEquals("District is not a child of the State", constraintViolations.iterator().next().getMessage());
    }

    //   Test block not in district
    @Test
    public void testBlockNotInDistrict() {
        TestLocation testLocation = new TestLocation();
        buildValidFullLocation(testLocation);

        testLocation.getDistrict().setBlocks(Collections.<Block>emptyList());
        testLocation.getBlock().setDistrict(null);

        Set<ConstraintViolation<TestLocation>> constraintViolations = validator.validate(testLocation);

        assertEquals(1, constraintViolations.size());
        assertEquals("Block is not a child of the District", constraintViolations.iterator().next().getMessage());
    }

    //   Test panchayat not in block
    @Test
    public void testPanchayatNotInBlock() {
        TestLocation testLocation = new TestLocation();
        buildValidFullLocation(testLocation);

        testLocation.getBlock().setPanchayats(Collections.<Panchayat>emptyList());
        testLocation.getPanchayat().setBlock(null);

        Set<ConstraintViolation<TestLocation>> constraintViolations = validator.validate(testLocation);

        assertEquals(1, constraintViolations.size());
        assertEquals("Panchayat is not a child of the Block", constraintViolations.iterator().next().getMessage());
    }
    
}

@ValidFullLocation
class TestLocation implements FullLocation {
    private State state;
    private District district;
    private Block block;
    private Panchayat panchayat;

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public District getDistrict() {
        return district;
    }

    @Override
    public void setDistrict(District district) {
        this.district = district;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(Block block) {
        this.block = block;
    }

    @Override
    public Panchayat getPanchayat() {
        return panchayat;
    }

    @Override
    public void setPanchayat(Panchayat panchayat) {
        this.panchayat = panchayat;
    }
}