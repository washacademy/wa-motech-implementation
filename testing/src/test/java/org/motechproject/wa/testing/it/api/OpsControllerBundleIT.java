package org.motechproject.wa.testing.it.api;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.mtraining.domain.ActivityState;
import org.motechproject.mtraining.domain.Bookmark;
import org.motechproject.mtraining.repository.ActivityDataService;
import org.motechproject.mtraining.service.BookmarkService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.testing.osgi.http.SimpleHttpClient;
import org.motechproject.testing.utils.TestContext;
import org.motechproject.wa.api.web.contract.AddSwcRequest;
import org.motechproject.wa.region.domain.*;
import org.motechproject.wa.region.repository.CircleDataService;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.region.repository.LanguageDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.DistrictService;
import org.motechproject.wa.region.service.LanguageService;
import org.motechproject.wa.rejectionhandler.domain.SwcImportRejection;
import org.motechproject.wa.rejectionhandler.repository.SwcImportRejectionDataService;
import org.motechproject.wa.swc.domain.*;
import org.motechproject.wa.swc.repository.SwcDataService;
import org.motechproject.wa.swc.repository.SwcErrorDataService;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.testing.it.api.utils.RequestBuilder;
import org.motechproject.wa.testing.it.utils.RegionHelper;
import org.motechproject.wa.testing.it.utils.SubscriptionHelper;
import org.motechproject.wa.testing.service.TestingService;
import org.motechproject.wa.washacademy.domain.CourseCompletionRecord;
import org.motechproject.wa.washacademy.domain.WaCourse;
import org.motechproject.wa.washacademy.dto.WaBookmark;
import org.motechproject.wa.washacademy.repository.CourseCompletionRecordDataService;
import org.motechproject.wa.washacademy.repository.WaCourseDataService;
import org.motechproject.wa.washacademy.service.WashAcademyService;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for Ops controller
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class OpsControllerBundleIT extends BasePaxIT {

    private String addSwcEndpoint = String.format("http://localhost:%d/motech-platform-server/module/api/ops/createUpdateRchSwc",
            TestContext.getJettyPort());
    private String deactivationRequest = String.format("http://localhost:%d/motech-platform-server/module/api/ops/deactivationRequest",
            TestContext.getJettyPort());
    State state;
    District district;
    Block block;
    Panchayat panchayat;
    Language language;

    @Inject
    PlatformTransactionManager transactionManager;

    @Inject
    TestingService testingService;

    @Inject
    SwcDataService swcDataService;

    @Inject
    SwcService swcService;

    @Inject
    SwcErrorDataService swcErrorDataService;


    @Inject
    LanguageDataService languageDataService;
    @Inject
    LanguageService languageService;
    @Inject
    CircleDataService circleDataService;
    @Inject
    StateDataService stateDataService;
    @Inject
    DistrictDataService districtDataService;
    @Inject
    DistrictService districtService;
    @Inject
    ActivityDataService activityDataService;
    @Inject
    WashAcademyService maService;
    @Inject
    CourseCompletionRecordDataService courseCompletionRecordDataService;

    @Inject
    SwcImportRejectionDataService swcImportRejectionDataService;

    @Inject
    WaCourseDataService waCourseDataService;

    private RegionHelper rh;
    private SubscriptionHelper sh;
    private static final String VALID_CALL_ID = "1234567890123456789012345";

    @Inject
    BookmarkService bookmarkService;

    @Before
    public void setupTestData() {
        testingService.clearDatabase();
        initializeLocationData();
    }

    // Test swc update with empty swc request
    @Test
    public void testEmptyAddSwcRequest() throws IOException, InterruptedException {
        HttpPost request = RequestBuilder.createPostRequest(addSwcEndpoint, new AddSwcRequest());
        assertTrue(SimpleHttpClient.execHttpRequest(request, HttpStatus.SC_BAD_REQUEST, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
    }

    // swc udpate failes with not all required fields present
    @Test
    public void testBadContactNumberAddSwcRequest() throws IOException, InterruptedException {
        AddSwcRequest addRequest = new AddSwcRequest();
        addRequest.setMsisdn(9876543210L);
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_BAD_REQUEST, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
    }

    // Create valid new swc
    @Test
    public void testCreateNewSwc() throws IOException, InterruptedException {
        AddSwcRequest addSwcRequest = getAddRequestASHA();
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addSwcRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
    }

    // Validating whether the type is ASHA or not
//    @Test
//    public void testASHAValidation() throws IOException, InterruptedException {
//        AddSwcRequest addSwcRequest = getAddRequestANM();
//        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addSwcRequest);
//        assertFalse(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
//    }

//    @Test
//    public void testInactiveGfValidation() throws IOException, InterruptedException {
//        stateDataService.create(state);
//        districtDataService.create(district);
//        AddSwcRequest addSwcRequest = getAddRequestInactiveGfStatus();
//        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addSwcRequest);
//        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
//        Swachchagrahi swc = swcService.getByContactNumber(9876543210L);
//        assertNull(swc);
//    }

//    @Test
//    public void testInactiveGfUpdate() throws IOException, InterruptedException {
//        stateDataService.create(state);
//        districtDataService.create(district);
//        AddSwcRequest addSwcRequest = getAddRequestASHA();
//        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addSwcRequest);
//        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
//        Swachchagrahi swc = swcService.getByContactNumber(9876543210L);
//        assertNotNull(swc);
//        addSwcRequest = getAddRequestInactiveGfStatus();
//        httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addSwcRequest);
//        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
//        swc = swcService.getByContactNumber(9876543210L);
//        assertNull(swc);
//    }

    // Create valid new swc
    @Test
    public void testCreateNewSwcTalukaVillage() throws IOException, InterruptedException {

        createSwcHelper("Chinkoo Devi", 9876543210L, "123");
        Swachchagrahi swc = swcService.getByContactNumber(9876543210L);
        assertNotNull(swc.getState());
        assertNotNull(swc.getDistrict());
        assertNotNull(swc.getBlock());    // null since we don't create it by default in helper
        assertNotNull(swc.getPanchayat());   // null since we don't create it by default in helper

        AddSwcRequest addSwcRequest = getAddRequestASHA();
        addSwcRequest.setBlockId(block.getCode());
        addSwcRequest.setPanchayatId(panchayat.getVcode());
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addSwcRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));


        // refetch and check that taluka and village are set
        swc = swcService.getByContactNumber(9876543210L);
        assertNotNull(swc.getState());
        assertNotNull(swc.getDistrict());
        assertNotNull(swc.getBlock());
        assertNotNull(swc.getPanchayat());
    }

    @Test
    public void testCheckPhoneNumber() throws IOException, InterruptedException {

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        AddSwcRequest addSwcRequest = getAddRequestASHAInvalid();
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addSwcRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_BAD_REQUEST, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
        transactionManager.commit(status);

        List<SwcImportRejection> swcImportRejections = swcImportRejectionDataService.retrieveAll();
        assertEquals(1, swcImportRejections.size());
    }

    // Swc test name update
    @Test
    public void testUpdateSwcName() throws IOException, InterruptedException {

        // create swc
        createSwcHelper("Kookoo Devi", 9876543210L, "123");

        AddSwcRequest updateRequest = getAddRequestASHA();
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, updateRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
        Swachchagrahi swc = swcService.getByContactNumber(9876543210L);
        assertNotEquals(swc.getName(), "Kookoo Devi");
        assertEquals(swc.getName(), updateRequest.getName());
    }

    // Swc update phone number
    @Test
    public void testUpdateSwcPhoneOpen() throws IOException, InterruptedException {

        // create swc
        createSwcHelper("Chinkoo Devi", 9876543210L, "123");

        AddSwcRequest updateRequest = getAddRequestASHA();
        updateRequest.setMsisdn(9876543211L);    // update
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, updateRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
    }

    // Test swc update to an existing used phone number by someone else
    @Test
    public void testUpdateSwcPhoneOccupied() throws IOException, InterruptedException {

        // create swc
        createSwcHelper("Chinkoo Devi", 9876543210L, "456");

        long before = swcImportRejectionDataService.count();

        AddSwcRequest updateRequest = getAddRequestASHA();
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, updateRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));

        long after = swcImportRejectionDataService.count();

        assertEquals("No new expected swc error created", before + 1, after);

        List<SwcError> swcErrors = swcErrorDataService.findBySwcId(
                updateRequest.getSwcId(),
                updateRequest.getStateId(),
                updateRequest.getDistrictId());

        // since we clear the db before each test, safe to assume that we will only have 1 item in list
        assertEquals(swcErrors.get(0).getReason(), SwcErrorReason.PHONE_NUMBER_IN_USE);
    }

    // Test swc update to an existing used phone number by someone else
    @Test
    public void testUpdateSwcAnonymousMctsMerge() throws IOException, InterruptedException {

        // create swc with null mcts id
        createSwcHelper("Chinkoo Devi", 9876543210L, null);

        AddSwcRequest updateRequest = getAddRequestASHA();
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, updateRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));

        Swachchagrahi swcByNumber = swcService.getByContactNumber(9876543210L);
        assertEquals("Anonymous user was not merged", updateRequest.getSwcId(), swcByNumber.getSwcId());
    }


    @Test
    public void testUpdateNoState() throws IOException, InterruptedException {

        // create swc
        createSwcHelper("State Singh", 9876543210L, "123");

        long before = swcErrorDataService.count();

        AddSwcRequest updateRequest = getAddRequestASHA();
        updateRequest.setStateId(5L);    // 5 doesn't exist since setup only creates state '1'
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, updateRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));

        long after = swcErrorDataService.count();

        assertEquals("No new expected swc error created", before + 1, after);

        List<SwcError> swcErrors = swcErrorDataService.findBySwcId(
                updateRequest.getSwcId(),
                updateRequest.getStateId(),
                updateRequest.getDistrictId());

        // since we clear the db before each test, safe to assume that we will only have 1 item in list
        assertEquals(swcErrors.get(0).getReason(), SwcErrorReason.INVALID_LOCATION_STATE);
    }

    @Test
    public void testUpdateNoDistrict() throws IOException, InterruptedException {

        // create swc
        createSwcHelper("District Singh", 9876543210L, "123");

        long before = swcErrorDataService.count();

        AddSwcRequest updateRequest = getAddRequestASHA();
        updateRequest.setDistrictId(5L);    // 5 doesn't exist since setup only creates district '1'
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, updateRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));

        long after = swcErrorDataService.count();

        assertEquals("No new expected swc error created", before + 1, after);

        List<SwcError> swcErrors = swcErrorDataService.findBySwcId(
                updateRequest.getSwcId(),
                updateRequest.getStateId(),
                updateRequest.getDistrictId());

        // since we clear the db before each test, safe to assume that we will only have 1 item in list
        assertEquals(swcErrors.get(0).getReason(), SwcErrorReason.INVALID_LOCATION_DISTRICT);
    }

    @Test
    public void testGetScoresForUser() throws IOException, InterruptedException {
        Long callingNumber = 9876543210L;
        String getScoresEndpoint = String.format("http://localhost:%d/motech-platform-server/module/api/ops/getScores?callingNumber=%d",
                TestContext.getJettyPort(), callingNumber);
        Map<String, Integer> scores = new HashMap<>();
        scores.put("1", 4);
        scores.put("2", 3);
        Map<String, Object> progress = new HashMap<>();
        progress.put("scoresByChapter", scores);
        Bookmark newBookmark = new Bookmark(callingNumber.toString(), null, null, null, progress);
        bookmarkService.createBookmark(newBookmark);

        HttpGet httpGet = RequestBuilder.createGetRequest(getScoresEndpoint);
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertNotNull(response);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String body = IOUtils.toString(response.getEntity().getContent());
        assertTrue(body.contains("1=4"));
        assertTrue(body.contains("2=3"));
    }

    @Test
    public void testGetScoresForUserNoScores() throws IOException, InterruptedException {
        Long callingNumber = 9876543210L;
        String getScoresEndpoint = String.format("http://localhost:%d/motech-platform-server/module/api/ops/getScores?callingNumber=%d",
                TestContext.getJettyPort(), 9976543210L);
        Map<String, Integer> scores = new HashMap<>();
        scores.put("1", 4);
        scores.put("2", 3);
        Map<String, Object> progress = new HashMap<>();
        progress.put("scoresByChapter", scores);
        Bookmark newBookmark = new Bookmark(callingNumber.toString(), null, null, null, progress);
        bookmarkService.createBookmark(newBookmark);

        HttpGet httpGet = RequestBuilder.createGetRequest(getScoresEndpoint);
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertNotNull(response);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String body = IOUtils.toString(response.getEntity().getContent());
        assertEquals("{000000}", body);
    }

