package org.motechproject.wa.testing.it.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.testing.osgi.http.SimpleHttpClient;
import org.motechproject.testing.utils.TestContext;
import org.motechproject.wa.api.web.contract.UserLanguageRequest;
import org.motechproject.wa.props.domain.DeployedService;
import org.motechproject.wa.props.domain.Service;
import org.motechproject.wa.props.repository.DeployedServiceDataService;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.repository.*;
import org.motechproject.wa.region.service.DistrictService;
import org.motechproject.wa.region.service.LanguageService;
import org.motechproject.wa.swc.domain.ServiceUsageCap;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwachchagrahiStatus;
import org.motechproject.wa.swc.domain.SwcJobStatus;
import org.motechproject.wa.swc.repository.ServiceUsageCapDataService;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.testing.it.utils.RegionHelper;
import org.motechproject.wa.testing.service.TestingService;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class LanguageControllerBundleIT extends BasePaxIT {
    private static final String ADMIN_USERNAME = "motech";
    private static final String ADMIN_PASSWORD = "motech";

    private static final String VALID_CALL_ID = "1234567890123456789012345";

    @Inject
    SwcService swcService;

    @Inject
    LanguageDataService languageDataService;

    @Inject
    LanguageService languageService;

    @Inject
    ServiceUsageCapDataService serviceUsageCapDataService;

    @Inject
    StateDataService stateDataService;

    @Inject
    DeployedServiceDataService deployedServiceDataService;

    @Inject
    CircleDataService circleDataService;

    @Inject
    TestingService testingService;

    @Inject
    DistrictDataService districtDataService;

    @Inject
    DistrictService districtService;
    
    @Inject
    NationalDefaultLanguageDataService nationalDefaultLanguageDataService;

    private RegionHelper rh;

    @Before
    public void setupTestData() {

        testingService.clearDatabase();

        rh = new RegionHelper(languageDataService, languageService, circleDataService, stateDataService,
                districtDataService, districtService);
//        rh.newDelhiDistrict();  delhiCircle also creates the district
        rh.delhiCircle();

        // All 3 services deployed in DELHI
        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.WASH_ACADEMY));

        // Services not deployed in KARNATAKA
