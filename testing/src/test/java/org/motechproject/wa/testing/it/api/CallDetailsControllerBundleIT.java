package org.motechproject.wa.testing.it.api;

import com.google.common.base.Joiner;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.testing.osgi.http.SimpleHttpClient;
import org.motechproject.testing.utils.TestContext;
import org.motechproject.wa.api.web.contract.SwcUserResponse;
import org.motechproject.wa.props.domain.DeployedService;
import org.motechproject.wa.props.domain.Service;
import org.motechproject.wa.props.repository.DeployedServiceDataService;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.repository.CircleDataService;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.region.repository.LanguageDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.DistrictService;
import org.motechproject.wa.region.service.LanguageService;
import org.motechproject.wa.swc.domain.*;
import org.motechproject.wa.swc.repository.CallDetailRecordDataService;
import org.motechproject.wa.swc.repository.SwcStatusUpdateAuditDataService;
import org.motechproject.wa.swc.service.CallDetailRecordService;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.testing.it.api.utils.RequestBuilder;
import org.motechproject.wa.testing.it.utils.RegionHelper;
import org.motechproject.wa.testing.service.TestingService;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.motechproject.wa.testing.it.api.utils.RequestBuilder;


import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class CallDetailsControllerBundleIT extends BasePaxIT {
    private static final String ADMIN_USERNAME = "motech";
    private static final String ADMIN_PASSWORD = "motech";
    private static final String VALID_CALL_ID = "1234567890123456789012345";

    private static final int MILLISECONDS_PER_SECOND = 1000;

    @Inject
    CallDetailRecordService callDetailRecordService;

    @Inject
    CallDetailRecordDataService callDetailRecordDataService;

    @Inject
    SwcService swcService;

    @Inject
    TestingService testingService;

    @Inject
    DeployedServiceDataService deployedServiceDataService;

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
    SwcStatusUpdateAuditDataService swcStatusUpdateAuditDataService;

    @Inject
    PlatformTransactionManager transactionManager;


    private RegionHelper rh;


    @Before
    public void clearDatabase() {
        testingService.clearDatabase();
        rh = new RegionHelper(languageDataService, languageService, circleDataService, stateDataService,
                districtDataService, districtService);
    }

    /**
     * method to pass all values as per data type mentioned in API
     */
    private String createCallDetailsJson(boolean includeCallingNumber, Long callingNumber,
                                         boolean includeCallId, String callId,
                                         boolean includeOperator, String operator,
                                         boolean includeCircle, String circle,
                                         boolean includeCallStartTime, Long callStartTime,
                                         boolean includeCallEndTime, Long callEndTime,
                                         boolean includeCallDurationInPulses, Integer callDurationInPulses,
                                         boolean includeEndOfUsagePromptCounter, Integer endOfUsagePromptCounter,
                                         boolean includeWelcomeMessagePromptFlag, Boolean welcomeMessagePromptFlag,
                                         boolean includeCallStatus, Integer callStatus,
                                         boolean includeCallDisconnectReason, Integer callDisconnectReason,
                                         boolean includeContet, String content) {
        StringBuffer contentTemplate = new StringBuffer("{");
        ArrayList<String> array = new ArrayList<>();

        if (includeCallingNumber) {
            array.add(String.format("\"callingNumber\": %s", callingNumber));
        }

        if (includeCallId) {
            array.add(String.format("\"callId\": %s", callId));
        }

        if (includeOperator) {
            array.add(String.format("\"operator\": \"%s\"", operator));
        }

        if (includeCircle) {
            array.add(String.format("\"circle\": \"%s\"", circle));
        }

        if (includeCallStartTime) {
            array.add(String.format("\"callStartTime\": %s", callStartTime));
        }

        if (includeCallEndTime) {
            array.add(String.format("\"callEndTime\": %s", callEndTime));
        }

        if (includeCallDurationInPulses) {
            array.add(String.format("\"callDurationInPulses\": %s", callDurationInPulses));
        }

        if (includeEndOfUsagePromptCounter) {
            array.add(String.format("\"endOfUsagePromptCounter\": %s", endOfUsagePromptCounter));
        }

        if (includeWelcomeMessagePromptFlag) {
            array.add(String.format("\"welcomeMessagePromptFlag\": %s", welcomeMessagePromptFlag));
            System.out.println(welcomeMessagePromptFlag);
        }

        if (includeCallStatus) {
            array.add(String.format("\"callStatus\": %s", callStatus));
        }

        if (includeCallDisconnectReason) {
            array.add(String.format("\"callDisconnectReason\": %s", callDisconnectReason));
        }

        if (includeContet) {
            array.add(String.format("\"content\": [%s]", content));
        }

        contentTemplate.append(Joiner.on(",").join(array));
        contentTemplate.append("}");

        return contentTemplate.toString();
    }

    /**
     * overloaded method to pass all values as String data type i.e in double
     * quotes ""
     */
    private String createCallDetailsJson(boolean includeCallingNumber,
            String callingNumber, boolean includeCallId, String callId,
            boolean includeOperator, String operator, boolean includeCircle,
            String circle, boolean includeCallStartTime, String callStartTime,
            boolean includeCallEndTime, String callEndTime,
            boolean includeCallDurationInPulses, String callDurationInPulses,
            boolean includeEndOfUsagePromptCounter,
            String endOfUsagePromptCounter,
            boolean includeWelcomeMessagePromptFlag,
            String welcomeMessagePromptFlag, boolean includeCallStatus,
            String callStatus, boolean includeCallDisconnectReason,
            String callDisconnectReason, boolean includeContet, String content) {
        StringBuffer contentTemplate = new StringBuffer("{");
        ArrayList<String> array = new ArrayList<>();

        if (includeCallingNumber) {
            array.add(String.format("\"callingNumber\": \"%s\"", callingNumber));
        }

        if (includeCallId) {
            array.add(String.format("\"callId\": \"%s\"", callId));
        }

        if (includeOperator) {
            array.add(String.format("\"operator\": \"%s\"", operator));
        }

        if (includeCircle) {
            array.add(String.format("\"circle\": \"%s\"", circle));
        }

        if (includeCallStartTime) {
            array.add(String.format("\"callStartTime\": \"%s\"", callStartTime));
        }

        if (includeCallEndTime) {
            array.add(String.format("\"callEndTime\": \"%s\"", callEndTime));
        }

        if (includeCallDurationInPulses) {
            array.add(String.format("\"callDurationInPulses\": \"%s\"",
                    callDurationInPulses));
        }

        if (includeEndOfUsagePromptCounter) {
            array.add(String.format("\"endOfUsagePromptCounter\": \"%s\"",
                    endOfUsagePromptCounter));
        }

        if (includeWelcomeMessagePromptFlag) {
            array.add(String.format("\"welcomeMessagePromptFlag\": \"%s\"",
                    welcomeMessagePromptFlag));
        }

        if (includeCallStatus) {
            array.add(String.format("\"callStatus\": \"%s\"", callStatus));
        }

        if (includeCallDisconnectReason) {
            array.add(String.format("\"callDisconnectReason\": \"%s\"",
                    callDisconnectReason));
        }

        if (includeContet) {
            array.add(String.format("\"content\": [%s]", content));
        }

        contentTemplate.append(Joiner.on(",").join(array));
        contentTemplate.append("}");

        return contentTemplate.toString();
    }

    private HttpPost createCallDetailsPost(String serviceName,
                                   boolean includeCallingNumber, Long callingNumber,
                                   boolean includeCallId, String callId,
                                   boolean includeOperator, String operator,
                                   boolean includeCircle, String circle,
                                   boolean includeCallStartTime, Long callStartTime,
                                   boolean includeCallEndTime, Long callEndTime,
                                   boolean includeCallDurationInPulses, Integer callDurationInPulses,
                                   boolean includeEndOfUsagePromptCounter, Integer endOfUsagePromptCounter,
                                   boolean includeWelcomeMessagePromptFlag, Boolean welcomeMessagePromptFlag,
                                   boolean includeCallStatus, Integer callStatus,
                                   boolean includeCallDisconnectReason, Integer callDisconnectReason,
                                   boolean includeContet, String content) {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/api/%s/callDetails",
                TestContext.getJettyPort(), serviceName));
        String callDetailsJson = createCallDetailsJson(includeCallingNumber, callingNumber, includeCallId,
                callId, includeOperator, operator, includeCircle, circle, includeCallStartTime, callStartTime,
                includeCallEndTime, callEndTime, includeCallDurationInPulses, callDurationInPulses,
                includeEndOfUsagePromptCounter, endOfUsagePromptCounter, includeWelcomeMessagePromptFlag,
                welcomeMessagePromptFlag, includeCallStatus, callStatus, includeCallDisconnectReason,
                callDisconnectReason, includeContet, content);
        StringEntity params;
        try {
            params = new StringEntity(callDetailsJson);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("We're no expecting this kind of exception in ITs!", e);
        }
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");
        return httpPost;
    }

    /**
     * overloaded method to pass all values as String type
     */
    private HttpPost createCallDetailsPost(String serviceName,
            boolean includeCallingNumber, String callingNumber,
            boolean includeCallId, String callId, boolean includeOperator,
            String operator, boolean includeCircle, String circle,
            boolean includeCallStartTime, String callStartTime,
            boolean includeCallEndTime, String callEndTime,
            boolean includeCallDurationInPulses, String callDurationInPulses,
            boolean includeEndOfUsagePromptCounter,
            String endOfUsagePromptCounter,
            boolean includeWelcomeMessagePromptFlag,
            String welcomeMessagePromptFlag, boolean includeCallStatus,
            String callStatus, boolean includeCallDisconnectReason,
            String callDisconnectReason, boolean includeContet, String content) {
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/api/%s/callDetails",
                TestContext.getJettyPort(), serviceName));
        String callDetailsJson = createCallDetailsJson(includeCallingNumber,
                callingNumber, includeCallId, callId, includeOperator,
                operator, includeCircle, circle, includeCallStartTime,
                callStartTime, includeCallEndTime, callEndTime,
                includeCallDurationInPulses, callDurationInPulses,
                includeEndOfUsagePromptCounter, endOfUsagePromptCounter,
                includeWelcomeMessagePromptFlag, welcomeMessagePromptFlag,
                includeCallStatus, callStatus, includeCallDisconnectReason,
                callDisconnectReason, includeContet, content);
        StringEntity params;
        try {
            params = new StringEntity(callDetailsJson);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                    "We're no expecting this kind of exception in ITs!", e);
        }
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");
        return httpPost;
    }

    /**
     * method to pass all values as per data type mentioned in API
     */
    private String createContentJson(boolean includeType, String type,
                                     boolean includeMkCardCode, String mkCardCode,
                                     boolean includeContentName, String contentName,
                                     boolean includeContentFileName, String contentFileName,
                                     boolean includeStartTime, Long startTime,
                                     boolean includeEndTime, Long endTime,
                                     boolean includeCompletionFlag, Boolean completionFlag,
                                     boolean includeCorrectAnswerEntered, Boolean correctAnswerEntered) {
        StringBuffer contentTemplate = new StringBuffer("{");
        ArrayList<String> array = new ArrayList<>();

        if (includeType) {
            array.add(String.format("\"type\": \"%s\"", type));
        }

        if (includeMkCardCode) {
            array.add(String.format("\"mkCardCode\": \"%s\"", mkCardCode));
        }

        if (includeContentName) {
            array.add(String.format("\"contentName\": \"%s\"", contentName));
        }

        if (includeContentFileName) {
            array.add(String.format("\"contentFileName\": \"%s\"", contentFileName));
        }

        if (includeStartTime) {
            array.add(String.format("\"startTime\": %s", startTime));
        }

        if (includeEndTime) {
            array.add(String.format("\"endTime\": %s", endTime));
        }

        if (includeCompletionFlag) {
            array.add(String.format("\"completionFlag\": %s", completionFlag));
        }

        if (includeCorrectAnswerEntered) {
            array.add(String.format("\"correctAnswerEntered\": %s", correctAnswerEntered));
        }

        contentTemplate.append(Joiner.on(",").join(array));
        contentTemplate.append("}");

        return contentTemplate.toString();
    }

    /**
     * overloaded method to pass all values as String type
     */
    private String createContentJson(boolean includeType, String type,
            boolean includeMkCardCode, String mkCardCode,
            boolean includeContentName, String contentName,
            boolean includeContentFileName, String contentFileName,
            boolean includeStartTime, String startTime, boolean includeEndTime,
            String endTime, boolean includeCompletionFlag,
            String completionFlag, boolean includeCorrectAnswerEntered,
            String correctAnswerEntered) {
        StringBuffer contentTemplate = new StringBuffer("{");
        ArrayList<String> array = new ArrayList<>();

        if (includeType) {
            array.add(String.format("\"type\": \"%s\"", type));
        }

        if (includeMkCardCode) {
            array.add(String.format("\"mkCardCode\": \"%s\"", mkCardCode));
        }

        if (includeContentName) {
            array.add(String.format("\"contentName\": \"%s\"", contentName));
        }

        if (includeContentFileName) {
            array.add(String.format("\"contentFileName\": \"%s\"",
                    contentFileName));
        }

        if (includeStartTime) {
            array.add(String.format("\"startTime\":  \"%s\"", startTime));
        }

        if (includeEndTime) {
            array.add(String.format("\"endTime\":  \"%s\"", endTime));
        }

        if (includeCompletionFlag) {
            array.add(String.format("\"completionFlag\":  \"%s\"",
                    completionFlag));
        }

        if (includeCorrectAnswerEntered) {
            array.add(String.format("\"correctAnswerEntered\":  \"%s\"",
                    correctAnswerEntered));
        }

        contentTemplate.append(Joiner.on(",").join(array));
        contentTemplate.append("}");

        return contentTemplate.toString();
    }

    private String createSwcUserResponseJson(String defaultLanguageLocationCode, String locationCode,
                                             List<String> allowedLanguageLocations,
                                             Long currentUsageInPulses, Long endOfUsagePromptCounter,
                                             Boolean welcomePromptFlag, Integer maxAllowedUsageInPulses,
                                             Integer maxAllowedEndOfUsagePrompt) throws IOException {
        SwcUserResponse userResponse = new SwcUserResponse();
        if (defaultLanguageLocationCode != null) {
            userResponse.setDefaultLanguageLocationCode(defaultLanguageLocationCode);
        }
        if (locationCode != null) {
            userResponse.setLanguageLocationCode(locationCode);
        }
        if (allowedLanguageLocations != null) {
            userResponse.setAllowedLanguageLocationCodes(new TreeSet<String>(allowedLanguageLocations));
        }
        if (currentUsageInPulses != null) {
            userResponse.setCurrentUsageInPulses(currentUsageInPulses);
        }
        if (endOfUsagePromptCounter != null) {
            userResponse.setEndOfUsagePromptCounter(endOfUsagePromptCounter);
        }
        if (welcomePromptFlag != null) {
            userResponse.setWelcomePromptFlag(welcomePromptFlag);
        }
        if (maxAllowedUsageInPulses != null) {
            userResponse.setMaxAllowedUsageInPulses(maxAllowedUsageInPulses);
        }
        if (maxAllowedEndOfUsagePrompt != null) {
            userResponse.setMaxAllowedEndOfUsagePrompt(maxAllowedEndOfUsagePrompt);
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(userResponse);
    }

//    // verifyFT366
//    @Test
//    public void testCallDetailsValidMobileKunji() throws IOException, InterruptedException {
//
//        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
//        swc.setJobStatus(SwcJobStatus.ACTIVE);
//        swcService.add(swc);
//
//
//        ArrayList<String> array = new ArrayList<>();
//        array.add(createContentJson(false, null,                   // type
//                true, "a",                     // mkCardCode
//                true, "YellowFever",           // contentName
//                true, "Yellowfever.wav",       // contentFile
//                true, 1200000000l,             // startTime
//                true, 1222222221l,             // endTime
//                false, null,                   // completionFlag
//                false, null));                 // correctAnswerEntered
//        array.add(createContentJson(false, null,                   // type
//                true, "b",                     // mkCardCode
//                true, "Malaria",               // contentName
//                true, "Malaria.wav",           // contentFile
//                true, 1200000000l,             // startTime
//                true, 1222222221l,             // endTime
//                false, null,                   // completionFlag
//                false, null));                 // correctAnswerEntered
//
//        HttpPost httpPost = createCallDetailsPost("mobilekunji",
//                true, 9810320300l,       // callingNumber
//                true, VALID_CALL_ID,  // callId
//                true, "A",               // operator
//                true, "AP",              // circle
//                true, 1422879843l,       // callStartTime
//                true, 1422879903l,       // callEndTime
//                true, 60,                // callDurationInPulses
//                true, 0,                 // endOfUsagePromptCounter
//                true, true,              // welcomeMessagePromptFlag
//                true, 1,                 // callStatus
//                true, 1,                 // callDisconnectReason
//                true, Joiner.on(",").join(array));          // content
//
//        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));
//
//        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//
//        CallDetailRecord cdr = callDetailRecordService.getByCallingNumber(9810320300l);
//
//        assertNotNull(cdr);
//        assertEquals(VALID_CALL_ID, cdr.getCallId());
//        assertEquals(2, cdr.getContent().size());
//
//        assertEquals("YellowFever", cdr.getContent().get(0).getContentName());
//        assertNull(cdr.getContent().get(0).getType());
//        assertNull(cdr.getContent().get(0).getCompletionFlag());
//        assertEquals("Malaria", cdr.getContent().get(1).getContentName());
//        assertNull(cdr.getContent().get(1).getType());
//        assertNull(cdr.getContent().get(1).getCompletionFlag());
//
//        assertEquals(swc.getId(), ((Swachchagrahi) callDetailRecordDataService.getDetachedField(cdr,
//                "swachchagrahi")).getId());
//
//        transactionManager.commit(status);
//    }

    /**
     * verifyFT470, verifyFT489
     */
    @Test
    public void testCallDetailsValidWashAcademy() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ true, "lesson",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */false, null));
        array.add(createContentJson(/* type */ true, "question",
                /* mkCardCode */ false, null,
                /* contentName */ true, "chapter-01question-01",
                /* contentFile */ true, "ch1_q1.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
        /* correctAnswerEntered */true, false));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
        /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
        /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        CallDetailRecord cdr = callDetailRecordService.getByCallingNumber(9810320300l);
        // assert call detail record
        assertNotNull(cdr);
        assertEquals(9810320300l, cdr.getCallingNumber());
        assertEquals(VALID_CALL_ID, cdr.getCallId());
        assertEquals("A", cdr.getOperator());
        assertEquals("AP", cdr.getCircle());
        assertEquals(1422879843l, cdr.getCallStartTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(1422879903l, cdr.getCallEndTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(60, cdr.getCallDurationInPulses());
        assertEquals(1, cdr.getEndOfUsagePromptCounter());
        assertEquals(1, cdr.getFinalCallStatus().getValue());
        assertEquals(2, cdr.getCallDisconnectReason().getValue());
        assertEquals(2, cdr.getContent().size());

        // assert content data record
        CallContent cc = cdr.getContent().get(1);
        assertEquals("question", cc.getType());
        assertEquals("chapter-01question-01", cc.getContentName());
        assertEquals("ch1_q1.wav", cc.getContentFile());
        assertEquals(1200000000l, cc.getStartTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(1222222221l, cc.getEndTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(true, cc.getCompletionFlag());

        // TODO correctAnswerEntered assertion

        assertNull(cdr.getWelcomePrompt());
        assertNull(cc.getWashAcademyCardCode());

        assertEquals(swc.getId(), ((Swachchagrahi) callDetailRecordDataService.getDetachedField(cdr,
                "swachchagrahi")).getId());

        transactionManager.commit(status);
    }

    /**
     * verifyFT471 To check that call details of user is saved successfully
     * using Save Call Details API when optional parameter "content" is null.
     */
    @Test
    public void testCallDetailsValidNoContent() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        CallDetailRecord cdr = callDetailRecordService.getByCallingNumber(9810320300l);

        // assert call detail record
        assertNotNull(cdr);
        assertEquals(9810320300l, cdr.getCallingNumber());
        assertEquals(VALID_CALL_ID, cdr.getCallId());
        assertEquals("A", cdr.getOperator());
        assertEquals("AP", cdr.getCircle());
        assertEquals(1422879843l, cdr.getCallStartTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(1422879903l, cdr.getCallEndTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(60, cdr.getCallDurationInPulses());
        assertEquals(0, cdr.getEndOfUsagePromptCounter());
        assertEquals(1, cdr.getFinalCallStatus().getValue());
        assertEquals(1, cdr.getCallDisconnectReason().getValue());

        // assert content
        assertEquals(0, cdr.getContent().size());

        transactionManager.commit(status);
    }

    /*****************************************************************************************************************
     Test the existence and validity of elements common to MA/MK
     callingNumber, callId, operator, circle, callStartTime, callEndTime, callDurationInPulses, endOfUsagePromptCount,
     callStatus, callDisconnectReason
     ****************************************************************************************************************/
    @Test
    @Ignore
    public void testCallDetailsSWCNotFound() throws IOException, InterruptedException {

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        Swachchagrahi swc = swcService.getByContactNumber(9810320300l);
        assertEquals(SwachchagrahiStatus.ANONYMOUS, swc.getCourseStatus());
    }

    @Test
    public void testCallDetailsNullCallDisconnectReason() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ false, null,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDisconnectReason: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallStatus() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ false, null,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStatus: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullEndOfUsagePromptCount() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ false, null,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endOfUsagePromptCount: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallDurationInPulses() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ false, null,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDurationInPulses: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallEndTime() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ false, null,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callEndTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallStartTime() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ false, null,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStartTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallingNumber() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ false, null,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsInvalidCallingNumber() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallId() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ false, null,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsInvalidCallId() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID.substring(1),
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallId" is having invalid value i.e. "CallId" is having value greater
     * than 25 digit.
     * This is a silly test since testCallDetailsInvalidCallId already covers this. sigh.
     */
    @Test
    public void verifyFT491() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID + "12",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Invalid>\"}", ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallStatus" is having invalid value.
     */
    @Test
    public void verifyFT499() throws IOException, InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 4,
        /* callDisconnectReason */true, 1,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"4 is an invalid FinalCallStatus\"}",
                ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallDisconnectReason" is having invalid value.
     */
    @Test
    public void verifyFT500() throws IOException, InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 3,
        /* callDisconnectReason */true, 7,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"7 is an invalid CallDisconnectReason\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallStartTime" is having invalid format.
     */
    @Test
    public void verifyFT494() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, "9810320300",
        /* callId */true, "234000011111111",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, "142287984ss",// Invalid
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, "1",
        /* callDisconnectReason */true, "1",
        /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callStartTime.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallEndTime" is having invalid format.
     */
    @Test
    public void verifyFT495() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, "9810320300",
        /* callId */true, "234000011111111",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903sasa",//Invalid
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, "1",
        /* callDisconnectReason */true, "1",
        /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callEndTime.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallDurationPulses" is having invalid format.
     */
    @Test
     public void verifyFT496() throws IOException,
             InterruptedException {

         Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                 9810320300L);
         swcService.add(swc);

         HttpPost httpPost = createCallDetailsPost("washacademy",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "a6",// Invalid
         /* endOfUsagePromptCounter */true, "0",
         /* welcomeMessagePromptFlag */false, null,
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

         Pattern expectedJsonResponse = Pattern
                .compile(".*callDurationInPulses.*");

         assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                 expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

     }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "EndOfUsagePrompt" is having invalid format.
     */
    @Test
     public void verifyFT497() throws IOException,
             InterruptedException {

         Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                 9810320300L);
         swcService.add(swc);

         HttpPost httpPost = createCallDetailsPost("washacademy",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "a",// Invalid
         /* welcomeMessagePromptFlag */false, null,
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

         Pattern expectedJsonResponse = Pattern
                .compile(".*endOfUsagePromptCounter.*");

         assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                 expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

     }

    /*****************************************************************************************************************
     Test the existence and validity of elements specific to MA
     content.type, content.completionFlag
     *****************************************************************************************************************/
    @Test
    public void testCallDetailsNullContentType() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ false, null,
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */ true, true));
        array.add(createContentJson(/* type */ true, "question",
                /* mkCardCode */ false, null,
                /* contentName */ true, "chapter-01question-01",
                /* contentFile */ true, "ch1_q1.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */ true, true));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<type: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullContentCompletionFlag() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ true, "lesson",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ false, null,
                /* correctAnswerEntered */ false, null));
        array.add(createContentJson(/* type */ true, "question",
                /* mkCardCode */ false, null,
                /* contentName */ true, "chapter-01question-01",
                /* contentFile */ true, "ch1_q1.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ false, null,
                /* correctAnswerEntered */ false, null));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<completionFlag: Not Present><completionFlag: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>ContentName" is null
     */
    @Test
    public void verifyFT483() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */false, null,
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, 1200000000l,
        /* endTime */true, 1222222221l,
        /* completionFlag */true, true,
        /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentName: Not Present>\"}", ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>ContentFile" is null
     */
    @Test
    public void verifyFT484() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "Chapter-01lesson-04",
        /* contentFile */false, null,
        /* startTime */true, 1200000000l,
        /* endTime */true, 1222222221l,
        /* completionFlag */true, true,
        /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentFile: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>StartTime" is null
     */
    @Test
    public void verifyFT485() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "Chapter-01lesson-04",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */false, null,
        /* endTime */true, 1222222221l,
        /* completionFlag */true, true,
        /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<startTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>EndTime" is null
     */
    @Test
    public void verifyFT486() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "Chapter-01lesson-04",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, 1200000000l,
        /* endTime */false, null,
        /* completionFlag */true, true,
        /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>StartTime" is having invalid format.
     */
    @Test
    public void verifyFT503() throws InterruptedException, IOException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000ss",// invalid
        /* endTime */true, "1222222221",
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*startTime.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>EndTime" is having invalid format.
     */
    @Test
    public void verifyFT504() throws InterruptedException, IOException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "122ss2222221",// Invalid
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*endTime.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>type" is having invalid format.
     */
    // TODO https://applab.atlassian.net/browse/wa-232
    @Test
    public void verifyFT505() throws InterruptedException, IOException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "bookmark",// Type can be "lesson", "chapter", "question"
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "1222222221",
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*type.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * callData>> completionFlag" is having invalid format.
     */
    @Test
    public void verifyFT506() throws InterruptedException, IOException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "1222222221",
        /* completionFlag */true, "t1",// Invalid
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*completionFlag.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * callData>>correctAnswerEntered" is having invalid format.
     */
    @Test
    public void verifyFT507() throws InterruptedException, IOException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "1222222221",
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "10"));//Invalid
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*correctAnswerEntered.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /*****************************************************************************************************************
     Test the existence and validity of elements specific to MK
     welcomeMessagePromptFlag, content.mkCardCode
     *****************************************************************************************************************/