//    @Test
//    public void testUpdateWrongGfStatus() throws IOException, InterruptedException {
//
//        // create swc with null mcts id
//        createSwcHelper("Chinkoo Devi", 9876543210L, null);
//
//        AddSwcRequest updateRequest = getAddRequestASHA();
//        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, updateRequest);
//        System.out.println(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
//        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
//
//        updateRequest.setJobStatus("Random");
//        httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, updateRequest);
//        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpRequest, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
//        assertNotNull(response);
//        assertEquals(400, response.getStatusLine().getStatusCode());
//
//
//    }

    private void createSwcHelper(String name, Long phoneNumber, String SwcId) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        stateDataService.create(state);
        // create swc
        Swachchagrahi swc = new Swachchagrahi(name, phoneNumber);
        swc.setSwcId(SwcId);
        swc.setState(state);
        swc.setDistrict(district);
        swc.setBlock(block);
        swc.setPanchayat(panchayat);
        swc.setLanguage(language);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);
        swcDataService.create(swc);
        transactionManager.commit(status);
    }

    // helper to create a valid swc add/update request with designation ASHA
    private AddSwcRequest getAddRequestASHA() {
        AddSwcRequest request = new AddSwcRequest();
        request.setMsisdn(9876543210L);
        request.setName("Chinkoo Devi");
        request.setSwcId("123");
        request.setStateId(state.getCode());
        request.setDistrictId(district.getCode());
        request.setBlockId(block.getCode());
        request.setPanchayatId(panchayat.getVcode());
        request.setBlockName(block.getName());
        request.setPanchayatName(panchayat.getName());
        return request;
    }

    // helper to create a invalid swc add/update request with designation ASHA
    private AddSwcRequest getAddRequestASHAInvalid() {
        AddSwcRequest request = new AddSwcRequest();
        request.setMsisdn(987643210L);
        request.setName("Chinkoo Devi");
        request.setSwcId("123");
        request.setStateId(state.getCode());
        request.setDistrictId(district.getCode());
        request.setBlockId(block.getCode());
        request.setPanchayatId(panchayat.getVcode());
        request.setBlockName(block.getName());
        request.setPanchayatName(panchayat.getName());
        return request;
    }

    // helper to create an swc add/update request with designation ANM
    private AddSwcRequest getAddRequestANM() {
        AddSwcRequest request = new AddSwcRequest();
        request.setMsisdn(9876543210L);
        request.setName("Chinkoo Devi");
        request.setSwcId("123");
        request.setStateId(state.getCode());
        request.setDistrictId(district.getCode());
        request.setBlockId(block.getCode());
        request.setPanchayatId(panchayat.getVcode());
        request.setBlockName(block.getName());
        request.setPanchayatName(panchayat.getName());
        return request;
    }

    private AddSwcRequest getAddRequestInactiveGfStatus() {
            AddSwcRequest request = new AddSwcRequest();
            request.setMsisdn(9876543210L);
            request.setName("Chinkoo Devi");
            request.setSwcId("123");
            request.setStateId(state.getCode());
            request.setDistrictId(district.getCode());
            request.setBlockId(block.getCode());
            request.setPanchayatId(panchayat.getVcode());
            request.setBlockName(block.getName());
            request.setPanchayatName(panchayat.getName());
            return request;
    }

    // helper to create location data
    private void initializeLocationData() {

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());


        panchayat = new Panchayat();
        panchayat.setName("Panchayat 1");
        panchayat.setRegionalName("Panchayat 1");
        panchayat.setVcode(1L);

        block = new Block();
        block.setName("Block 1");
        block.setRegionalName("Block 1");
        block.setIdentity(1);
        block.setCode(1L);
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

        language = languageDataService.create(new Language("15", "HINDI_DEFAULT"));
        district.setLanguage(language);

        transactionManager.commit(status);
    }

    // create subscriber with many subscriptions helper