//        rh.bangaloreDistrict();  karnatakaCircle creates bangalore district
        rh.karnatakaCircle();
    }

    private void createSwcCappedServiceNoUsageNoLocationNoLanguage() {
        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 1111111111l);
        swcService.add(swc);

        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(null, Service.WASH_ACADEMY, 3600);
        serviceUsageCapDataService.create(serviceUsageCap);
    }

    @Test //getting 302 SC_REMOVED_TEMPORARILY
    public void testSetLanguageInvalidService() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/NO_SERVICE/languageLocationCode", TestContext.getJettyPort()));
        StringEntity params = new StringEntity("{\"callingNumber\":1111111111,\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":10}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<serviceName: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testSetLanguageMissingCallingNumber() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode", TestContext.getJettyPort()));
        StringEntity params = new StringEntity("{\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":10}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testSetLanguageInvalidCallingNumber() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode", TestContext.getJettyPort()));

        UserLanguageRequest request = new UserLanguageRequest(
                123L, //callingNumber
                VALID_CALL_ID, //callId
                "123"); //languageLocationCode
        String json = new ObjectMapper().writeValueAsString(request);
        StringEntity params = new StringEntity(json);
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testSetLanguageMissingCallId() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(
                String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                        TestContext.getJettyPort()));
        StringEntity params = new StringEntity("{\"callingNumber\":1111111111,\"languageLocationCode\":10}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testSetLanguageInvalidCallId() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(
                String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                        TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":abcdef,\"callId\":\"123456789012345\",\"languageLocationCode\":\"10\"}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST, ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }
//
    @Test
    public void testSetLanguageMissingLanguageLocationCode() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode", TestContext.getJettyPort()));
        StringEntity params = new StringEntity("{\"callingNumber\":1111111111,\"callId\":"+ VALID_CALL_ID + "}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
        		"{\"failureReason\":\"<languageLocationCode: Not Present>\"}",
        		ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testSetLanguageInvalidLanguageLocationCode() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode", TestContext.getJettyPort()));
        StringEntity params = new StringEntity("{\"callingNumber\":1111111111,\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":\"AA\"}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_NOT_FOUND,
        		"{\"failureReason\":\"<languageLocationCode: Not Found>\"}",
        		ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testSetLanguageNoSWC() throws IOException, InterruptedException {

        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode", TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1111111111,\"callId\":" + VALID_CALL_ID + ",\"languageLocationCode\":\""
                        + rh.hindiLanguage().getCode() + "\"}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK));

        Swachchagrahi swc = swcService.getByContactNumber(1111111111l);
        assertNotNull(swc);
        assertEquals(SwachchagrahiStatus.ANONYMOUS, swc.getCourseStatus());
        Language language = swc.getLanguage();
        assertNotNull(language);
        assertEquals("SWC Language Code", rh.hindiLanguage().getCode(),
                language.getCode());
    }

    @Test
    public void testSetLanguageLanguageNotFound() throws IOException, InterruptedException {
        createSwcCappedServiceNoUsageNoLocationNoLanguage();

        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode", TestContext.getJettyPort()));
        StringEntity params = new StringEntity("{\"callingNumber\":1111111111,\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":77}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_NOT_FOUND,
                "{\"failureReason\":\"<languageLocationCode: Not Found>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testSetLanguageUndeployedState() throws IOException, InterruptedException {
        createSwcCappedServiceNoUsageNoLocationNoLanguage();
//
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode", TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1111111111,\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":\""
                        + rh.tamilLanguage().getCode() + "\"}");
        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        // the WASH_ACADEMY service hasn't been deployed for KARNATAKA, so this request should fail
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        System.out.println(response.getEntity().getContent());
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
        assertEquals("{\"failureReason\":\"<WASH_ACADEMY: Not Deployed In State>\"}", EntityUtils.toString(response.getEntity()));

    }

    @Test
    public void testSetLanguageValid() throws IOException, InterruptedException {
        createSwcCappedServiceNoUsageNoLocationNoLanguage();

        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode", TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1111111111,\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":\""
                        + rh.hindiLanguage().getCode() + "\"}");

        httpPost.setEntity(params);

        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK));

        Swachchagrahi swc = swcService.getByContactNumber(1111111111l);
        Language language = swc.getLanguage();
        assertNotNull(language);
        assertEquals(SwachchagrahiStatus.ANONYMOUS, swc.getCourseStatus());
        assertEquals("SWC Language Code", rh.hindiLanguage().getCode(),
                language.getCode());
    }

    /**
     * To set the LanguageLocationCode of the user using Set User Language
     * Location Code API.
     */
    @Test
    public void verifyFT463() throws IOException, InterruptedException {
        // create SWC record
        Swachchagrahi swc = new Swachchagrahi(1111111112l);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService
                .getByContactNumber(1111111112l);
        assertEquals(null, swc.getLanguage());// No Language

        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1111111112,\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":\""
                        + rh.hindiLanguage().getCode() + "\"}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");
        
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        
        swc = swcService
                .getByContactNumber(1111111112l);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(rh.hindiLanguage().getCode(), swc.getLanguage().getCode());
    }

    /**
     * To check that Set User Language Location Code API is rejected when
     * mandatory parameter callingNumber is missing.
     */
    @Test
    public void verifyFT464() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":\""
                        + rh.hindiLanguage().getCode() + "\"}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To check that Set User Language Location Code API is rejected when
     * mandatory parameter callId is missing.
     */
    @Test
    public void verifyFT465() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1111111112,\"languageLocationCode\":\""
                        + rh.hindiLanguage().getCode() + "\"}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To check that Set User Language Location Code API is rejected when
     * mandatory parameter languageLocationCode is missing.
     */
    @Test
    public void verifyFT466() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1111111112,\"callId\":"+ VALID_CALL_ID + "}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<languageLocationCode: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To check that Set User Language Location Code API is rejected when
     * mandatory parameter callingNumber is having invalid value
     * i.e.callingNumber value less than 10 digit
     */
    @Test
    public void verifyFT467() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":123456789,\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":\""
                        + rh.hindiLanguage().getCode() + "\"}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To check that Set User Language Location Code API is rejected when
     * mandatory parameter callId is having invalid value. i.e.callId value
     * greater than 15 digit
     */
    @Test
    public void verifyFT468() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1234567890,\"callId\":1234567890123456,\"languageLocationCode\":\""
                        + rh.hindiLanguage().getCode() + "\"}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");
        
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To check that Set User Language Location Code API is rejected when
     * mandatory parameter languageLocationCode is having invalid value. i.e.
     * Language Location Code value languageLocationCode value that doesn’t
     * exist in wa DB.
     */
    @Test
    public void verifyFT469() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/motech-platform-server/module/api/washacademy/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1234567890,\"callId\":"+ VALID_CALL_ID + ",\"languageLocationCode\":\"TT\"}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_NOT_FOUND,
                "{\"failureReason\":\"<languageLocationCode: Not Found>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }
    
//    private void createSwcWithStatusAnonymous(){
//    	Language language = new Language("99", "Papiamento");
//        languageDataService.create(language);
//
//    	// create anonymous SWC record
//        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
//        swcService.add(swc);
//
//        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(null, Service.MOBILE_KUNJI, 3600);
//        serviceUsageCapDataService.create(serviceUsageCap);
//
//        nationalDefaultLanguageDataService.create(new NationalDefaultLanguage(language));
//
//    }
    
    /*
     * To set the LanguageLocationCode of the anonymous user using languageLocationCode API.
     */
//    @Test
//    public void verifyFT359() throws IOException, InterruptedException{
//    	createSwcWithStatusAnonymous();
//    	HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/api/mobilekunji/languageLocationCode", TestContext.getJettyPort()));
//        StringEntity params = new StringEntity("{\"callingNumber\":1111111111,\"callId\":"+ VALID_CALL_ID +",\"languageLocationCode\":99}");
//        httpPost.setEntity(params);
//
//        httpPost.addHeader("content-type", "application/json");
//
//        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK));
//
//        Swachchagrahi swc = swcService.getByContactNumber(1111111111l);
//        Language language = swc.getLanguage();
//        assertNotNull(language);
//        assertEquals(SwachchagrahiStatus.ANONYMOUS, swc.getCourseStatus());
//        assertEquals("SWC Language Code", "99", language.getCode());
//    }
}
