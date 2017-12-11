package org.motechproject.nms.testing.it.flw;

import org.joda.time.DateTime;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.mds.ex.JdoListenerInvocationException;
import org.motechproject.nms.flw.domain.*;
import org.motechproject.nms.flw.repository.SwcStatusUpdateAuditDataService;
import org.motechproject.nms.flw.repository.SwcDataService;
import org.motechproject.nms.flw.repository.WhitelistEntryDataService;
import org.motechproject.nms.flw.repository.WhitelistStateDataService;
import org.motechproject.nms.flw.service.SwcSettingsService;
import org.motechproject.nms.flw.service.SwcService;
import org.motechproject.nms.flw.service.ServiceUsageService;
import org.motechproject.nms.region.domain.Circle;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.Language;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.repository.CircleDataService;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.LanguageDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.service.DistrictService;
import org.motechproject.nms.region.service.LanguageService;
import org.motechproject.nms.testing.service.TestingService;
import org.motechproject.server.config.SettingsFacade;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Verify that SwcService present, functional.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class FrontLineWorkerServiceBundleIT extends BasePaxIT {

    @Inject
    SwcDataService swcDataService;
    @Inject
    SwcService swcService;
    @Inject
    ServiceUsageService serviceUsageService;
    @Inject
    DistrictDataService districtDataService;
    @Inject
    DistrictService districtService;
    @Inject
    LanguageDataService languageDataService;
    @Inject
    LanguageService languageService;
    @Inject
    StateDataService stateDataService;
    @Inject
    CircleDataService circleDataService;
    @Inject
    WhitelistEntryDataService whitelistEntryDataService;
    @Inject
    WhitelistStateDataService whitelistStateDataService;
    @Inject
    TestingService testingService;
    @Inject
    SwcStatusUpdateAuditDataService swcStatusUpdateAuditDataService;
    @Inject
    PlatformTransactionManager transactionManager;


    private State sampleState;

    private static final String WEEKS_TO_KEEP_INVALID_SWCS = "swc.weeks_to_keep_invalid_swcs";

    private static final String FLW_PURGE_EVENT_SUBJECT = "nms.swc.purge_invalid_flw";

    @Inject
    private SwcSettingsService swcSettingsService;

    private SettingsFacade settingsFacade;

    @Inject
    private EventRelay eventRelay;

    String oldWeeksToKeepInvalidFLWs;

    @Before
    public void doTheNeedful() {
        testingService.clearDatabase();

        settingsFacade = swcSettingsService.getSettingsFacade();
        oldWeeksToKeepInvalidFLWs = settingsFacade
                .getProperty(WEEKS_TO_KEEP_INVALID_SWCS);
    }

    @After
    public void restore() {
        settingsFacade.setProperty(WEEKS_TO_KEEP_INVALID_SWCS,
                oldWeeksToKeepInvalidFLWs);
    }

    private void createLanguageLocationData() {
        Language ta = languageDataService.create(new Language("50", "tamil"));

        District district = new District();
        district.setName("District 1");
        district.setRegionalName("District 1");
        district.setLanguage(ta);
        district.setCode(1L);

        State state = new State();
        state.setName("State 1");
        state.setCode(1L);
        state.getDistricts().add(district);
        sampleState = stateDataService.create(state);

        Circle circle = new Circle("AA");
        circle.setDefaultLanguage(ta);
        circleDataService.create(circle);
    }

    @Test
    public void testFrontLineWorkerServicePresent() throws Exception {
        assertNotNull(swcService);
    }

    @Test
    @Ignore
    public void testPurgeOldInvalidFrontLineWorkers() {
        // FLW1 & 2 Should be purged, the others should remain

        Swachchagrahi flw1 = new Swachchagrahi("Test Worker", 1111111110L);
        flw1.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw1);
        flw1.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw1.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(flw1);

        Swachchagrahi flw2 = new Swachchagrahi("Test Worker", 1111111111L);
        flw2.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw2);
        flw2.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw2.setInvalidationDate(new DateTime().now().minusWeeks(6).minusDays(1));
        swcService.update(flw2);

        Swachchagrahi flw3 = new Swachchagrahi("Test Worker", 1111111112L);
        flw3.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw3);
        flw3.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw3.setInvalidationDate(new DateTime().now().minusWeeks(6));
        swcService.update(flw3);

        Swachchagrahi flw4 = new Swachchagrahi("Test Worker", 2111111111L);
        flw4.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw4);
        flw4.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(flw4);

        Swachchagrahi flw5 = new Swachchagrahi("Test Worker", 2111111112L);
        flw5.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw5);
        flw5.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(flw5);

        Swachchagrahi flw6 = new Swachchagrahi("Test Worker", 2111111113L);
        flw6.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw6);

        List<Swachchagrahi> records = swcService.getRecords();
        assertEquals(6, records.size());
        assertTrue(records.contains(flw1));
        assertTrue(records.contains(flw2));
        assertTrue(records.contains(flw3));
        assertTrue(records.contains(flw4));
        assertTrue(records.contains(flw5));
        assertTrue(records.contains(flw6));

        swcService.purgeOldInvalidSWCs(new MotechEvent());
        records = swcService.getRecords();
        assertEquals(4, records.size());
        assertFalse(records.contains(flw1));
        assertFalse(records.contains(flw2));
        assertTrue(records.contains(flw3));
        assertTrue(records.contains(flw4));
        assertTrue(records.contains(flw5));
        assertTrue(records.contains(flw6));
    }

    @Test
    public void testFrontLineWorkerService() throws Exception {
        Swachchagrahi flw = new Swachchagrahi("Test Worker", 1111111111L);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);

        Swachchagrahi otherFlw = swcService.getByContactNumber(1111111111L);
        assertNotNull(otherFlw);

        Swachchagrahi record = swcService.getByContactNumber(flw.getContactNumber());
        assertEquals(flw, record);

        List<Swachchagrahi> records = swcService.getRecords();
        assertTrue(records.contains(flw));

        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(flw);
        swcService.delete(flw);
        record = swcService.getByContactNumber(flw.getContactNumber());
        assertNull(record);
    }

    /**
     * NMS_FT_515 : To verify that status of Active flw is set to "Invalid" successfully
     */
    @Test
    public void testFrontLineWorkerUpdate() {
        createLanguageLocationData();

        District district = districtService.findByStateAndCode(sampleState, 1L);
        Language language = languageService.getForCode("50");

        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);
        flw = swcService.getByContactNumber(2111111111L);

        assertEquals(SwachchagrahiStatus.ANONYMOUS, flw.getCourseStatus());

        flw.setState(sampleState);
        flw.setDistrict(district);
        flw.setName("Frank Huster");