//    private void createSubscriberHelper() {
//
//        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        // create subscription for a msisdn
//        rh = new RegionHelper(languageDataService, languageService, circleDataService, stateDataService,
//                districtDataService, districtService);
//        sh = new SubscriptionHelper(subscriptionService, subscriberDataService, subscriptionPackDataService,
//                languageDataService, languageService, circleDataService, stateDataService, districtDataService,
//                districtService);
//
//        Subscriber subscriberIVR = subscriberDataService.create(new Subscriber(5000000000L));
//        subscriberIVR.setLastMenstrualPeriod(DateTime.now().plusWeeks(70));
//        subscriberIVR = subscriberDataService.update(subscriberIVR);
//
//       subscriptionService.createSubscription(subscriberIVR, subscriberIVR.getCallingNumber(), rh.kannadaLanguage(), rh.karnatakaCircle(),
//                sh.pregnancyPack(), SubscriptionOrigin.IVR);
//
//        Subscriber subscriberMCTS = subscriberDataService.create(new Subscriber(6000000000L));
//        subscriberMCTS.setLastMenstrualPeriod(DateTime.now().plusWeeks(70));
//        subscriberMCTS = subscriberDataService.update(subscriberMCTS);
//        subscriptionService.createSubscription(subscriberMCTS, subscriberMCTS.getCallingNumber(), rh.kannadaLanguage(), rh.karnatakaCircle(),
//                sh.pregnancyPack(), SubscriptionOrigin.MCTS_IMPORT);
//        transactionManager.commit(status);
//    }


