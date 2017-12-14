package org.motechproject.nms.testing.it.region;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.motechproject.nms.region.domain.*;
import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.repository.BlockDataService;
import org.motechproject.nms.region.repository.PanchayatDataService;
import org.motechproject.nms.testing.service.TestingService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class LocationServiceBundleIT extends BasePaxIT {
    @Inject
    StateDataService stateDataService;

    @Inject
    DistrictDataService districtDataService;

    @Inject
    BlockDataService blockDataService;

    @Inject
    PanchayatDataService panchayatDataService;


    @Inject
    TestingService testingService;

    @Inject
    PlatformTransactionManager transactionManager;

    State state;
    District district;
    Block block;
    Panchayat panchayat;



    @Before
    public void doTheNeedful() {
        testingService.clearDatabase();

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());


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

        state = new State();
        state.setName("State 1");
        state.setCode(1L);
        state.getDistricts().add(district);
        transactionManager.commit(status);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateStateNoName() throws Exception {
        state.setName(null);

        stateDataService.create(state);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateDistrictNoName() throws Exception {
        district.setName(null);

        districtDataService.create(district);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateTalukaNoName() throws Exception {
        block.setName(null);

        blockDataService.create(block);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateVillageNoName() throws Exception {
        panchayat.setName(null);

        // Panchayat is the leaf, I have to create something connected to the object graph so I save the
        // block (it's parent) instead
        blockDataService.create(block);
    }

    @Test (expected = ConstraintViolationException.class)
    @Ignore // Remove once https://applab.atlassian.net/browse/MOTECH-1691 is resolved
    public void testCreateVillageNoCode() throws Exception {
        panchayat.setVcode(0);
        panchayat.setSvid(0);

        // Panchayat is the leaf, I have to create something connected to the object graph so I save the
        // block (it's parent) instead
        stateDataService.create(state);
    }


    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testDistrictsWithSameCodeDifferentStates() throws Exception {
        stateDataService.create(state);
        districtDataService.create(district);

        State otherState = new State();
        otherState.setName("State 2");
        otherState.setCode(2L);
        stateDataService.create(otherState);

        District otherDistrict = new District();
        otherDistrict.setName("District 2");
        otherDistrict.setRegionalName("District 2");
        otherDistrict.setCode(1L);
        otherDistrict.setState(otherState);
        districtDataService.create(otherDistrict);
    }


    @Test
    public void testDistrictsWithSameCodeSameStates() throws Exception {
        stateDataService.create(state);
        districtDataService.create(district);

        District otherDistrict = new District();
        otherDistrict.setName("District 2");
        otherDistrict.setRegionalName("District 2");
        otherDistrict.setCode(1L);
        otherDistrict.setState(state);

        exception.expect(javax.jdo.JDODataStoreException.class);
        districtDataService.create(otherDistrict);
    }


    @Test
    public void testVillagesWithSameCodeDifferentTalukas() throws Exception {
        stateDataService.create(state);
        districtDataService.create(district);
        blockDataService.create(block);
        panchayatDataService.create(panchayat);

        Block otherBlock = new Block();
        otherBlock.setName("Block 2");
        otherBlock.setRegionalName("Block 2");
        otherBlock.setDistrict(district);
        otherBlock.setIdentity(2);
        otherBlock.setCode("0005");
        blockDataService.create(otherBlock);

        Panchayat otherPanchayat = new Panchayat();
        otherPanchayat.setName("Panchayat 2");
        otherPanchayat.setRegionalName("Panchayat 2");
        otherPanchayat.setVcode(1L);
        otherPanchayat.setBlock(otherBlock);
        panchayatDataService.create(otherPanchayat);
    }


    @Test
    public void testVillagesWithSameCodeSameTalukas() throws Exception {
        stateDataService.create(state);
        districtDataService.create(district);
        blockDataService.create(block);
        panchayatDataService.create(panchayat);

        Panchayat otherPanchayat = new Panchayat();
        otherPanchayat.setName("Panchayat 2");
        otherPanchayat.setRegionalName("Panchayat 2");
        otherPanchayat.setVcode(1L);
        otherPanchayat.setBlock(block);
        exception.expect(javax.jdo.JDODataStoreException.class);
        panchayatDataService.create(otherPanchayat);
    }

    // Single Block Single HB, should find it
    // State -> District -> Block -> HealthBlock(1)
//    @Test
//    public void testFindHealthBlockByTalukaAndCode1() {
//        stateDataService.create(state);
//        districtDataService.create(district);
//        Block t = blockDataService.create(block);
//        healthBlockDataService.create(healthBlock);
//
//        HealthBlock hb = healthBlockService.findByTalukaAndCode(t, 1L);
//        assertNotNull(hb);
//        assertEquals(hb.getCode(), healthBlock.getCode());
//    }
//
//    // Multiple, lookup by t(1) and HB(2), should find it
//    // State -> District -> Block(1) -> HealthBlock(1)
//    //                   -> Block(2) -> HealthBlock(2)
//    @Test
//    public void testFindHealthBlockByTalukaAndCode2() {
//        stateDataService.create(state);
//        district = districtDataService.create(district);
//        Block t = blockDataService.create(block);
//        healthBlockDataService.create(healthBlock);
//
//        HealthBlock healthBlock2 = new HealthBlock();
//        healthBlock2.setName("Health Block 2");
//        healthBlock2.setRegionalName("Health Block 2");
//        healthBlock2.setHq("Health Block 2 HQ");
//        healthBlock2.setCode(2L);
//
//        Block block2 = new Block();
//        block2.setName("Block 2");
//        block2.setRegionalName("Block 2");
//        block2.setIdentity(2);
//        block2.setCode("0005");
//        block2.setDistrict(district);
//        block2.getHealthBlocks().add(healthBlock2);
//
//        block2 = blockDataService.create(block2);
//        healthBlockDataService.create(healthBlock2);
//
//        HealthBlock hb = healthBlockService.findByTalukaAndCode(t, 2L);
//        assertNotNull(hb);
//        assertEquals(hb.getCode(), healthBlock2.getCode());
//
//        hb = healthBlockService.findByTalukaAndCode(t, 1L);
//        assertNotNull(hb);
//        assertEquals(hb.getCode(), healthBlock.getCode());
//
//        hb = healthBlockService.findByTalukaAndCode(block2, 1L);
//        assertNotNull(hb);
//        assertEquals(hb.getCode(), healthBlock.getCode());
//
//        hb = healthBlockService.findByTalukaAndCode(block2, 2L);
//        assertNotNull(hb);
//        assertEquals(hb.getCode(), healthBlock2.getCode());
//    }
//
//    // Two HB in Single Block, lookup by t(1) hb(1), should find it
//    // State -> District -> Block -> HealthBlock(1)
//    //                             -> HealthBlock(2)
//    @Test
//    public void testFindHealthBlockByTalukaAndCode3() {
//        stateDataService.create(state);
//        districtDataService.create(district);
//        Block t = blockDataService.create(block);
//        healthBlockDataService.create(healthBlock);
//
//        HealthBlock healthBlock2 = new HealthBlock();
//        healthBlock2.setName("Health Block 2");
//        healthBlock2.setRegionalName("Health Block 2");
//        healthBlock2.setHq("Health Block 2 HQ");
//        healthBlock2.setCode(2L);
//        healthBlock2.setBlock(t);
//
//        healthBlockDataService.create(healthBlock2);
//
//        HealthBlock hb = healthBlockService.findByTalukaAndCode(t, 1L);
//        assertNotNull(hb);
//        assertEquals(hb.getCode(), healthBlock.getCode());
//
//        hb = healthBlockService.findByTalukaAndCode(t, 2L);
//        assertNotNull(hb);
//        assertEquals(hb.getCode(), healthBlock2.getCode());
//    }
//    // Multiple, lookup by t(1), hb(2) should not find it
//    // State(1) -> District -> Block(1) -> HealthBlock(1)
//    // State(2) -> District -> Block(2) -> HealthBlock(2)
//    @Test
//    public void testFindHealthBlockByTalukaAndCode4() {
//        stateDataService.create(state);
//        districtDataService.create(district);
//        Block t = blockDataService.create(block);
//        healthBlockDataService.create(healthBlock);
//
//        HealthBlock healthBlock2 = new HealthBlock();
//        healthBlock2.setName("Health Block 2");
//        healthBlock2.setRegionalName("Health Block 2");
//        healthBlock2.setHq("Health Block 2 HQ");
//        healthBlock2.setCode(2L);
//
//        Block block2 = new Block();
//        block2.setName("Block 2");
//        block2.setRegionalName("Block 2");
//        block2.setIdentity(2);
//        block2.setCode("0005");
//        block2.getHealthBlocks().add(healthBlock2);
//
//        District district2 = new District();
//        district2.setName("District 2");
//        district2.setRegionalName("District 2");
//        district2.setCode(2L);
//        district2.getBlocks().add(block2);
//
//        State state2 = new State();
//        state2.setName("State 2");
//        state2.setCode(2L);
//        state2.getDistricts().add(district2);
//
//        stateDataService.create(state2);
//        districtDataService.create(district2);
//        Block t2 = blockDataService.create(block2);
//        healthBlockDataService.create(healthBlock2);
//
//        HealthBlock hb = healthBlockService.findByTalukaAndCode(t, 2L);
//        assertNull(hb);
//
//        hb = healthBlockService.findByTalukaAndCode(t2, 1L);
//        assertNull(hb);
//    }

    @Test
    @Ignore // TODO: Remove once https://applab.atlassian.net/browse/MOTECH-1678 is resolved
    public void testValidCreate() throws Exception {
        stateDataService.create(state);

        State newState = stateDataService.findByCode(1L);
        assertNotNull(newState);
        Assert.assertEquals(state, newState);

        Set<District> districtList = newState.getDistricts();
        assertEquals(1, districtList.size());
        assertTrue(districtList.contains(district));

        List<Block> blockList = districtList.iterator().next().getBlocks();
        assertEquals(1, blockList.size());
        assertTrue(blockList.contains(block));

        List<Panchayat> panchayatList = blockList.get(0).getPanchayats();
        assertEquals(1, panchayatList.size());
        assertTrue(panchayatList.contains(panchayat));

    }
}