//        flw.setLanguage(language);

        swcService.update(flw);
        flw = swcService.getByContactNumber(2111111111L);
        assertEquals(SwachchagrahiStatus.ACTIVE, flw.getCourseStatus());

        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(flw);
        flw = swcService.getById(flw.getId());
        assertEquals(SwachchagrahiStatus.INVALID, flw.getCourseStatus());
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testDeleteNonInvalidFrontLineWorker() {
        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);
        flw = swcService.getByContactNumber(2111111111L);

        assertEquals(SwachchagrahiStatus.ANONYMOUS, flw.getCourseStatus());

        exception.expect(JdoListenerInvocationException.class);
        swcService.delete(flw);
    }

    @Test
    public void testDeleteRecentInvalidFrontLineWorker() {
        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);

        flw = swcService.getByContactNumber(2111111111L);
        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(flw);

        flw = swcService.getById(flw.getId());
        assertEquals(SwachchagrahiStatus.INVALID, flw.getCourseStatus());

        exception.expect(JdoListenerInvocationException.class);
        swcService.delete(flw);
    }

    @Test
    public void testDeleteOldInvalidFrontLineWorker() {
        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);

        flw = swcService.getByContactNumber(2111111111L);
        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(flw);

        flw = swcService.getById(flw.getId());
        assertEquals(SwachchagrahiStatus.INVALID, flw.getCourseStatus());

        swcService.delete(flw);
    }

    @Test
    public void testNewAshaTakingOldResignationNumber() {
        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);
        flw = swcService.getByContactNumber(2111111111L);
        flw.setJobStatus(SwcJobStatus.INACTIVE);
        swcService.update(flw);
        flw = swcService.getByContactNumber(2111111111L);
        assertNull(flw);

        flw = new Swachchagrahi("New Asha", 2111111111L);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);
        flw = swcService.getByContactNumber(2111111111L);
        assertNotNull(flw);

        assertEquals("New Asha", flw.getName());
        List<Swachchagrahi> swachchagrahis = swcDataService.retrieveAll();
        assertEquals(2, swachchagrahis.size());
    }

    /**
     * To disable automatic deletion of all records of beneficiary which were
     * marked invalid 6 weeks ago.
     *
     * @throws InterruptedException
     */
    // TODO https://applab.atlassian.net/browse/NMS-257
    @Test
    public void verifyFT548() throws InterruptedException {
        Map<String, Object> eventParams = new HashMap<>();
        MotechEvent motechEvent = new MotechEvent(FLW_PURGE_EVENT_SUBJECT,
                eventParams);

        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);
        flw = swcService.getByContactNumber(2111111111L);
        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw.setInvalidationDate(DateTime.now().minusWeeks(7));
        swcService.update(flw);

        //call purge event
        swcService.purgeOldInvalidSWCs(motechEvent);

        // assert flW deleted
        flw = swcService.getByContactNumber(2111111111L);
        assertNull(flw);

        // change configuration to disable deletion by setting weeks to large value
        settingsFacade.setProperty(WEEKS_TO_KEEP_INVALID_SWCS, "1000");

        // add new invalidated flw
        flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setSwcId("SwcId");
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);
        flw = swcService.getByContactNumber(2111111111L);
        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        // set invalid date to 2 years back
        flw.setInvalidationDate(DateTime.now().minusYears(2));
        swcService.update(flw);

        //call purge event
        swcService.purgeOldInvalidSWCs(motechEvent);

        // assert flW not deleted
        flw = swcService.getBySwcId("SwcId");
        assertNotNull(flw);
    }

    /**
     * To verify that status of Anonymous flw is set to "Invalid" successfully
     */
    @Test
    public void verifyFT514() {
        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);

        flw = swcService.getByContactNumber(2111111111L);
        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(flw);

        flw = swcService.getById(flw.getId());
        assertEquals(SwachchagrahiStatus.INVALID, flw.getCourseStatus());
        assertNull(flw.getContactNumber());
    }

    /**
     * To verify that status of Inactive flw is set to "Invalid" successfully
     */
    @Test
    public void verifyFT516() {
        createLanguageLocationData();

        District district = districtService.findByStateAndCode(sampleState, 1L);
        Language language = languageService.getForCode("50");
        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setState(sampleState);
        flw.setDistrict(district);
//        flw.setLanguage(language);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);

        flw = swcService.getByContactNumber(2111111111L);
        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(flw);

        flw = swcService.getById(flw.getId());
        assertEquals(SwachchagrahiStatus.INVALID, flw.getCourseStatus());
        assertNull(flw.getContactNumber());
    }

    /**
     * To verify that status of "Active" flw to "Invalid" and
     * the status of "Anonymous" flw to "Active" is audited properly
     */
    @Test
    @Ignore
    public void verifyFT518() {
        createLanguageLocationData();

        // Creating a Active flw user and updating his status to Invalid
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        District district = districtService.findByStateAndCode(sampleState, 1L);
        Language language = languageService.getForCode("50");
        Swachchagrahi flw = new Swachchagrahi("Test Worker", 2111111111L);
        flw.setState(sampleState);
        flw.setDistrict(district);
//        flw.setLanguage(language);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw);
        transactionManager.commit(status);

        flw = swcService.getByContactNumber(2111111111L);
        flw.setCourseStatus(SwachchagrahiStatus.INVALID);
        flw.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(flw);
        assertEquals(swcStatusUpdateAuditDataService.count(), 1l);


        // Creating a Anonymous flw user and updating his status to Active

        status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        District district1 = districtService.findByStateAndCode(sampleState, 1L);
        Language language1 = languageService.getForCode("50");
        Swachchagrahi flw1 = new Swachchagrahi(2111111112L);
        flw1.setState(sampleState);
        flw1.setDistrict(district1);
//        flw1.setLanguage(language1);
        flw1.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(flw1);
        transactionManager.commit(status);

        flw1 = swcService.getByContactNumber(2111111112L);
        flw1.setName("Test Worker1");
        swcService.update(flw1);
        assertEquals(swcStatusUpdateAuditDataService.count(), 2l);

        // Changing the status previous updated Active user to Invalid
        Swachchagrahi flw2 = swcService.getByContactNumber(2111111112L);
        flw2.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(flw2);
        assertEquals(swcStatusUpdateAuditDataService.count(), 3l);
        

    }
}