//    public void testifAllSubscriberDectivated() {
//
//        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//
//        Subscriber subscriberIVR = subscriberDataService.findByNumber(5000000000L).get(0);
//        Set<Subscription> subscriptionsIVR = ( Set<Subscription> ) subscriberDataService.getDetachedField(subscriberIVR, "subscriptions");
//        for (Subscription subscriptionIVR : subscriptionsIVR) {
//            Assert.assertTrue(subscriptionIVR.getDeactivationReason().equals(DeactivationReason.WEEKLY_CALLS_NOT_ANSWERED));
//            Assert.assertTrue(subscriptionIVR.getStatus().equals(SubscriptionStatus.DEACTIVATED));
//        }
//
//        Subscriber subscriberMCTS = subscriberDataService.findByNumber(6000000000L).get(0);
//        Set<Subscription> subscriptionsMCTS = ( Set<Subscription> ) subscriberDataService.getDetachedField(subscriberMCTS, "subscriptions");
//        for (Subscription subscriptionMCTS : subscriptionsMCTS) {
//            Assert.assertTrue(subscriptionMCTS.getDeactivationReason().equals(DeactivationReason.WEEKLY_CALLS_NOT_ANSWERED) || subscriptionMCTS.getDeactivationReason().equals(DeactivationReason.LOW_LISTENERSHIP));
//            Assert.assertTrue(subscriptionMCTS.getStatus().equals(SubscriptionStatus.DEACTIVATED));
//        }
//        transactionManager.commit(status);
//
//    }


    private void testDeactivationRequestByMsisdn(Long msisdn, String deactivationReason, int status) throws IOException, InterruptedException, URISyntaxException {
        StringBuilder sb = new StringBuilder(deactivationRequest);
        sb.append("?");
        sb.append(String.format("msisdn=%s", msisdn.toString()));
        sb.append("&");
        sb.append(String.format("deactivationReason=%s", deactivationReason));
        HttpDelete httpRequest = new HttpDelete(sb.toString());
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, status, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));
    }

    // Test audit trail of deactivation subscriptions
