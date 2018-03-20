package org.motechproject.wa.testing.it.swc;

import org.joda.time.DateTime;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.mds.ex.JdoListenerInvocationException;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.wa.region.domain.Circle;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.repository.CircleDataService;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.region.repository.LanguageDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.DistrictService;
import org.motechproject.wa.region.service.LanguageService;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwachchagrahiStatus;
import org.motechproject.wa.swc.domain.SwcJobStatus;
import org.motechproject.wa.swc.repository.SwcDataService;
import org.motechproject.wa.swc.repository.SwcStatusUpdateAuditDataService;
import org.motechproject.wa.swc.repository.WhitelistEntryDataService;
import org.motechproject.wa.swc.repository.WhitelistStateDataService;
import org.motechproject.wa.swc.service.ServiceUsageService;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.swc.service.SwcSettingsService;
import org.motechproject.wa.testing.service.TestingService;
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

import static org.junit.Assert.*;


/**
 * Verify that SwcService present, functional.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class SwachgrahiServiceBundleIT extends BasePaxIT {

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

    private static final String SWC_PURGE_EVENT_SUBJECT = "wa.swc.purge_invalid_swc";

    @Inject
    private SwcSettingsService swcSettingsService;

    private SettingsFacade settingsFacade;

    @Inject
    private EventRelay eventRelay;

    String oldWeeksToKeepInvalidSWCs;

    @Before
    public void doTheNeedful() {
        testingService.clearDatabase();

        settingsFacade = swcSettingsService.getSettingsFacade();
        oldWeeksToKeepInvalidSWCs = settingsFacade
                .getProperty(WEEKS_TO_KEEP_INVALID_SWCS);
    }

    @After
    public void restore() {
        settingsFacade.setProperty(WEEKS_TO_KEEP_INVALID_SWCS,
                oldWeeksToKeepInvalidSWCs);
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
    public void testPurgeOldInvalidFrontLineWorkers() {
        // SWC1 & 2 Should be purged, the others should remain

        Swachchagrahi swc1 = new Swachchagrahi("Test Worker", 1111111110L);
        swc1.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc1);
        swc1.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc1.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc1);

        Swachchagrahi swc2 = new Swachchagrahi("Test Worker", 1111111111L);
        swc2.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc2);
        swc2.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc2.setInvalidationDate(new DateTime().now().minusWeeks(6).minusDays(1));
        swcService.update(swc2);

        Swachchagrahi swc3 = new Swachchagrahi("Test Worker", 1111111112L);
        swc3.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc3);
        swc3.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc3.setInvalidationDate(new DateTime().now().minusWeeks(6));
        swcService.update(swc3);

        Swachchagrahi swc4 = new Swachchagrahi("Test Worker", 2111111111L);
        swc4.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc4);
        swc4.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc4);

        Swachchagrahi swc5 = new Swachchagrahi("Test Worker", 2111111112L);
        swc5.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc5);
        swc5.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(swc5);

        Swachchagrahi swc6 = new Swachchagrahi("Test Worker", 2111111113L);
        swc6.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc6);

        List<Swachchagrahi> records = swcService.getRecords();
        assertEquals(6, records.size());
        assertTrue(records.contains(swc1));
        assertTrue(records.contains(swc2));
        assertTrue(records.contains(swc3));
        assertTrue(records.contains(swc4));
        assertTrue(records.contains(swc5));
        assertTrue(records.contains(swc6));

        swcService.purgeOldInvalidSWCs(new MotechEvent());
        records = swcService.getRecords();
        assertEquals(4, records.size());
        assertFalse(records.contains(swc1));
        assertFalse(records.contains(swc2));
        assertTrue(records.contains(swc3));
        assertTrue(records.contains(swc4));
        assertTrue(records.contains(swc5));
        assertTrue(records.contains(swc6));
    }

    @Test
    public void testFrontLineWorkerService() throws Exception {
        Swachchagrahi swc = new Swachchagrahi("Test Worker", 1111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        Swachchagrahi otherSwc = swcService.getByContactNumber(1111111111L);
        assertNotNull(otherSwc);

        Swachchagrahi record = swcService.getByContactNumber(swc.getContactNumber());
        assertEquals(swc, record);

        List<Swachchagrahi> records = swcService.getRecords();
        assertTrue(records.contains(swc));

        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc);
        swcService.delete(swc);
        record = swcService.getByContactNumber(swc.getContactNumber());
        assertNull(record);
    }

    /**
     * wa_FT_515 : To verify that status of Active swc is set to "Invalid" successfully
     */
    @Test
    public void testFrontLineWorkerUpdate() {
        createLanguageLocationData();

        District district = districtService.findByStateAndCode(sampleState, 1L);
        Language language = languageService.getForCode("50");

        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(2111111111L);

        assertEquals(SwachchagrahiStatus.ANONYMOUS, swc.getCourseStatus());

        swc.setState(sampleState);
        swc.setDistrict(district);
        swc.setName("Frank Huster");
//        swc.setLanguage(language);

        swcService.update(swc);
        swc = swcService.getByContactNumber(2111111111L);
        assertEquals(SwachchagrahiStatus.ACTIVE, swc.getCourseStatus());

        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(swc);
        swc = swcService.getById(swc.getId());
        assertEquals(SwachchagrahiStatus.INVALID, swc.getCourseStatus());
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testDeleteNonInvalidFrontLineWorker() {
        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(2111111111L);

        assertEquals(SwachchagrahiStatus.ANONYMOUS, swc.getCourseStatus());

        exception.expect(JdoListenerInvocationException.class);
        swcService.delete(swc);
    }

    @Test
    public void testDeleteRecentInvalidFrontLineWorker() {
        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        swc = swcService.getByContactNumber(2111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(swc);

        swc = swcService.getById(swc.getId());
        assertEquals(SwachchagrahiStatus.INVALID, swc.getCourseStatus());

        exception.expect(JdoListenerInvocationException.class);
        swcService.delete(swc);
    }

    @Test
    public void testDeleteOldInvalidFrontLineWorker() {
        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        swc = swcService.getByContactNumber(2111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc);

        swc = swcService.getById(swc.getId());
        assertEquals(SwachchagrahiStatus.INVALID, swc.getCourseStatus());

        swcService.delete(swc);
    }

    @Test
    public void testNewAshaTakingOldResignationNumber() {
        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(2111111111L);
        swc.setJobStatus(SwcJobStatus.INACTIVE);
        swcService.update(swc);
        swc = swcService.getByContactNumber(2111111111L);
        assertNull(swc);

        swc = new Swachchagrahi("New Asha", 2111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(2111111111L);
        assertNotNull(swc);

        assertEquals("New Asha", swc.getName());
        List<Swachchagrahi> swachchagrahis = swcDataService.retrieveAll();
        assertEquals(2, swachchagrahis.size());
    }

    /**
     * To disable automatic deletion of all records of beneficiary which were
     * marked invalid 6 weeks ago.
     *
     * @throws InterruptedException
     */
    // TODO https://applab.atlassian.net/browse/wa-257
    @Test
    public void verifyFT548() throws InterruptedException {
        Map<String, Object> eventParams = new HashMap<>();
        MotechEvent motechEvent = new MotechEvent(SWC_PURGE_EVENT_SUBJECT,
                eventParams);

        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(2111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(DateTime.now().minusWeeks(7));
        swcService.update(swc);

        //call purge event
        swcService.purgeOldInvalidSWCs(motechEvent);

        // assert flW deleted
        swc = swcService.getByContactNumber(2111111111L);
        assertNull(swc);

        // change configuration to disable deletion by setting weeks to large value
        settingsFacade.setProperty(WEEKS_TO_KEEP_INVALID_SWCS, "1000");

        // add new invalidated swc
        swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setSwcId("SwcId");
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(2111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        // set invalid date to 2 years back
        swc.setInvalidationDate(DateTime.now().minusYears(2));
        swcService.update(swc);

        //call purge event
        swcService.purgeOldInvalidSWCs(motechEvent);

        // assert flW not deleted
        swc = swcService.getBySwcId("SwcId");
        assertNotNull(swc);
    }

    /**
     * To verify that status of Anonymous swc is set to "Invalid" successfully
     */
    @Test
    public void verifyFT514() {
        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        swc = swcService.getByContactNumber(2111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc);

        swc = swcService.getById(swc.getId());
        assertEquals(SwachchagrahiStatus.INVALID, swc.getCourseStatus());
        assertNull(swc.getContactNumber());
    }

    /**
     * To verify that status of Inactive swc is set to "Invalid" successfully
     */
    @Test
    public void verifyFT516() {
        createLanguageLocationData();

        District district = districtService.findByStateAndCode(sampleState, 1L);
        Language language = languageService.getForCode("50");
        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setState(sampleState);
        swc.setDistrict(district);
//        swc.setLanguage(language);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        swc = swcService.getByContactNumber(2111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc);

        swc = swcService.getById(swc.getId());
        assertEquals(SwachchagrahiStatus.INVALID, swc.getCourseStatus());
        assertNull(swc.getContactNumber());
    }

    /**
     * To verify that status of "Active" swc to "Invalid" and
     * the status of "Anonymous" swc to "Active" is audited properly
     */
    @Test
    @Ignore
    public void verifyFT518() {
        createLanguageLocationData();

        // Creating a Active swc user and updating his status to Invalid
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        District district = districtService.findByStateAndCode(sampleState, 1L);
        Language language = languageService.getForCode("50");
        Swachchagrahi swc = new Swachchagrahi("Test Worker", 2111111111L);
        swc.setState(sampleState);
        swc.setDistrict(district);
//        swc.setLanguage(language);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        transactionManager.commit(status);

        swc = swcService.getByContactNumber(2111111111L);
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc);
        assertEquals(swcStatusUpdateAuditDataService.count(), 1l);


        // Creating a Anonymous swc user and updating his status to Active

        status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        District district1 = districtService.findByStateAndCode(sampleState, 1L);
        Language language1 = languageService.getForCode("50");
        Swachchagrahi swc1 = new Swachchagrahi(2111111112L);
        swc1.setState(sampleState);
        swc1.setDistrict(district1);
//        swc1.setLanguage(language1);
        swc1.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc1);
        transactionManager.commit(status);

        swc1 = swcService.getByContactNumber(2111111112L);
        swc1.setName("Test Worker1");
        swcService.update(swc1);
        assertEquals(swcStatusUpdateAuditDataService.count(), 2l);

        // Changing the status previous updated Active user to Invalid
        Swachchagrahi swc2 = swcService.getByContactNumber(2111111112L);
        swc2.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(swc2);
        assertEquals(swcStatusUpdateAuditDataService.count(), 3l);
        

    }
}
