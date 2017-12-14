package org.motechproject.nms.testing.it.region;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.nms.region.domain.*;
import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.repository.CircleDataService;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.repository.BlockDataService;
import org.motechproject.nms.region.repository.PanchayatDataService;
import org.motechproject.nms.region.service.CircleService;
import org.motechproject.nms.region.service.StateService;
import org.motechproject.nms.testing.service.TestingService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(value = MotechNativeTestContainerFactory.class)
public class CircleServiceBundleIT extends BasePaxIT {

    @Inject
    TestingService testingService;

    @Inject
    CircleDataService circleDataService;

    @Inject
    CircleService circleService;

    @Inject
    StateDataService stateDataService;

    @Inject
    StateService stateService;

    @Inject
    DistrictDataService districtDataService;

    @Inject
    BlockDataService blockDataService;

    @Inject
    PanchayatDataService panchayatDataService;


    District district;
    Block block;
    Panchayat panchayat;
    // Circle 1           -> State 1
    // Circle 2           -> State 2, State 3
    // Circle 3, Circle 4 -> State 4
    private void setupData() {
        testingService.clearDatabase();

        final Circle circle1 = new Circle();
        circle1.setName("Circle 1");

        final Circle circle2 = new Circle();
        circle2.setName("Circle 2");

        final Circle circle3 = new Circle();
        circle3.setName("Circle 3");

        final Circle circle4 = new Circle();
        circle4.setName("Circle 4");

        circleDataService.create(circle1);
        circleDataService.create(circle2);
        circleDataService.create(circle3);
        circleDataService.create(circle4);

        panchayat = new Panchayat();
        panchayat.setName("Panchayat 1");
        panchayat.setRegionalName("Panchayat 1");
        panchayat.setVcode(1L);

        block = new Block();
        block.setName("Block 1");
        block.setRegionalName("Block 1");
        block.setIdentity(1);
        block.setCode("0004");
        block.getPanchayats().add(panchayat);

        district = new District();
        district.setName("District 1");
        district.setRegionalName("District 1");
        district.setCode(1L);
        district.getBlocks().add(block);
        district.setCircle(circle1);

        final State state1 = new State();
        state1.setName("State 1");
        state1.setCode(1L);
        state1.getDistricts().add(district);

        district = new District();
        district.setName("District 2");
        district.setRegionalName("District 2");
        district.setCode(2L);
        district.setCircle(circle2);

        final State state2 = new State();
        state2.setName("State 2");
        state2.setCode(2L);
        state2.getDistricts().add(district);

        district = new District();
        district.setName("District 3");
        district.setRegionalName("District 3");
        district.setCode(3L);
        district.setCircle(circle2);

        final State state3 = new State();
        state3.setName("State 3");
        state3.setCode(3L);
        state3.getDistricts().add(district);

        district = new District();
        district.setName("District 4");
        district.setRegionalName("District 4");
        district.setCode(4L);

        District district2 = new District();
        district2.setName("District 4.1");
        district2.setRegionalName("District 4.1");
        district2.setCode(5L);

        final State state4 = new State();
        state4.setName("State 4");
        state4.setCode(4L);
        state4.getDistricts().addAll(Arrays.asList(district, district2));

        stateDataService.create(state1);
        stateDataService.create(state2);
        stateDataService.create(state3);
        stateDataService.create(state4);
    }

    @Test
    public void testCircleSingleState() throws Exception {
        setupData();

        Circle circle = circleDataService.findByName("Circle 1");
        assertNotNull(circle);

        assertEquals(1, stateService.getAllInCircle(circle).size());

        State state = stateService.getAllInCircle(circle).iterator().next();
        assertEquals("State 1", state.getName());

        state = stateDataService.findByCode(1L);
        assertNotNull(state);

        assertEquals(1, circleService.getAllInState(state).size());

        circle = circleService.getAllInState(state).iterator().next();
        assertEquals("Circle 1", circle.getName());
    }

    @Test
    @Ignore
    public void testCircleMultipleStates() throws Exception {
        setupData();

        Circle circle = circleDataService.findByName("Circle 2");
        assertNotNull(circle);

        assertEquals(2, stateService.getAllInCircle(circle).size());

        State state = stateDataService.findByCode(2L);
        assertNotNull(state);

        assertEquals(1, circleService.getAllInState(state).size());

        circle = circleService.getAllInState(state).iterator().next();
        assertEquals("Circle 2", circle.getName());

        state = stateDataService.findByCode(3L);
        assertNotNull(state);

        assertEquals(1, circleService.getAllInState(state).size());

        circle = circleService.getAllInState(state).iterator().next();
        assertEquals("Circle 2", circle.getName());
    }

    @Ignore
    public void testMultipleCirclesSingleState() throws Exception {
        setupData();

        Circle circle = circleDataService.findByName("Circle 3");
        assertNotNull(circle);

        assertEquals(1, stateService.getAllInCircle(circle).size());

        State state = stateService.getAllInCircle(circle).iterator().next();
        assertEquals("State 4", state.getName());

        circle = circleDataService.findByName("Circle 4");
        assertNotNull(circle);

        assertEquals(1, stateService.getAllInCircle(circle).size());

        state = stateService.getAllInCircle(circle).iterator().next();
        assertEquals("State 4", state.getName());

        state = stateDataService.findByCode(4L);
        assertNotNull(state);

        assertEquals(2, circleService.getAllInState(state).size());
    }

}