//    @Test
//    public void testCallDetailsNullWelcomeMessagePromptFlag() throws IOException, InterruptedException {
//        HttpPost httpPost = createCallDetailsPost("washacademy",
//                /* callingNumber */ true, 9810320300l,
//                /* callId */ true, VALID_CALL_ID,
//                /* operator */ true, "A",
//                /* circle */ true, "AP",
//                /* callStartTime */ true, 1422879903l,
//                /* callEndTime */ true, 1422879903l,
//                /* callDurationInPulses */ true, 60,
//                /* endOfUsagePromptCounter */ true, 0,
//                /* welcomeMessagePromptFlag */ false, null,
//                /* callStatus */ true, 1,
//                /* callDisconnectReason */ true, 1,
//                /* content */ false, null);
//
//        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
//                "{\"failureReason\":\"<welcomeMessagePromptFlag: Not Present>\"}",
//                ADMIN_USERNAME, ADMIN_PASSWORD));
//    }
//
//    @Test
//    public void testCallDetailsNullContentMkCardNumber() throws IOException, InterruptedException {
//        ArrayList<String> array = new ArrayList<>();
//        array.add(createContentJson(/* type */ false, null,
//                /* mkCardCode */ false, null,
//                /* contentName */ true, "Chapter-01lesson-04",
//                /* contentFile */ true, "ch1_l4.wav",
//                /* startTime */ true, 1200000000l,
//                /* endTime */ true, 1222222221l,
//                /* completionFlag */ false, null,
//                /* correctAnswerEntered */ false, null));
//        HttpPost httpPost = createCallDetailsPost("washacademy",
//                /* callingNumber */ true, 9810320300l,
//                /* callId */ true, VALID_CALL_ID,
//                /* operator */ true, "A",
//                /* circle */ true, "AP",
//                /* callStartTime */ true, 1422879843l,
//                /* callEndTime */ true, 1422879903l,
//                /* callDurationInPulses */ true, 60,
//                /* endOfUsagePromptCounter */ true, 0,
//                /* welcomeMessagePromptFlag */ true, false,
//                /* callStatus */ true, 1,
//                /* callDisconnectReason */ true, 1,
//                /* content */ true, Joiner.on(",").join(array));
//
//        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
//                "{\"failureReason\":\"<mkCardCode: Not Present>\"}",
//                ADMIN_USERNAME, ADMIN_PASSWORD));
//    }

    /**
     * To check that call details of user is saved successfully using Save Call
     * Details API when optional parameter "content" is null.
     */
    @Test
    public void verifyFT367() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        CallDetailRecord cdr = callDetailRecordService.getByCallingNumber(9810320300l);

        // assert call detail record
        assertNotNull(cdr);
        assertEquals(9810320300l, cdr.getCallingNumber());
        assertEquals(VALID_CALL_ID, cdr.getCallId());
        assertEquals("A", cdr.getOperator());
        assertEquals("AP", cdr.getCircle());
        assertEquals(1422879843l, cdr.getCallStartTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(1422879903l, cdr.getCallEndTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(60, cdr.getCallDurationInPulses());
        assertEquals(0, cdr.getEndOfUsagePromptCounter());
        assertEquals(1, cdr.getFinalCallStatus().getValue());
        assertEquals(1, cdr.getCallDisconnectReason().getValue());

        // assert content
        assertEquals(0, cdr.getContent().size());

        transactionManager.commit(status);

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallingNumber" is null
     */
    @Test
    public void verifyFT368() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ false, null,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallId" is null
     */
    @Test
    public void verifyFT369() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ false, null,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallStartTime" is null
     */
    @Test
    public void verifyFT372() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ false, "AP",
                /* callStartTime */ false, null,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertFalse(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStartTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallEndTime" is null
     */
    @Test
    public void verifyFT373() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ false, null,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callEndTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallDurationPulses" is null
     */
    @Test
    public void verifyFT374() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ false, null,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDurationInPulses: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "EndOfUsagePrompt" is null
     */
    @Test
    public void verifyFT375() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ false, null,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endOfUsagePromptCount: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "WelcomeMessageFlag" is null
     */
    @Test
    public void verifyFT376() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertFalse(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<welcomeMessagePromptFlag: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallStatus" is null
     */
    @Test
    public void verifyFT377() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ false, null,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStatus: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallDisconnectReason" is null
     */
    @Test
    public void verifyFT378() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ false, null,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDisconnectReason: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>ContentName" is null
     */
    @Test
    public void verifyFT379() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(true, "question",                   // type
                false, null,                     // mkCardCode
                false, null,           // contentName
                true, "Yellowfever.wav",       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                true, true,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentName: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>ContentFile" is null
     */
    @Test
    public void verifyFT380() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(true, "question",                   // type
                false, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                false, null,       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                true, true,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentFile: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>StartTime" is null
     */
    @Test
    public void verifyFT381() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(true, "question",                   // type
                false, null,                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "YellowFever.wav",       // contentFile
                false, null,             // startTime
                true, 1222222221l,             // endTime
                true, true,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<startTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>EndTime" is null
     */
    @Test
    public void verifyFT382() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(true, "question",                   // type
                false, null,                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "YellowFever.wav",       // contentFile
                true, 1222222220l,             // startTime
                false, null,             // endTime
                true, true,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>MKCardCode" is missing
     */
//    @Test
//    public void verifyFT383() throws IOException, InterruptedException {
//
//        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
//        swcService.add(swc);
//
//        ArrayList<String> array = new ArrayList<>();
//        array.add(createContentJson(true, "question",                   // type
//                false, null,                     // mkCardCode
//                true, "YellowFever",           // contentName
//                true, "YellowFever.wav",       // contentFile
//                true, 1222222220l,             // startTime
//                true, 1222222221l,             // endTime
//                false, null,                   // completionFlag
//                false, null));                 // correctAnswerEntered
//
//
//        HttpPost httpPost = createCallDetailsPost("washacademy",
//                /* callingNumber */ true, 1234567890L,
//                /* callId */ true, VALID_CALL_ID,
//                /* operator */ true, "A",
//                /* circle */ true, "AP",
//                /* callStartTime */ true, 1422879843l,
//                /* callEndTime */ true, 1422879903l,
//                /* callDurationInPulses */ true, 60,
//                /* endOfUsagePromptCounter */ true, 0,
//                /* welcomeMessagePromptFlag */ true, true,
//                /* callStatus */ true, 1,
//                /* callDisconnectReason */ true, 1,
//                /* content */ true, Joiner.on(",").join(array));
//
//        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
//                "{\"failureReason\":\"<mkCardCode: Not Present>\"}",
//                ADMIN_USERNAME, ADMIN_PASSWORD));
//    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallingNumber" is having invalid value.
     */
    @Test
    public void verifyFT384() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 12356789L,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallId" is having invalid value
     */
    @Test
    public void verifyFT385() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, VALID_CALL_ID.substring(5),
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallStartTime" is having invalid format.
     */
    @Test
    public void verifyFT388() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, "9810320300",
        /* callId */true, "234000011111111",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, "142287984ss",// Invalid
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",
        /* welcomeMessagePromptFlag */true, "true",
        /* callStatus */true, "1",
        /* callDisconnectReason */true, "1",
        /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callStartTime.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallEndTime" is having invalid format.
     */
    @Test
    public void verifyFT389() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, "9810320300",
        /* callId */true, "234000011111111",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903sasa",//Invalid
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",
        /* welcomeMessagePromptFlag */true, "true",
        /* callStatus */true, "1",
        /* callDisconnectReason */true, "1",
        /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callEndTime.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallDurationPulses" is having invalid format.
     */
    @Test
    public void verifyFT390() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "a6",// Invalid
         /* endOfUsagePromptCounter */true, "0",
         /* welcomeMessagePromptFlag */true, "true",
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callDurationInPulses.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "EndOfUsagePrompt" is having invalid format.
     */
    @Test
    public void verifyFT391() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "a",// Invalid
         /* welcomeMessagePromptFlag */true, "true",
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*endOfUsagePromptCounter.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "WelcomeMessageFlag" is having invalid value.
     */
    @Test
    public void verifyFT392() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",// Invalid
         /* welcomeMessagePromptFlag */true, "abc",
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallStatus" is having invalid value.
     */
    @Test
    public void verifyFT393() throws IOException, InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */true, true,
        /* callStatus */true, 4,
        /* callDisconnectReason */true, 1,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"4 is an invalid FinalCallStatus\"}",
                ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallDisconnectReason" is having invalid value.
     */
    @Test
    public void verifyFT394() throws IOException, InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */true, true,
        /* callStatus */true, 3,
        /* callDisconnectReason */true, 7,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"7 is an invalid CallDisconnectReason\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>StartTime" is having invalid format.
     */
    @Test
    public void verifyFT397() throws InterruptedException, IOException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */true, "a",
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000ss",// invalid
        /* endTime */true, "1222222221",
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */true, true,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*startTime.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>EndTime" is having invalid format.
     */
    @Test
    public void verifyFT398() throws InterruptedException, IOException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */true, "a",
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "122ss2222221",// Invalid
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("washacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, VALID_CALL_ID,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */true, true,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*endTime.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that current usage pulses is resetted after the end of month.
     */
    @Test
    public void verifyFT522() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        // SWC usage
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1200000000l);
        swc.setLanguage(rh.hindiLanguage());
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        // invoke get user detail API
        StringBuilder sb = new StringBuilder(String.format("http://localhost:%d/api/", TestContext.getJettyPort()));
        String sep = "";
        sb.append(String.format("%s/", "washacademy"));
        sb.append("user?");
        sb.append(String.format("callingNumber=%s", "1200000000"));
        sep = "&";
        sb.append(String.format("%scircle=%s", sep, rh.delhiCircle().getName()));
        sb.append(String.format("%scallId=%s", sep, VALID_CALL_ID));
        sb.append(String.format("%soperator=%s", sep, "OP"));
        HttpGet httpGet = new HttpGet(sb.toString());
        httpGet.addHeader("content-type", "application/json");
        String expectedJsonResponse = createSwcUserResponseJson(rh.hindiLanguage().getCode(), // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses=No capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));


        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(true, "lesson",                   // type
                true, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "Yellowfever.wav",       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                true, true,                   // completionFlag
                false, null));                 // correctAnswerEntered

        HttpPost httpPost = createCallDetailsPost("washacademy",
                true, 1200000000l,       // callingNumber
                true, VALID_CALL_ID,  // callId
                true, "OP",               // operator
                true, rh.delhiCircle().getName(),              // circle
                true, DateTime.now().getMillis()/1000,       // callStartTime
                true, 1422879903l,       // callEndTime
                true, 60,                // callDurationInPulses
                true, 0,                 // endOfUsagePromptCounter
                true, true,              // welcomeMessagePromptFlag
                true, 1,                 // callStatus
                true, 1,                 // callDisconnectReason
                true, Joiner.on(",").join(array));          // content

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));


        // invoke get user detail API
        sb = new StringBuilder(String.format("http://localhost:%d/api/", TestContext.getJettyPort()));
        sep = "";
        sb.append(String.format("%s/", "washacademy"));
        sb.append("user?");
        sb.append(String.format("callingNumber=%s", "1200000000"));
        sep = "&";
        sb.append(String.format("%scircle=%s", sep, rh.delhiCircle().getName()));
        sb.append(String.format("%scallId=%s", sep, VALID_CALL_ID));
        sb.append(String.format("%soperator=%s", sep, "OP"));
        httpGet = new HttpGet(sb.toString());
        httpGet.addHeader("content-type", "application/json");
        expectedJsonResponse = createSwcUserResponseJson(rh.hindiLanguage().getCode(), // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                60L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses=No capping
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that status of swc must be set to "Active" when user call first time and
     * its information exists in wa DB and status as "Inactive"
     */
    @Test
    public void verifyFT512() throws IOException, InterruptedException {
        rh.delhiState();
        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 1200000001L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setDistrict(rh.newDelhiDistrict());
        swc.setState(rh.delhiState());
        swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        // invoke get user detail API
        StringBuilder sb = new StringBuilder(String.format("http://localhost:%d/api/", TestContext.getJettyPort()));
        String sep = "";
        sb.append(String.format("%s/", "washacademy"));
        sb.append("user?");
        sb.append(String.format("callingNumber=%s", "1200000001"));
        sep = "&";
        sb.append(String.format("%scircle=%s", sep, rh.delhiCircle().getName()));
        sb.append(String.format("%scallId=%s", sep, VALID_CALL_ID));
        sb.append(String.format("%soperator=%s", sep, "OP"));
        HttpGet httpGet = new HttpGet(sb.toString());
        httpGet.addHeader("content-type", "application/json");
        String expectedJsonResponse = createSwcUserResponseJson(rh.hindiLanguage().getCode(), // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                0L, // currentUsageInPulses=updated
                0L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses=State capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(true, "lesson",                   // type
                true, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "Yellowfever.wav",       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                true, true,                   // completionFlag
                false, null));                 // correctAnswerEntered

        HttpPost httpPost = createCallDetailsPost("washacademy",
                true, 1200000001l,       // callingNumber
                true, VALID_CALL_ID,  // callId
                true, "A",               // operator
                true, "AP",              // circle
                true, DateTime.now().getMillis()/1000,       // callStartTime
                true, 1422879903l,       // callEndTime
                true, 60,                // callDurationInPulses
                true, 0,                 // endOfUsagePromptCounter
                true, true,              // welcomeMessagePromptFlag
                true, 1,                 // callStatus
                true, 1,                 // callDisconnectReason
                true, Joiner.on(",").join(array));          // content

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        swc = swcService.getByContactNumber(1200000001l);
        assertEquals(SwachchagrahiStatus.ACTIVE, swc.getCourseStatus());
    }

    /**
     * To verify that the swc status update audit record is created
     * along with the change of status of a swc from INACTIVE to ACTIVE
     * upon calling for first time.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void verifyFT514() throws IOException, InterruptedException{

        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 1200000001L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setDistrict(rh.newDelhiDistrict());
        swc.setState(rh.delhiState());
        swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        // invoke get user detail API
        StringBuilder sb = new StringBuilder(String.format("http://localhost:%d/api/", TestContext.getJettyPort()));
        String sep = "";
        sb.append(String.format("%s/", "washacademy"));
        sb.append("user?");
        sb.append(String.format("callingNumber=%s", "1200000001"));
        sep = "&";
        sb.append(String.format("%scircle=%s", sep, rh.delhiCircle().getName()));
        sb.append(String.format("%scallId=%s", sep, VALID_CALL_ID));
        sb.append(String.format("%soperator=%s", sep, "OP"));
        System.out.println(sb.toString());
        HttpGet httpGet = new HttpGet(sb.toString());
        httpGet.addHeader("content-type", "application/json");
        String expectedJsonResponse = createSwcUserResponseJson(rh.hindiLanguage().getCode(), // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                0L, // currentUsageInPulses=updated
                0L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses=State capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ true, "lesson",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */false, null));
        array.add(createContentJson(/* type */ true, "question",
                /* mkCardCode */ false, null,
                /* contentName */ true, "chapter-01question-01",
                /* contentFile */ true, "ch1_q1.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
        /* correctAnswerEntered */true, false));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 1200000001l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
        /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
        /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        swc = swcService.getByContactNumber(1200000001l);
        assertEquals(SwachchagrahiStatus.ACTIVE, swc.getCourseStatus());
        assertEquals(swcStatusUpdateAuditDataService.count(), 1l);
        List<SwcStatusUpdateAudit> swcStatusUpdateAuditList = swcStatusUpdateAuditDataService.findBySwcId(swc.getSwcId());
        assertEquals(swcStatusUpdateAuditList.size(), 1);

    }

    @Test
    public void testCallDetailscontentFileNull() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson( true, "question", false, null, true, "Chapter-01lesson-04",
                false, null,true, 1200000000l,
               true, 1222222221l,
                true, true,
               true, true));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                true, 9810320300l,
                 true, VALID_CALL_ID,
                 true, "A",
                 true, "AP",
                 true, 1422879843l,
                 true, 1422879903l,
                 true, 60,
                true, 1,
                false, null,
               true, 1,
                true, 2,
                 true, Joiner.on(",").join(array));

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpPost, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentFile: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    // Test with no content
    // Test with empty content
    // Test with invalid callingNumber
    // Test with null callingNumber
    // Test with no swc for callingNumber
    // Test with invalid callId
    // Test with missing callId


    @Test
    public void testCallDetailsCallIdMissing() throws IOException, InterruptedException {

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ false, "",
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */true, 2,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsCallingNumberMissing() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */false, "",
                /* callId */true, VALID_CALL_ID,
                /* operator */true, "A",
                /* circle */true, "AP",
                /* callStartTime */true, "1422879902",
                /* callEndTime */true, "1422879903",
                /* callDurationInPulses */true, "60",
                /* endOfUsagePromptCounter */true, "0",
                /* welcomeMessagePromptFlag */false, null,
                /* callStatus */true, "1",
                /* callDisconnectReason */true, "1",
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    @Test
    public void testCallDetailsCallStartTimeMissing() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */true, "9810320300",
                /* callId */true, VALID_CALL_ID,
                /* operator */true, "A",
                /* circle */true, "AP",
                /* callStartTime */false, "",
                /* callEndTime */true, "1422879903",
                /* callDurationInPulses */true, "60",
                /* endOfUsagePromptCounter */true, "0",
                /* welcomeMessagePromptFlag */false, null,
                /* callStatus */true, "1",
                /* callDisconnectReason */true, "1",
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStartTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailscallEndTimeMissing() throws IOException,
            InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */true, "9810320300",
                /* callId */true, VALID_CALL_ID,
                /* operator */true, "A",
                /* circle */true, "AP",
                /* callStartTime */true, "1422879902",
                /* callEndTime */false, "1422879903",
                /* callDurationInPulses */true, "60",
                /* endOfUsagePromptCounter */true, "0",
                /* welcomeMessagePromptFlag */false, null,
                /* callStatus */true, "1",
                /* callDisconnectReason */true, "1",
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callEndTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailscallDurationInPulsesMissing() throws IOException,
            InterruptedException {
    Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
            9810320300L);
    swcService.add(swc);

    HttpPost httpPost = createCallDetailsPost("washacademy",
            /* callingNumber */true, "9810320300",
            /* callId */true, VALID_CALL_ID,
            /* operator */true, "A",
            /* circle */true, "AP",
            /* callStartTime */true, "1422879902",
            /* callEndTime */true, "1422879903",
            /* callDurationInPulses */false, "",
            /* endOfUsagePromptCounter */true, "0",
            /* welcomeMessagePromptFlag */false, null,
            /* callStatus */true, "1",
            /* callDisconnectReason */true, "1",
            /* content */false, null);

    assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDurationInPulses: Not Present>\"}",
               ADMIN_USERNAME, ADMIN_PASSWORD));
}

    @Test
    public void testCallDetailsEndOfUsagePromptCountMissing() throws IOException,
            InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */true, "9810320300",
                /* callId */true, VALID_CALL_ID,
                /* operator */true, "A",
                /* circle */true, "AP",
                /* callStartTime */true, "1422879902",
                /* callEndTime */true, "1422879903",
                /* callDurationInPulses */true, "60",
                /* endOfUsagePromptCounter */false, "",
                /* welcomeMessagePromptFlag */false, null,
                /* callStatus */true, "1",
                /* callDisconnectReason */true, "1",
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endOfUsagePromptCount: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsCallStatusMissing() throws IOException,
            InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */true, "9810320300",
                /* callId */true, VALID_CALL_ID,
                /* operator */true, "A",
                /* circle */true, "AP",
                /* callStartTime */true, "1422879902",
                /* callEndTime */true, "1422879903",
                /* callDurationInPulses */true, "60",
                /* endOfUsagePromptCounter */true, "0",
                /* welcomeMessagePromptFlag */false, null,
                /* callStatus */false, "",
                /* callDisconnectReason */true, "1",
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStatus: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailscallDisconnectReasonMissing() throws IOException,
            InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright",
                9810320300L);
        swcService.add(swc);

        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */true, "9810320300",
                /* callId */true, VALID_CALL_ID,
                /* operator */true, "A",
                /* circle */true, "AP",
                /* callStartTime */true, "1422879902",
                /* callEndTime */true, "1422879903",
                /* callDurationInPulses */true, "60",
                /* endOfUsagePromptCounter */true, "0",
                /* welcomeMessagePromptFlag */false, null,
                /* callStatus */true, "1",
                /* callDisconnectReason */false, "",
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDisconnectReason: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDataTypeMissing() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ false, "",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */false, null));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpPost, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<type: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsContentNameMissing() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
                /* mkCardCode */false, null,
                /* contentName */false, "",
                /* contentFile */true, "ch1_q1.wav",
                /* startTime */true, 1200000000l,
                /* endTime */true, 1222222221l,
                /* completionFlag */true, true,
                /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpPost, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentName: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }
    @Test
    public void testCallDetailscontentFileMissing() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
                /* mkCardCode */false, null,
                /* contentName */true, "Chapter-01lesson-04",
                /* contentFile */false, "",
                /* startTime */true, 1200000000l,
                /* endTime */true, 1222222221l,
                /* completionFlag */true, true,
                /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpPost, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentFile: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }
    @Test
    public void testCallDataCompletionFlagMissing() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ true, "lesson",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ false, false,
                /* correctAnswerEntered */false, null));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpPost, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<completionFlag: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDataStartTimeMissing() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ true, "lesson",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ false, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */false, null));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpPost, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<startTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDataEndTimeMissing() throws IOException, InterruptedException {

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 9810320300L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ true, "lesson",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ false, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */false, null));
        HttpPost httpPost = createCallDetailsPost("washacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, VALID_CALL_ID,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpPost, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }
}