//    public void testDeactivationSubscriptionAuditService(Long msisdn, SubscriptionOrigin origin, int testNumber) {
//        List<DeactivationSubscriptionAuditRecord> auditRecords = deactivationSubscriptionAuditRecordDataService.retrieveAll();
//        assertEquals(testNumber, auditRecords.size());
//        assertEquals(msisdn, auditRecords.get(testNumber-1).getMsisdn());
//        assertEquals(origin, auditRecords.get(testNumber-1).getSubscriptionOrigin());
//        assertEquals(AuditStatus.SUCCESS, auditRecords.get(testNumber-1).getAuditStatus());
//    }

    //Test deactivation of specific msisdn - 5000000000L as IVR and 6000000000L as MCTS import
//    @Test
//    public void testDeactivateSpecificValidMsisdn() throws IOException, InterruptedException, URISyntaxException {
//        createSubscriberHelper();
//        testDeactivationRequestByMsisdn(5000000000L, "WEEKLY_CALLS_NOT_ANSWERED", HttpStatus.SC_OK);
//        testDeactivationRequestByMsisdn(5000000000L, "WEEKLY_CALLS_NOT_ANSWERED", HttpStatus.SC_OK);   // Test deactivation of same number again
//        testDeactivationSubscriptionAuditService(5000000000L, SubscriptionOrigin.IVR, 1);
//        testDeactivationRequestByMsisdn(6000000000L, "LOW_LISTENERSHIP", HttpStatus.SC_OK);
//        testDeactivationSubscriptionAuditService(6000000000L, SubscriptionOrigin.MCTS_IMPORT, 2);
//        testifAllSubscriberDectivated();
//        testReactivationDisabledAfterDeactivation(5000000000L);
//        testReactivationDisabledAfterDeactivation(6000000000L);
//    }

//    @Test
//    public void testDeactivateSpecificValidNotInDatabaseMsisdn() throws IOException, InterruptedException, URISyntaxException {
//        testDeactivationRequestByMsisdn(7000000000L, "LOW_LISTENERSHIP", HttpStatus.SC_BAD_REQUEST);
//    }

    @Test
    public void testDeactivateSpecificInValidMsisdn() throws IOException, InterruptedException, URISyntaxException {
        testDeactivationRequestByMsisdn(1000-00L, "LOW_LISTENERSHIP", HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testDeactivateSpecificInValidDeactivationReason() throws IOException, InterruptedException, URISyntaxException {
        testDeactivationRequestByMsisdn(5000000000L, "DEACTIVATED_BY_USER", HttpStatus.SC_BAD_REQUEST);
    }

//    private void testReactivationDisabledAfterDeactivation(long msisdn) throws IOException, InterruptedException, URISyntaxException {
//        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        // Testing weekly_calls_not_answered record with the given number
//        BlockedMsisdnRecord blockedMsisdnRecord = blockedMsisdnRecordDataService.findByNumber(msisdn);
//        assertNotNull(blockedMsisdnRecord);
//
//        Subscriber subscriber = subscriberDataService.findByNumber(msisdn).get(0);
//        Subscription subscription = subscriptionService.createSubscription(subscriber, subscriber.getCallingNumber(), rh.kannadaLanguage(), rh.karnatakaCircle(),
//                sh.pregnancyPack(), SubscriptionOrigin.IVR);
//        Assert.assertNull(subscription);
//        transactionManager.commit(status);
//    }

    // Test whether MSISDN is updated in Bookmark, Activity and Course Completion Records along with Swc
    @Test
    public void testMaMsisdnUpdate() throws IOException, InterruptedException {
        setupWaCourse();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        stateDataService.create(state);
        transactionManager.commit(status);
        AddSwcRequest addSwcRequest = getAddRequestASHA();
        HttpPost httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, addSwcRequest);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));

        Swachchagrahi swc = swcService.getByContactNumber(9876543210L);
        Long swcId = swc.getId();
        WaBookmark bookmark = new WaBookmark(swcId, VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark);
        assertNotNull(maService.getBookmark(9876543210L, VALID_CALL_ID));
        assertEquals(1, activityDataService.findRecordsForUserByState("9876543210", ActivityState.STARTED).size());

        bookmark.setBookmark("COURSE_COMPLETED");
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 5; i++) {
            scores.put(String.valueOf(i), 3);
        }

        swc = swcService.getByContactNumber(9876543210L);
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark);
        List <CourseCompletionRecord> ncrs = courseCompletionRecordDataService.findBySwcId(swc.getId());
        assertEquals(1, ncrs.size());

        // Update Msisdn and verify MA records
        AddSwcRequest request = new AddSwcRequest();
        request.setMsisdn(7896543210L);
        request.setName("Chinkoo Devi");
        request.setSwcId("123");
        request.setStateId(state.getCode());
        request.setDistrictId(district.getCode());
        request.setBlockId(block.getCode());
        request.setPanchayatId(panchayat.getVcode());
        request.setBlockName(block.getName());
        request.setPanchayatName(panchayat.getName());
        httpRequest = RequestBuilder.createPostRequest(addSwcEndpoint, request);
        assertTrue(SimpleHttpClient.execHttpRequest(httpRequest, HttpStatus.SC_OK, RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD));

        assertNull(maService.getBookmark(9876543210L, VALID_CALL_ID));
        assertNotNull(maService.getBookmark(7896543210L, VALID_CALL_ID));

        assertEquals(0, activityDataService.findRecordsForUserByState("9876543210", ActivityState.STARTED).size());
        assertEquals(1, activityDataService.findRecordsForUserByState("7896543210", ActivityState.STARTED).size());

        swc = swcService.getByContactNumber(7896543210L);
        assertEquals(1, courseCompletionRecordDataService.findBySwcId(swc.getId()).size());
    }

    private JSONObject setupWaCourse() throws IOException {
        org.motechproject.wa.washacademy.dto.WaCourse course = new org.motechproject.wa.washacademy.dto.WaCourse();
        String jsonText = IOUtils.toString(new InputStreamReader(getClass().getClassLoader().
                getResourceAsStream("WaCourse.json")));
        JSONObject jo = new JSONObject(jsonText);
        course.setName(jo.get("name").toString());
        course.setContent(jo.get("chapters").toString());
        waCourseDataService.create(new WaCourse(course.getName(), course.getContent(),4,8));
        return jo;
    }
}
