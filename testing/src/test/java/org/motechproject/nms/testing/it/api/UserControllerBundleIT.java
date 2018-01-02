package org.motechproject.nms.testing.it.api;

import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.nms.api.web.contract.BadRequest;
import org.motechproject.nms.api.web.contract.SwcUserResponse;
import org.motechproject.nms.api.web.contract.UserLanguageRequest;
import org.motechproject.nms.api.web.repository.AnonymousCallAuditDataService;
import org.motechproject.nms.api.web.repository.InactiveJobCallAuditDataService;
import org.motechproject.nms.swc.domain.Swachchagrahi;
import org.motechproject.nms.swc.domain.SwachchagrahiStatus;
import org.motechproject.nms.swc.domain.CallDetailRecord;
import org.motechproject.nms.swc.domain.SwcJobStatus;
import org.motechproject.nms.swc.domain.ServiceUsageCap;
import org.motechproject.nms.swc.domain.WhitelistEntry;
import org.motechproject.nms.swc.domain.WhitelistState;
import org.motechproject.nms.swc.repository.CallDetailRecordDataService;
import org.motechproject.nms.swc.repository.ServiceUsageCapDataService;
import org.motechproject.nms.swc.repository.WhitelistEntryDataService;
import org.motechproject.nms.swc.repository.WhitelistStateDataService;
import org.motechproject.nms.swc.repository.SwcDataService;
import org.motechproject.nms.swc.service.CallDetailRecordService;
import org.motechproject.nms.swc.service.SwcService;
import org.motechproject.nms.swc.service.ServiceUsageService;
import org.motechproject.nms.swc.service.WhitelistService;
import org.motechproject.nms.props.domain.DeployedService;
import org.motechproject.nms.props.domain.Service;
import org.motechproject.nms.props.repository.DeployedServiceDataService;
import org.motechproject.nms.region.domain.Circle;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.Language;
import org.motechproject.nms.region.domain.NationalDefaultLanguage;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.repository.CircleDataService;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.LanguageDataService;
import org.motechproject.nms.region.repository.NationalDefaultLanguageDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.service.DistrictService;
import org.motechproject.nms.region.service.LanguageService;
import org.motechproject.nms.testing.it.api.utils.ApiTestHelper;
import org.motechproject.nms.testing.it.api.utils.RequestBuilder;
import org.motechproject.nms.testing.it.utils.ApiRequestHelper;
import org.motechproject.nms.testing.it.utils.RegionHelper;
import org.motechproject.nms.testing.service.TestingService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.testing.osgi.http.SimpleHttpClient;
import org.motechproject.testing.utils.TestContext;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.motechproject.nms.testing.it.utils.RegionHelper.createDistrict;
import static org.motechproject.nms.testing.it.utils.RegionHelper.createState;


/**
 * Verify that User API is present and functional.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class UserControllerBundleIT extends BasePaxIT {
    private static final String ADMIN_USERNAME = "motech";
    private static final String ADMIN_PASSWORD = "motech";
    private static final String VALID_CALL_ID = "1234567890123456789012345";

    @Inject
    SwcService swcService;
    @Inject
    CallDetailRecordDataService callDetailRecordDataService;
    @Inject
    ServiceUsageCapDataService serviceUsageCapDataService;
    @Inject
    LanguageDataService languageDataService;
    @Inject
    StateDataService stateDataService;
    @Inject
    WhitelistEntryDataService whitelistEntryDataService;
    @Inject
    WhitelistStateDataService whitelistStateDataService;
    @Inject
    CircleDataService circleDataService;
    @Inject
    DistrictDataService districtDataService;
    @Inject
    DistrictService districtService;
    @Inject
    DeployedServiceDataService deployedServiceDataService;
    @Inject
    NationalDefaultLanguageDataService nationalDefaultLanguageDataService;
    @Inject
    TestingService testingService;
    @Inject
    LanguageService languageService;

    @Inject
    SwcDataService swcDataService;

    @Inject
    WhitelistService whitelistService;

    @Inject
    ServiceUsageService serviceUsageService;

    @Inject
    CallDetailRecordService callDetailRecordService;

    @Inject
    PlatformTransactionManager transactionManager;

    @Inject
    AnonymousCallAuditDataService anonymousCallAuditDataService;

    @Inject
    InactiveJobCallAuditDataService inactiveJobCallAuditDataService;

    public static final Long WHITELIST_CONTACT_NUMBER = 1111111111l;
    public static final Long NOT_WHITELIST_CONTACT_NUMBER = 9000000000l;

    private State whitelistState;
    private State nonWhitelistState;

    private RegionHelper rh;
   


    @Before
    public void setupTestData() {
        testingService.clearDatabase();

        rh = new RegionHelper(languageDataService, languageService, circleDataService, stateDataService,
                districtDataService, districtService);

      
    }


    
    private void createSwcCappedServiceNoUsageNoLocationNoLanguage() {

        Language language = new Language("99", "Papiamento");
        languageDataService.create(language);

        Swachchagrahi swc = new Swachchagrahi("Frank Lloyd Wright", 1111111111L);
        swcService.add(swc);

        Circle circle = new Circle("AA");
        circle.setDefaultLanguage(language);
        circleDataService.create(circle);

        District district = new District();
        district.setName("District 1");
        district.setRegionalName("District 1");
        district.setLanguage(language);
        district.setCircle(circle);
        district.setCode(1L);

        State state = new State();
        state.setName("State 1");
        state.setCode(1L);
        state.getDistricts().add(district);

        stateDataService.create(state);

        deployedServiceDataService.create(new DeployedService(state, Service.WASH_ACADEMY));
        deployedServiceDataService.create(new DeployedService(state, Service.MOBILE_KUNJI));

        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(null, Service.MOBILE_KUNJI, 3600);
        serviceUsageCapDataService.create(serviceUsageCap);
    }

    private void createSwcWithLanguageServiceUsageAndCappedService() {

        rh.delhiState();
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.WASH_ACADEMY));
        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(null, Service.MOBILE_KUNJI, 3600);
        serviceUsageCapDataService.create(serviceUsageCap);

        // A service record without endOfService and WelcomePrompt played
        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(1);
        cdr.setEndOfUsagePromptCounter(0);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);
    }

    private void createSwcWithLanguageFullServiceUsageAndCappedService() {

        rh.newDelhiDistrict();
        rh.southDelhiDistrict();
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.WASH_ACADEMY));
        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(null, Service.MOBILE_KUNJI, 3600);
        serviceUsageCapDataService.create(serviceUsageCap);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(1);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(true);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);
    }

    private void createSwcWithLanguageFullUsageOfBothServiceUncapped() {

        // Make sure to create two districts (with two languages) for the delhi state
        District district = rh.newDelhiDistrict();
        rh.southDelhiDistrict();
        // And a circle
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.WASH_ACADEMY));
        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = ApiTestHelper.createSwc("Hillary Devi", 1111111111L, "123", SwachchagrahiStatus.ACTIVE);
        swc.setLanguage(rh.hindiLanguage());
        swc.setDistrict(district);
        swc.setState(district.getState());
        swcService.add(swc);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(1);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(true);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        // Academy doesn't have a welcome prompt
        cdr = new CallDetailRecord();
        cdr.setCallingNumber(1111111111l);
        cdr.setSwachchagrahi(swc);
        cdr.setService(Service.WASH_ACADEMY);
        cdr.setCallDurationInPulses(1);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(null, Service.MOBILE_KUNJI, 10);
        serviceUsageCapDataService.create(serviceUsageCap);
    }

    private void createSwcWithStateNotInWhitelist() {

        circleDataService.create(new Circle("AA"));

        District district = new District();
        district.setName("9");
        district.setRegionalName("9");
        district.setCode(9l);

        State whitelistState = new State("WhitelistState", 1l);
        whitelistState.getDistricts().add(district);

        district.setState(whitelistState);

        stateDataService.create(whitelistState);

        deployedServiceDataService.create(new DeployedService(whitelistState, Service.WASH_ACADEMY));
        deployedServiceDataService.create(new DeployedService(whitelistState, Service.MOBILE_KUNJI));

        whitelistStateDataService.create(new WhitelistState(whitelistState));

        WhitelistEntry entry = new WhitelistEntry(0000000000l, whitelistState);
        whitelistEntryDataService.create(entry);

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111l);
        swc.setState(whitelistState);
        swc.setDistrict(district);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
    }

    private void createSwcWithLanguageLocationCodeNotInWhitelist() {

        Language language = new Language("34", "Language From Whitelisted State");
        languageDataService.create(language);

        Circle circle = new Circle("AA");
        circleDataService.create(circle);

        District district = new District();
        district.setName("District 1");
        district.setRegionalName("District 1");
        district.setLanguage(language);
        district.setCircle(circle);
        district.setCode(1L);

        State whitelist = new State();
        whitelist.setName("Whitelist");
        whitelist.setCode(1L);
        whitelist.getDistricts().add(district);

        stateDataService.create(whitelist);

        deployedServiceDataService.create(new DeployedService(whitelist, Service.WASH_ACADEMY));
        deployedServiceDataService.create(new DeployedService(whitelist, Service.MOBILE_KUNJI));


        whitelistStateDataService.create(new WhitelistState(whitelist));

        WhitelistEntry entry = new WhitelistEntry(0000000000l, whitelist);
        whitelistEntryDataService.create(entry);

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111l);
        swc.setLanguage(language);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
    }

    private void createCircleWithLanguage() {

        // Let's create a pretend circle with two states

        final Circle circle = new Circle("AA");
        circle.setDefaultLanguage(rh.hindiLanguage());
        circleDataService.create(circle);

        // Calling these will make sure the districts exist and will map the districts' language to their state
        District d = rh.newDelhiDistrict();
        d.setCircle(circle);
        circle.getDistricts().add(d);
        districtDataService.update(d);

        d = rh.mysuruDistrict();
        d.setCircle(circle);
        circle.getDistricts().add(d);
        districtDataService.update(d);

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        nationalDefaultLanguageDataService.create(new NationalDefaultLanguage(rh.hindiLanguage()));
    }

    private void createCircleWithSingleLanguage() {
        Language language = new Language("99", "Papiamento");
        languageDataService.create(language);

        Circle circle = new Circle("AA");
        circle.setDefaultLanguage(language);
        circleDataService.create(circle);

        District district = new District();
        district.setName("District 1");
        district.setRegionalName("District 1");
        district.setCode(1L);
        district.setCircle(circle);

        State state = new State();
        state.setName("State 1");
        state.setCode(1L);
        state.getDistricts().add(district);

        stateDataService.create(state);

        nationalDefaultLanguageDataService.create(new NationalDefaultLanguage(language));
    }

    private HttpGet createHttpGet(boolean includeService, String service,
                                  boolean includeCallingNumber, String callingNumber,
                                  boolean includeOperator, String operator,
                                  boolean includeCircle, String circle,
                                  boolean includeCallId, String callId) {

        StringBuilder sb = new StringBuilder(String.format("http://localhost:%d/api/", TestContext.getJettyPort()));
        String sep = "";
        if (includeService) {
            sb.append(String.format("%s/", service));
        }
        sb.append("user?");
        if (includeCallingNumber) {
            sb.append(String.format("callingNumber=%s", callingNumber));
            sep = "&";
        }
        if (includeOperator) {
            sb.append(String.format("%soperator=%s", sep, operator));
            sep = "&";
        }
        if (includeCircle) {
            sb.append(String.format("%scircle=%s", sep, circle));
            sep = "&";
        }
        if (includeCallId) {
            sb.append(String.format("%scallId=%s", sep, callId));
        }

        return new HttpGet(sb.toString());
    }

    private HttpPost createHttpPost(String service, UserLanguageRequest request) throws IOException {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/api/%s/languageLocationCode",
                TestContext.getJettyPort(), service));
        ObjectMapper mapper = new ObjectMapper();
        StringEntity params = new StringEntity(mapper.writeValueAsString(request));
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");
        return httpPost;
    }

       private SwcUserResponse createSwcUserResponse(String defaultLanguageLocationCode, String locationCode,
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
            userResponse.setAllowedLanguageLocationCodes(Sets.newLinkedHashSet(allowedLanguageLocations));
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

        return userResponse;
    }

    private String createSwcUserResponseJson(String defaultLanguageLocationCode, String locationCode,
                                             List<String> allowedLanguageLocations,
                                             Long currentUsageInPulses, Long endOfUsagePromptCounter,
                                             Boolean welcomePromptFlag, Integer maxAllowedUsageInPulses,
                                             Integer maxAllowedEndOfUsagePrompt) throws IOException {
        SwcUserResponse userResponse = createSwcUserResponse(defaultLanguageLocationCode, locationCode,
                                                             allowedLanguageLocations, currentUsageInPulses,
                endOfUsagePromptCounter, welcomePromptFlag, maxAllowedUsageInPulses, maxAllowedEndOfUsagePrompt);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(userResponse);
    }

    private String createFailureResponseJson(String failureReason)throws IOException {
        BadRequest badRequest = new BadRequest(failureReason);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(badRequest);
    }

    private void createSwcWithLanguageNoDeployedServices() {

        Language language = new Language("10", "English");
        languageDataService.create(language);

        Circle circle = new Circle("AA");
        circleDataService.create(circle);

        District district = new District();
        district.setName("District 1");
        district.setRegionalName("District 1");
        district.setLanguage(language);
        district.setCode(1L);
        district.setCircle(circle);

        State state = new State();
        state.setName("State 1");
        state.setCode(1L);
        state.getDistricts().add(district);

        stateDataService.create(state);

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(language);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
    }

    // Request undeployed service by language location
    @Test
    public void testUndeployedServiceByLanguageLocation() throws IOException, InterruptedException {
        createSwcWithLanguageNoDeployedServices();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                false, null,            //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<MOBILE_KUNJI: Not Deployed In State>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
    }

    private void createSwcWithLocationNoLanguageNoDeployedServices() {

        District district = new District();
        district.setName("District 1");
        district.setRegionalName("District 1");
        district.setCode(1L);

        State state = new State();
        state.setName("State 1");
        state.setCode(1L);
        state.getDistricts().add(district);

        stateDataService.create(state);

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setState(state);
        swc.setDistrict(district);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
    }

    // Request undeployed service by swc location
    @Test
    public void testUndeployedServiceByFLWLocation() throws IOException, InterruptedException {
        createSwcWithLocationNoLanguageNoDeployedServices();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                false, null,            //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<MOBILE_KUNJI: Not Deployed In State>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
    }

    // Request undeployed service by circle
    private void createSwcWithNoLocationNoLanguageNoDeployedServices() {

        rh.newDelhiDistrict();
        rh.delhiCircle();

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swcService.add(swc);
    }

    // Request undeployed service by swc location
    @Test
    public void testUndeployedServiceByCircleLocation() throws IOException, InterruptedException {
        createSwcWithNoLocationNoLanguageNoDeployedServices();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",                //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true, VALID_CALL_ID                 //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<MOBILE_KUNJI: Not Deployed In State>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
    }

      @Test
    public void testSwcUserRequestWithoutServiceUsage() throws IOException, InterruptedException {
        createSwcCappedServiceNoUsageNoLocationNoLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                "99",  //defaultLanguageLocationCode
                null,  //locationCode
                Collections.singletonList("99"), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                3600,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSwcUserRequestWithServiceUsageOnly() throws IOException, InterruptedException {
        createSwcWithLanguageServiceUsageAndCappedService();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",                //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true, VALID_CALL_ID                 //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                1L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                3600,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSwcUserRequestWithServiceUsageAndEndOfUsageAndWelcomeMsg() throws IOException, InterruptedException {
        createSwcWithLanguageFullServiceUsageAndCappedService();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",                //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true, VALID_CALL_ID                 //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                1L,    //currentUsageInPulses
                1L,    //endOfUsagePromptCounter
                true,  //welcomePromptFlag
                3600,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testInvalidServiceName() throws IOException, InterruptedException {
        HttpGet httpGet = createHttpGet(
                true, "INVALID!!!!",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<serviceName: Invalid>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

  
   
    @Test
    public void testNoCircle() throws IOException, InterruptedException {
        createCircleWithLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                false, null,            //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                null,  //locationCode
                Arrays.asList(rh.tamilLanguage().getCode(), rh.hindiLanguage().getCode(), rh.kannadaLanguage()
                        .getCode()), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

  

    // An FLW that does not exist
    @Test
    public void testGetUserDetailsUnknownUser() throws IOException, InterruptedException {
        createCircleWithLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111112",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                null,  //locationCode
                Arrays.asList(rh.hindiLanguage().getCode(), rh.kannadaLanguage()
                        .getCode()), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }


    @Test
    @Ignore  // Currently under discussion with IMI.  My preference would be for them to handle this case
    public void testGetUserDetailsUnknownUserCircleSingleLanguage() throws IOException, InterruptedException {
        createCircleWithSingleLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111112",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                "99",  //defaultLanguageLocationCode
                "99",  //locationCode
                null, // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));

        Swachchagrahi swc = swcService.getByContactNumber(1111111112l);
        assertNotNull(swc);
        Language language = swc.getLanguage();
        assertNotNull(language);
        assertEquals("FLW Language Code", rh.hindiLanguage().getCode(), language.getCode());
    }

    @Test
    public void testGetUserDetailsUnknownUserUnknownCircle() throws IOException, InterruptedException {
        createCircleWithLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111112",     //callingNumber
                true, "OP",             //operator
                false, "",              //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                null,  //locationCode
                Arrays.asList(rh.tamilLanguage().getCode(), rh.hindiLanguage().getCode(), rh.kannadaLanguage()
                        .getCode()), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    // An FLW with usage for both MA and MK
    @Test
    public void testGetUserDetailsUserOfBothServices() throws IOException, InterruptedException {
        createSwcWithLanguageFullUsageOfBothServiceUncapped();

        HttpGet httpGet = createHttpGet(
                true, "washacademy",              //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true, VALID_CALL_ID                 //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(),
                1L,    //currentUsageInPulses
                1L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    // An FLW with usage and a service with a cap
    @Test
    public void testGetUserDetailsServiceCapped() throws IOException, InterruptedException {
        createSwcWithLanguageFullUsageOfBothServiceUncapped();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",                //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true, VALID_CALL_ID                 //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(),
                1L,    //currentUsageInPulses
                1L,    //endOfUsagePromptCounter
                true,  //welcomePromptFlag
                10,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testGetUserNotInWhitelistByState() throws IOException, InterruptedException {
        createSwcWithStateNotInWhitelist();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<callingNumber: Not Authorized>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testGetUserNotInWhitelistByLanguageLocationCode() throws IOException, InterruptedException {
        createSwcWithLanguageLocationCodeNotInWhitelist();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID     //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<callingNumber: Not Authorized>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSetLanguageInvalidService() throws IOException, InterruptedException {
        HttpPost httpPost = createHttpPost("INVALID_SERVICE", new UserLanguageRequest(1111111111L, VALID_CALL_ID, "10"));

        String expectedJsonResponse = createFailureResponseJson("<serviceName: Invalid>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSetLanguageMissingCallingNumber() throws IOException, InterruptedException {
        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(null, VALID_CALL_ID, "10"));

        String expectedJsonResponse = createFailureResponseJson("<callingNumber: Not Present>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSetLanguageInvalidCallingNumber() throws IOException, InterruptedException {
        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(123L, VALID_CALL_ID, "10"));

        String expectedJsonResponse = createFailureResponseJson("<callingNumber: Invalid>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSetLanguageMissingCallId() throws IOException, InterruptedException {
        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1111111111L, null, "10"));

        String expectedJsonResponse = createFailureResponseJson("<callId: Not Present>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSetLanguageInvalidCallId() throws IOException, InterruptedException {
        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1111111111L, VALID_CALL_ID.substring(1), "10"));

        String expectedJsonResponse = createFailureResponseJson("<callId: Invalid>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSetLanguageMissingLanguageLocationCode() throws IOException, InterruptedException {
        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1111111111L, VALID_CALL_ID, null));

        String expectedJsonResponse = createFailureResponseJson("<languageLocationCode: Not Present>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSetLanguageInvalidJson() throws IOException, InterruptedException {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/api/mobilekunji/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":invalid,\"callId\":123456789012345,\"languageLocationCode\":\"10\"}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");

        Pattern expectedJsonPattern = Pattern.compile(".*Could not read JSON.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST, expectedJsonPattern,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testSetLanguageNoFLW() throws IOException, InterruptedException {
        createCircleWithLanguage();

        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1111111111L, VALID_CALL_ID,
                rh.hindiLanguage().getCode()));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost));

        Swachchagrahi swc = swcService.getByContactNumber(1111111111l);
        assertNotNull(swc);
        Language language = swc.getLanguage();
        assertNotNull(language);
        assertEquals("FLW Language Code", rh.hindiLanguage().getCode(), language.getCode());
    }

    @Test
    public void testSetLanguageLanguageNotFound() throws IOException, InterruptedException {
        createSwcCappedServiceNoUsageNoLocationNoLanguage();

        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1111111111L, VALID_CALL_ID, "77"));

        String expectedJsonResponse = createFailureResponseJson("<languageLocationCode: Not Found>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testSetLanguageValid() throws IOException, InterruptedException {
        createSwcCappedServiceNoUsageNoLocationNoLanguage();

        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1111111111L, VALID_CALL_ID, "99"));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK));

        Swachchagrahi swc = swcService.getByContactNumber(1111111111L);
        Language language = swc.getLanguage();
        assertNotNull(language);
        assertEquals("FLW Language Code", "99", language.getCode());
    }
 

    private void createCircleWithMultipleLanguages() {

        rh.bangaloreDistrict();
        rh.karnatakaState();
        rh.mysuruDistrict();

        Circle c = rh.karnatakaCircle();
        c.setDefaultLanguage(rh.kannadaLanguage());
        circleDataService.update(c);

       
        deployedServiceDataService.create(new DeployedService(rh.karnatakaState(), Service.WASH_ACADEMY));
        deployedServiceDataService.create(new DeployedService(rh.karnatakaState(), Service.MOBILE_KUNJI));
    }

   
    private void setupWhiteListData() {
        rh.newDelhiDistrict();
        rh.bangaloreDistrict();

        whitelistState = rh.delhiState();
        stateDataService.create(whitelistState);

        whitelistStateDataService.create(new WhitelistState(whitelistState));

        nonWhitelistState = rh.karnatakaState();
        stateDataService.create(nonWhitelistState);
    }

    /**
     * To verify Active/Inactive User should be able to access MK Service
     * content, if user's callingNumber is in whitelist and whitelist is set to
     * Enabled for user's state.
     */
    @Test
    public void verifyFT340() throws InterruptedException, IOException {
        setupWhiteListData();

        // create a FLW with whitelist number and whitelist state
        Swachchagrahi whitelistWorker = new Swachchagrahi("Test",
                WHITELIST_CONTACT_NUMBER);
        whitelistWorker.setState(whitelistState);
        whitelistWorker.setDistrict(rh.newDelhiDistrict());
        whitelistWorker.setLanguage(rh.hindiLanguage());
        whitelistWorker.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.INACTIVE,
                whitelistWorker.getCourseStatus());

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.MOBILE_KUNJI));

        // Create the whitelist number entry in whitelist table
        WhitelistEntry entry = new WhitelistEntry(WHITELIST_CONTACT_NUMBER,
                whitelistState);
        whitelistEntryDataService.create(entry);

        // Check the response
        HttpGet request = createHttpGet(true, "mobilekunji",
                true, String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A",
                true, rh.delhiCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());

        // Update user's status to active
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        whitelistWorker.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swcService.update(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ACTIVE, whitelistWorker.getCourseStatus());

        // Check the response
        request = createHttpGet(true, "mobilekunji", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A", true, rh
                        .delhiCircle().getName(), true, VALID_CALL_ID);
        httpResponse = SimpleHttpClient.httpRequestAndResponse(request,
                RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());
    }
    
    /**
     * To verify anonymous User belongs to a circle that has multiple states,
     * should be able to access MK Service content, if user's callingNumber is
     * in whitelist and whitelist is set to Enabled for user's state
     */
    @Test
    public void verifyFT341() throws InterruptedException, IOException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        setupWhiteListData();

        // Delhi circle has a state already, add one more
        Circle delhiCircle = rh.delhiCircle();

        State nws = createState(7L, "New State in delhi");
        stateDataService.create(nws);
        districtDataService.create(createDistrict(nws, 1L, "Circle", delhiCircle));

        // Create the whitelist number entry in whitelist table
        WhitelistEntry entry = new WhitelistEntry(WHITELIST_CONTACT_NUMBER,
                whitelistState);
        whitelistEntryDataService.create(entry);

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.MOBILE_KUNJI));

        transactionManager.commit(status);

        // Check the response
        HttpGet request = createHttpGet(true, "mobilekunji", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), false, "", false, "",
                true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());

        status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        Set<State> states = languageService.getAllStatesForLanguage(rh
                .hindiLanguage());
        assertEquals(1, states.size());

        transactionManager.commit(status);

        // create set Language location code request and check the response
        HttpPost postRequest = createHttpPost("mobilekunji",
                new UserLanguageRequest(WHITELIST_CONTACT_NUMBER,
                        VALID_CALL_ID, rh.hindiLanguage().getCode()));
        httpResponse = SimpleHttpClient.httpRequestAndResponse(postRequest,
                RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());

        // assert user's status
        Swachchagrahi whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ANONYMOUS,
                whitelistWorker.getCourseStatus());
    }

    /**
     * To verify Active/Inactive User shouldn't be able to access MK Service
     * content, if user's callingNumber is not in whitelist and whitelist is set
     * to Enabled for user's state.
     */
    @Test
    public void verifyFT342() throws InterruptedException, IOException {
        setupWhiteListData();

        // create a FLW with non-whitelist number and whitelist state
        Swachchagrahi notWhitelistWorker = ApiTestHelper.createSwc("Frank Llyod Wright", NOT_WHITELIST_CONTACT_NUMBER, "123", SwachchagrahiStatus.ACTIVE);
        notWhitelistWorker.setState(whitelistState);
        notWhitelistWorker.setDistrict(rh.newDelhiDistrict());
        notWhitelistWorker.setLanguage(rh.hindiLanguage());
        swcService.add(notWhitelistWorker);

        // assert user's status
        notWhitelistWorker = swcService
                .getByContactNumber(NOT_WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.INACTIVE,
                notWhitelistWorker.getCourseStatus());

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.MOBILE_KUNJI));

        // Check the response
        HttpGet request = createHttpGet(true, "mobilekunji",
                true, String.valueOf(NOT_WHITELIST_CONTACT_NUMBER), true, "A",
                true, rh.delhiCircle().getName(), true, VALID_CALL_ID);

        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine()
                .getStatusCode());

        // Update user's status
        notWhitelistWorker = swcService
                .getByContactNumber(NOT_WHITELIST_CONTACT_NUMBER);
        notWhitelistWorker.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swcService.update(notWhitelistWorker);

        // assert user's status
        notWhitelistWorker = swcService
                .getByContactNumber(NOT_WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ACTIVE,
                notWhitelistWorker.getCourseStatus());

        // Check the response
        request = createHttpGet(true, "mobilekunji", true,
                String.valueOf(NOT_WHITELIST_CONTACT_NUMBER), true, "A", true,
                rh.delhiCircle().getName(), true, VALID_CALL_ID);
        httpResponse = SimpleHttpClient.httpRequestAndResponse(request,
                RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify anonymous User belongs to a circle that has multiple states,
     * shouldn't be able to access MK Service content, if user's callingNumber
     * is not in whitelist and whitelist is set to Enabled for user's state.
     */
    @Test
    public void verifyFT343() throws InterruptedException, IOException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        setupWhiteListData();

        // Delhi circle has a state already, add one more
        Circle delhiCircle = rh.delhiCircle();
        State nws = createState(7L, "New State in delhi");
        stateDataService.create(nws);
        districtDataService.create(createDistrict(nws, 1L, "Circle", delhiCircle));

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.MOBILE_KUNJI));

        transactionManager.commit(status);

        // Check the response
        HttpGet request = createHttpGet(true, "mobilekunji", true,
                String.valueOf(NOT_WHITELIST_CONTACT_NUMBER), false, "", false,
                "", true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());


        status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        Set<State> states = languageService.getAllStatesForLanguage(rh
                .hindiLanguage());
        assertEquals(1, states.size());

        transactionManager.commit(status);

        // create set Language location code request and check the response
        HttpPost postRequest = createHttpPost("mobilekunji",
                new UserLanguageRequest(NOT_WHITELIST_CONTACT_NUMBER,
                        VALID_CALL_ID, rh.hindiLanguage().getCode()));
        httpResponse = SimpleHttpClient.httpRequestAndResponse(postRequest,
                RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify Active/Inactive User should be able to access MK Service
     * content, if user's callingNumber is in whitelist and whitelist is set to
     * disabled for user's state.
     */
    @Test
    public void verifyFT344() throws InterruptedException, IOException {
        setupWhiteListData();
        // user's number in whitelist, but state not whitelisted
        Swachchagrahi whitelistWorker =  ApiTestHelper.createSwc("Test", WHITELIST_CONTACT_NUMBER, "123", SwachchagrahiStatus.ACTIVE);
        whitelistWorker.setDistrict(rh.bangaloreDistrict());
        whitelistWorker.setLanguage(rh.kannadaLanguage());
        whitelistWorker.setState(nonWhitelistState);
        swcService.add(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.INACTIVE,
                whitelistWorker.getCourseStatus());

        // create user's number in whitelist entry table
        whitelistEntryDataService.create(new WhitelistEntry(
                WHITELIST_CONTACT_NUMBER, nonWhitelistState));

        // service deployed in user's state
        deployedServiceDataService.create(new DeployedService(
                nonWhitelistState, Service.MOBILE_KUNJI));

        // Check the response
        HttpGet request = createHttpGet(true, "mobilekunji",
                true, String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A",
                true, rh.karnatakaCircle().getName(), true, VALID_CALL_ID);

        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());

        // Update user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        whitelistWorker.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swcService.update(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ACTIVE, whitelistWorker.getCourseStatus());

        // Check the response
        request = createHttpGet(true, "mobilekunji", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A", true, rh
                        .karnatakaCircle().getName(), true, VALID_CALL_ID);

        httpResponse = SimpleHttpClient.httpRequestAndResponse(request,
                RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify anonymous User belongs to a circle that has multiple states,
     * should be able to access MK Service content, if user's callingNumber is
     * in whitelist and whitelist is set to disabled for user's state.
     */
    @Test
    public void verifyFT345() throws InterruptedException, IOException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        setupWhiteListData();

        // karnataka circle has a state already, add one more

        Circle karnatakaCircle = rh.karnatakaCircle();
        State s = createState(7L, "New State in Karnataka");
        stateDataService.create(s);
        districtDataService.create(createDistrict(s, 1L, "Circle", karnatakaCircle));

        // Create the whitelist number entry in whitelist table
        WhitelistEntry entry = new WhitelistEntry(WHITELIST_CONTACT_NUMBER,
                whitelistState);
        whitelistEntryDataService.create(entry);

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(
                nonWhitelistState, Service.MOBILE_KUNJI));

        transactionManager.commit(status);

        // Check the response
        HttpGet request = createHttpGet(true, "mobilekunji", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), false, "", false, "",
                true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());

        status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        Set<State> states = languageService.getAllStatesForLanguage(rh
                .tamilLanguage());
        assertEquals(1, states.size());

        transactionManager.commit(status);

        // create set Language location code request and check the response
        HttpPost postRequest = createHttpPost("mobilekunji",
                new UserLanguageRequest(WHITELIST_CONTACT_NUMBER,
                        VALID_CALL_ID, rh.tamilLanguage().getCode()));
        httpResponse = SimpleHttpClient.httpRequestAndResponse(postRequest,
                RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());

        // assert user's status
        Swachchagrahi whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ANONYMOUS,
                whitelistWorker.getCourseStatus());
    }

    /**
     * To verify anonymous User belongs to a circle that has single state,
     * should be able to access MK Service content, if user's callingNumber is
     * in whitelist and whitelist is set to Enabled for user's state.
     */
    @Test
    public void verifyFT346() throws InterruptedException, IOException {
        setupWhiteListData();

        Circle delhiCircle = circleDataService.findByName(rh.delhiCircle().getName());
        assertNotNull(delhiCircle);

        // Create the whitelist number entry in whitelist table
        WhitelistEntry entry = new WhitelistEntry(WHITELIST_CONTACT_NUMBER,
                whitelistState);
        whitelistEntryDataService.create(entry);

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.MOBILE_KUNJI));

        // Check the response
        HttpGet request = createHttpGet(true, "mobilekunji", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), false, "", true, rh
                        .delhiCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify anonymous User belongs to a circle that has single state,
     * shouldn't be able to access MK Service content, if user's callingNumber
     * is not in whitelist and whitelist is set to Enabled for user's state.
     */
    @Test
    public void verifyFT347() throws InterruptedException, IOException {
        setupWhiteListData();

        Circle delhiCircle = circleDataService.findByName(rh.delhiCircle().getName());
        assertNotNull(delhiCircle);

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.MOBILE_KUNJI));

        // Check the response
        HttpGet request = createHttpGet(true, "mobilekunji", true,
                String.valueOf(NOT_WHITELIST_CONTACT_NUMBER), false, "", true,
                rh.delhiCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        Set<State> states = languageService.getAllStatesForLanguage(rh.hindiLanguage());
        assertEquals(1, states.size());

        transactionManager.commit(status);

        // create set Language location code request and check the response
        HttpPost postRequest = createHttpPost("mobilekunji",
                new UserLanguageRequest(NOT_WHITELIST_CONTACT_NUMBER,
                        VALID_CALL_ID, rh.hindiLanguage().getCode()));
        httpResponse = SimpleHttpClient.httpRequestAndResponse(postRequest,
                RequestBuilder.ADMIN_USERNAME, RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify Active User should be able to access MA Service content, if
     * user's callingNumber is in whitelist and whitelist is set to Enabled for
     * user's state.
     */
    @Test
    public void verifyFT439() throws InterruptedException, IOException {
        setupWhiteListData();

        // create a FLW with whitelist number and whitelist state
        Swachchagrahi whitelistWorker = ApiTestHelper.createSwc("Frank Llyod Wright", WHITELIST_CONTACT_NUMBER, "123", SwachchagrahiStatus.ANONYMOUS);
        whitelistWorker.setState(whitelistState);
        whitelistWorker.setDistrict(rh.newDelhiDistrict());
        whitelistWorker.setLanguage(rh.hindiLanguage());
        swcService.add(whitelistWorker);

        // Update user's status to active
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        whitelistWorker.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swcService.update(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ACTIVE, whitelistWorker.getCourseStatus());

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.WASH_ACADEMY));

        // Create the whitelist number entry in whitelist table
        WhitelistEntry entry = new WhitelistEntry(WHITELIST_CONTACT_NUMBER,
                whitelistState);
        whitelistEntryDataService.create(entry);

        // Check the response
        HttpGet request = createHttpGet(true, "washacademy", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A", true, rh
                        .delhiCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify Inactive User, should be able to access MA Service content, if
     * user's callingNumber is in whitelist and whitelist is set to Enabled for
     * user's state.
     */
    @Test
    public void verifyFT440() throws InterruptedException, IOException {
        setupWhiteListData();

        // create a FLW with whitelist number and whitelist state
        Swachchagrahi whitelistWorker = ApiTestHelper.createSwc("Dingo Ate Baby", WHITELIST_CONTACT_NUMBER, "123", SwachchagrahiStatus.INACTIVE);
        whitelistWorker.setState(whitelistState);
        whitelistWorker.setDistrict(rh.newDelhiDistrict());
        whitelistWorker.setLanguage(rh.hindiLanguage());
        swcService.add(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.INACTIVE,
                whitelistWorker.getCourseStatus());

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.WASH_ACADEMY));

        // Create the whitelist number entry in whitelist table
        WhitelistEntry entry = new WhitelistEntry(WHITELIST_CONTACT_NUMBER,
                whitelistState);
        whitelistEntryDataService.create(entry);

        // Check the response
        HttpGet getRequest = createHttpGet(true, "washacademy", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A", true, rh
                        .delhiCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                getRequest, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify anonymous User belongs to a circle that has multiple states,
     * if user's callingNumber is in whitelist and whitelist is set to Enabled for user's state.
     * It still doesn't matter. Anonymous users CANNOT access MA
     */
    @Test
    public void verifyFT441() throws InterruptedException, IOException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        setupWhiteListData();

        // Delhi circle has a state already, add one more
        Circle delhiCircle = rh.delhiCircle();
        State s = createState(7L, "New State in delhi");
        stateDataService.create(s);
        districtDataService.create(createDistrict(s, 1L, "Circle", delhiCircle));


        // Create the whitelist number entry in whitelist table
        WhitelistEntry entry = new WhitelistEntry(WHITELIST_CONTACT_NUMBER,
                whitelistState);
        whitelistEntryDataService.create(entry);

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.WASH_ACADEMY));

        // Check the response
        HttpGet request = createHttpGet(true, "washacademy", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), false, "", true, "DE",
                true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine()
                .getStatusCode());

        transactionManager.commit(status);
    }

    /**
     * To verify Active User shouldn't be able to access MA Service content, if
     * user's callingNumber is not is whitelist and whitelist is set to Enabled
     * for user's state.
     */
    @Test
    public void verifyFT443() throws InterruptedException, IOException {
        setupWhiteListData();

        // create a FLW with non-whitelist number and whitelist state
        Swachchagrahi notWhitelistWorker = new Swachchagrahi("Test",
                NOT_WHITELIST_CONTACT_NUMBER);
        notWhitelistWorker.setState(whitelistState);
        notWhitelistWorker.setDistrict(rh.newDelhiDistrict());
        notWhitelistWorker.setLanguage(rh.hindiLanguage());
        notWhitelistWorker.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(notWhitelistWorker);

        // Update user's status
        notWhitelistWorker = swcService
                .getByContactNumber(NOT_WHITELIST_CONTACT_NUMBER);
        notWhitelistWorker.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swcService.update(notWhitelistWorker);

        // assert user's status
        notWhitelistWorker = swcService
                .getByContactNumber(NOT_WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ACTIVE,
                notWhitelistWorker.getCourseStatus());

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.WASH_ACADEMY));

        // Check the response
        HttpGet request = createHttpGet(true, "washacademy", true,
                String.valueOf(NOT_WHITELIST_CONTACT_NUMBER), true, "A", true,
                rh.delhiCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify Inactive User shouldn't be able to access MA Service content,
     * if user's callingNumber is not in whitelist and whitelist is set to
     * Enabled for user's state.
     */
    @Test
    public void verifyFT444() throws InterruptedException, IOException {
        setupWhiteListData();

        // create a FLW with non-whitelist number and whitelist state
        Swachchagrahi notWhitelistWorker = new Swachchagrahi("Test",
                NOT_WHITELIST_CONTACT_NUMBER);
        notWhitelistWorker.setState(whitelistState);
        notWhitelistWorker.setDistrict(rh.newDelhiDistrict());
        notWhitelistWorker.setLanguage(rh.hindiLanguage());
        notWhitelistWorker.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(notWhitelistWorker);

        // assert user's status
        notWhitelistWorker = swcService
                .getByContactNumber(NOT_WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.INACTIVE,
                notWhitelistWorker.getCourseStatus());

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.WASH_ACADEMY));

        // Check the response
        HttpGet request = createHttpGet(true, "washacademy", true,
                String.valueOf(NOT_WHITELIST_CONTACT_NUMBER), true, "A", true,
                rh.delhiCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine()
                .getStatusCode());
    }
    
    /**
     * To verify anonymous User belongs to a circle that has multiple states,
     * if user's callingNumber is not in whitelist and whitelist is set to Enabled for user's state.
     * It still doesn't matter. No anonymous users on MA service.
     */
    @Test
    public void verifyFT445() throws InterruptedException, IOException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        setupWhiteListData();

        // Delhi circle has a state already, add one more
        Circle delhiCircle = rh.delhiCircle();
        State s = createState(7L, "New State in delhi");
        stateDataService.create(s);
        districtDataService.create(createDistrict(s, 1L, "Circle", delhiCircle));

        // Deploy the service in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.WASH_ACADEMY));
        transactionManager.commit(status);

        // Check the response
        HttpGet request = createHttpGet(true, "washacademy", true,
                String.valueOf(NOT_WHITELIST_CONTACT_NUMBER), false, "", false,
                "",
                true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify Active User should be able to access MA Service content, if
     * user's callingNumber is in whitelist and whitelist is set to disabled for
     * user's state.
     */
    @Test
    public void verifyFT447() throws InterruptedException, IOException {
        setupWhiteListData();
        // user's no in whitelist, but state not whitelisted
        Swachchagrahi whitelistWorker = ApiTestHelper.createSwc("Baby Dingo", WHITELIST_CONTACT_NUMBER, "123", SwachchagrahiStatus.INACTIVE);
        whitelistWorker.setDistrict(rh.bangaloreDistrict());
        whitelistWorker.setLanguage(rh.kannadaLanguage());
        whitelistWorker.setState(nonWhitelistState);
        swcService.add(whitelistWorker);

        // Update user's status to active
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        whitelistWorker.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swcService.update(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ACTIVE, whitelistWorker.getCourseStatus());

        // create user's number in whitelist entry table
        whitelistEntryDataService.create(new WhitelistEntry(
                WHITELIST_CONTACT_NUMBER, nonWhitelistState));

        // service deployed in user's state
        deployedServiceDataService.create(new DeployedService(
                nonWhitelistState, Service.WASH_ACADEMY));

        // Check the response
        HttpGet request = createHttpGet(true, "washacademy", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A", true, rh
                        .karnatakaCircle().getName(), true, VALID_CALL_ID);

        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify Inactive User should be able to access MA Service content, if
     * user's callingNumber is in whitelist and whitelist is set to disabled for
     * user's state.
     */
    @Test
    public void verifyFT448() throws InterruptedException, IOException {
        setupWhiteListData();
        // user's no in whitelist, but state not whitelisted
        Swachchagrahi whitelistWorker = ApiTestHelper.createSwc("Kangaroo Jack", WHITELIST_CONTACT_NUMBER, "123", SwachchagrahiStatus.INACTIVE);
        whitelistWorker.setDistrict(rh.bangaloreDistrict());
        whitelistWorker.setLanguage(rh.kannadaLanguage());
        whitelistWorker.setState(nonWhitelistState);
        swcService.add(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.INACTIVE,
                whitelistWorker.getCourseStatus());

        // create user's number in whitelist entry table
        whitelistEntryDataService.create(new WhitelistEntry(
                WHITELIST_CONTACT_NUMBER, nonWhitelistState));

        // service deployed in user's state
        deployedServiceDataService.create(new DeployedService(
                nonWhitelistState, Service.WASH_ACADEMY));

        // Check the response
        HttpGet request = createHttpGet(true, "washacademy", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A", true, rh
                        .karnatakaCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());
    }

    /**
     * This case has been added to test the functionality mentioned in
     * NMS.GEN.FLW.008. The NMS system shall provide means to mark an FLW as
     * invalid using CSV upload. Once an FLW is marked invalid, any incoming
     * call with MSISDN that is same as that of invalid FLW shall be treated as
     * that of an anonymous caller.
     */
    // TODO JIRA issue: https://applab.atlassian.net/browse/NMS-236
    @Ignore
    @Test
    public void verifyStatusChangeFromInvalidToAnonymous()
            throws InterruptedException, IOException {
        setupWhiteListData();
        // user's no in whitelist, but state not whitelisted
        Swachchagrahi whitelistWorker = new Swachchagrahi("Test",
                WHITELIST_CONTACT_NUMBER);
        whitelistWorker.setDistrict(rh.newDelhiDistrict());
        whitelistWorker.setLanguage(rh.hindiLanguage());
        whitelistWorker.setState(whitelistState);
        swcService.add(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.INACTIVE,
                whitelistWorker.getCourseStatus());

        // Update user's status to active
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        whitelistWorker.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcService.update(whitelistWorker);

        // assert user's status
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.INVALID, whitelistWorker.getCourseStatus());

        // create user's number in whitelist entry table
        whitelistEntryDataService.create(new WhitelistEntry(
                WHITELIST_CONTACT_NUMBER, whitelistState));

        // service deployed in user's state
        deployedServiceDataService.create(new DeployedService(whitelistState,
                Service.WASH_ACADEMY));

        // Check the response
        HttpGet request = createHttpGet(true, "washacademy", true,
                String.valueOf(WHITELIST_CONTACT_NUMBER), true, "A", true, rh
                        .delhiCircle().getName(), true, VALID_CALL_ID);
        HttpResponse httpResponse = SimpleHttpClient.httpRequestAndResponse(
                request, RequestBuilder.ADMIN_USERNAME,
                RequestBuilder.ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine()
                .getStatusCode());

        // assert user's status, On issuing the getUserDetails API to a invalid
        // user, its status should get changed to anonymous
        whitelistWorker = swcService
                .getByContactNumber(WHITELIST_CONTACT_NUMBER);
        assertEquals(SwachchagrahiStatus.ANONYMOUS,
                whitelistWorker.getCourseStatus());
    }

    /**
     * To verify that MK service shall be allowed when cappingType is set to
     * "State Capping" having usage pluses remaining.
     * <p>
     * To verify that MK maxallowedUsageInPulses counter is set successfully.
     */
    @Test
    public void verifyFT329_427() throws IOException, InterruptedException {
        rh.delhiState();
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        // Set maxallowedUsageInPulses to 3800
        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(rh.delhiState(), Service.MOBILE_KUNJI, 3800);
        serviceUsageCapDataService.create(serviceUsageCap);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(1);
        cdr.setEndOfUsagePromptCounter(0);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",                //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true,VALID_CALL_ID                  //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                1L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                3800, // maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that MK service  shall allow unlimited usage when cappingType is set to "No Capping"  for
     * user who has not listened  welcome message completely.
     */
    @Test
    public void verifyFT332() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        rh.southDelhiDistrict();
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1111111111l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",                //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true, VALID_CALL_ID                 //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false,  //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that MK service  shall allow unlimited usage when cappingType is set to "No Capping"  for
     * user who has listened  welcome message completely earlier.
     */
    @Test
    public void verifyFT333() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        rh.southDelhiDistrict();
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1111111111l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(1);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(true);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",                //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true, VALID_CALL_ID                 //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                1L,    //currentUsageInPulses
                1L,    //endOfUsagePromptCounter
                true,  //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Anonymous user belongs to circle having one state should  be able to listen MK content and
     * service deployment status is set to deploy in that particular state.
     */
    @Test
    public void verifyFT334() throws IOException, InterruptedException {
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1111111111l, "123", SwachchagrahiStatus.ANONYMOUS);
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",                //service
                true, "1111111111",                 //callingNumber
                true, "OP",                         //operator
                true, rh.delhiCircle().getName(),   //circle
                true, VALID_CALL_ID                 //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false,  //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Anonymous user belongs to a circle having multiple states should  be able to listen
     * MK content and  service deploy status is set to deploy in that particular state.
     */
    @Test
    public void verifyFT335() throws IOException, InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swcService.add(swc);

        createCircleWithLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID     //callId
        );

        List<String> allowedLLCCodes = new ArrayList<>();
        allowedLLCCodes.add(rh.hindiLanguage().getCode());
        allowedLLCCodes.add(rh.kannadaLanguage().getCode());

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                null,  //locationCode
                allowedLLCCodes, // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false,  //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));

        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1111111111L, "1234567890123451234512345",
                rh.hindiLanguage().getCode()));

        response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }

    /**
     * To verify that Inactive user should  be able to listen MK content if service
     * deploy status is set to deploy in a particular state.
     */
    @Test
    public void verifyFT336_1() throws IOException, InterruptedException {
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, rh.delhiCircle().getName(),             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false,  //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Active user should  be able to listen MK content if service
     * deploy status is set to deploy in a particular state.
     */
    @Test
    public void verifyFT336_2() throws IOException, InterruptedException {
        rh.delhiCircle();

        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.MOBILE_KUNJI));

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, rh.delhiCircle().getName(),             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                rh.hindiLanguage().getCode(),  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false,  //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Anonymous user belonging to circle having one state should not be able to listen MK
     * content if service deploy status is set to not deploy in a particular state.
     */
    @Test
    public void verifyFT337() throws IOException, InterruptedException {
        rh.delhiCircle();

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, rh.delhiCircle().getName(),             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = "{\"failureReason\":\"<MOBILE_KUNJI: Not Deployed In State>\"}";

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Anonymous user belonging to circle having multiple state should not  be able to
     * listen MK content if service deploy status is set to not deploy in a particular state.
     */
    @Test
    public void verifyFT338() throws IOException, InterruptedException {
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swcService.add(swc);

        createCircleWithLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID //callId
        );

        List<String> allowedLLCCodes = new ArrayList<>();
        allowedLLCCodes.add(rh.hindiLanguage().getCode());
        allowedLLCCodes.add(rh.kannadaLanguage().getCode());

        String expectedJsonResponse = createSwcUserResponseJson(
            rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
            null,  //locationCode
            allowedLLCCodes, // allowedLanguageLocationCodes
            0L,    //currentUsageInPulses
            0L,    //endOfUsagePromptCounter
            false,  //welcomePromptFlag
            -1,  //maxAllowedUsageInPulses
            2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));

        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1111111111L, "1234567890123451234512345",
                rh.kannadaLanguage().getCode()));

        response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);

        expectedJsonResponse = "{\"failureReason\":\"<MOBILE_KUNJI: Not Deployed In State>\"}";

        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Inactive user should not  be able to listen MK content if service
     * deploy status is set to not deploy in a particular state.
     */
    @Test
    public void verifyFT339_1() throws IOException, InterruptedException {
        rh.delhiCircle();

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);
        swcService.add(swc);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, rh.delhiCircle().getName(),             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = "{\"failureReason\":\"<MOBILE_KUNJI: Not Deployed In State>\"}";

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Active user should not  be able to listen MK content if service
     * deploy status is set to not deploy in a particular state.
     */
    @Test
    public void verifyFT339_2() throws IOException, InterruptedException {
        rh.delhiCircle();

        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.hindiLanguage());
        swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swcService.add(swc);

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, rh.delhiCircle().getName(),             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = "{\"failureReason\":\"<MOBILE_KUNJI: Not Deployed In State>\"}";

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that getuserdetails API is rejected when mandatory parameter
     * callingNumber is missing.
     */
    @Test
    public void verifyFT456() throws IOException, InterruptedException {
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                false, null, // callingNumber missing
                true, "OP", // operator
                true, rh.delhiCircle().getName(),// circle
                true, VALID_CALL_ID // callId
        );
        String expectedJsonResponse = createFailureResponseJson("<callingNumber: Not Present>");
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that getuserdetails API is rejected when mandatory parameter
     * callId is missing.
     */
    @Test
    public void verifyFT457() throws IOException, InterruptedException {
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                true, "OP", // operator
                true, rh.delhiCircle().getName(),// circle
                false, null // callId Missing
        );
        String expectedJsonResponse = createFailureResponseJson("<callId: Not Present>");
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
                .getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that getuserdetails API is rejected when mandatory parameter
     * callingNumber is having invalid value
     */
    @Test
    public void verifyFT458() throws IOException, InterruptedException {
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "123456789", // callingNumber Invalid
                true, "OP", // operator
                true, rh.delhiCircle().getName(),// circle
                true, VALID_CALL_ID // callId
        );
        String expectedJsonResponse = createFailureResponseJson("<callingNumber: Invalid>");
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
                .getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that getuserdetails API is rejected when mandatory parameter
     * callId is having invalid value
     */
    @Test
    public void verifyFT460() throws IOException, InterruptedException {
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1234567890", // callingNumber Invalid
                true, "OP", // operator
                true, rh.delhiCircle().getName(),// circle
                true, "1234567890123456" // callId
        );
        String expectedJsonResponse = createFailureResponseJson("<callId: Invalid>");
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
                .getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }


    /**
     * To get the details of the inactive user using get user details API when
     * languageLocation code is retrieved based on state and district. FLW must
     * exist in system with status as Inactive
     */
    @Test
    public void verifyFT461() throws IOException, InterruptedException {
        // create Invalid FLW record
        Swachchagrahi swc = ApiTestHelper.createSwc("Baby Dingo", 1200000001l, "123", SwachchagrahiStatus.INACTIVE);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swcService.add(swc);

        // assert for FLW status
        swc = swcService.getByContactNumber(1200000001l);
        assertTrue(SwachchagrahiStatus.INACTIVE == swc.getCourseStatus());
        
        Circle circle = rh.karnatakaCircle();
        circle.setDefaultLanguage(rh.kannadaLanguage());
        circleDataService.update(circle);
        
        deployedServiceDataService.create(new
                DeployedService(rh.karnatakaState(), Service.WASH_ACADEMY));

        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000001", // callingNumber
                true, "OP", // operator
                true, circle.getName(),// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(rh
                .kannadaLanguage().getCode(), // defaultLanguageLocationCode=circle default
                rh.tamilLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }

    /**
     * To get the details of the active user using get user details API when
     * languageLocation code is retrieved based on state and district. FLW must
     * exist in system with status as active.
     */
    @Test
    public void verifyFT462() throws IOException, InterruptedException {
        // create anonymous FLW record
        Swachchagrahi swc = ApiTestHelper.createSwc("Frankie Dingo", 1200000001l, "123", SwachchagrahiStatus.ANONYMOUS);
        swcService.add(swc);

        // update FLW status to ACTIVE
        swc = swcService.getByContactNumber(1200000001l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swcService.update(swc);

        // assert for FLW status
        swc = swcService.getByContactNumber(1200000001l);
        assertTrue(SwachchagrahiStatus.ACTIVE == swc.getCourseStatus());

        Circle circle = rh.karnatakaCircle();
        circle.setDefaultLanguage(rh.kannadaLanguage());
        circleDataService.update(circle);

        deployedServiceDataService.create(new DeployedService(rh
                .karnatakaState(), Service.WASH_ACADEMY));

        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000001", // callingNumber
                true, "OP", // operator
                true, circle.getName(),// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(rh
                .kannadaLanguage().getCode(), // defaultLanguageLocationCode=circle
                                              // default
                rh.tamilLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }
    
    private void createCircleWithNoLanguage(){
    	Circle circle = new Circle("AA");
        circle.setDefaultLanguage(rh.hindiLanguage());
        circleDataService.create(circle);
        
        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(null, Service.MOBILE_KUNJI, 3600);
        serviceUsageCapDataService.create(serviceUsageCap);
        
    }
    
    private void createSwcWithStatusActive(){
    	// create anonymous FLW record
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        // update FLW status to ACTIVE
        swc = swcService.getByContactNumber(1111111111L);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swcService.update(swc);

        Circle circle = rh.karnatakaCircle();
        circle.setDefaultLanguage(rh.kannadaLanguage());
        circleDataService.update(circle);

        deployedServiceDataService.create(new DeployedService(rh
                .karnatakaState(), Service.MOBILE_KUNJI));
    }
    
    private void createSwcWithStatusInactive(){
    	// create Invalid FLW record
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1111111111L);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);

        Circle circle = rh.karnatakaCircle();
        circle.setDefaultLanguage(rh.kannadaLanguage());
        circleDataService.update(circle);
        
        deployedServiceDataService.create(new
        DeployedService(rh.karnatakaState(), Service.MOBILE_KUNJI));
    }
    
    /*
     * To get the details of the Anonymous user using getuserdetails API 
     * when circle sent in request is not mapped to any languageLocation.
     */
    @Test
    public void verifyFT349() throws IOException, InterruptedException {
        createCircleWithNoLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111112",     //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                null,  //locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                3600,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }
    
    /*
     * To get the details of the Anonymous user using getuserdetails API 
     * when circle sent in request is mapped to multiple languageLocationCodes
     */
    @Test
    public void verifyFT350() throws IOException, InterruptedException {
        createCircleWithMultipleLanguages();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1200000000",     //callingNumber
                true, "OP",             //operator
                true, "KA",             //circle
                true, VALID_CALL_ID //callId
        );

        SwcUserResponse expectedResponse = createSwcUserResponse(
                rh.kannadaLanguage().getCode(),  //defaultLanguageLocationCode
                null,  //locationCode
                Arrays.asList(rh.kannadaLanguage().getCode(), rh.tamilLanguage().getCode()), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        ObjectMapper mapper = new ObjectMapper();
        SwcUserResponse actual = mapper.readValue(EntityUtils
                .toString(response.getEntity()), SwcUserResponse.class);
        assertEquals(expectedResponse, actual);
    }
    
    /*
     * To get the details of the Anonymous user using getuserdetails API 
     * when circle and operator are missing.
     */
    @Test
    public void verifyFT351() throws IOException, InterruptedException {
    	//Used this method to set up mobile_kunji environment 
    	createCircleWithLanguage();
    	
        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",      //service
                true, "1111111112",       //callingNumber
                false, null,              //operator
                false, null,			  //circle
                true, VALID_CALL_ID   //callId
        );

        SwcUserResponse expectedResponse = createSwcUserResponse(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                null,  //locationCode
                Arrays.asList(rh.hindiLanguage().getCode(), rh.kannadaLanguage().getCode(), rh.tamilLanguage()
                        .getCode()), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );
        
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        ObjectMapper mapper = new ObjectMapper();
        SwcUserResponse actual = mapper.readValue(EntityUtils
                .toString(response.getEntity()), SwcUserResponse.class);
        assertEquals(expectedResponse, actual);
    }
    
    /*
     * To verify that getuserdetails API is rejected when mandatory parameter 
     * callingNumber is missing.
     */
    @Test
    public void verifyFT352() throws IOException, InterruptedException {
        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                false, null,            //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<callingNumber: Not Present>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }
    
    /*
     * To verify that getuserdetails API is rejected when mandatory parameter callId is missing.
     */
    @Test
    public void verifyFT353() throws IOException, InterruptedException {
    	//Used this method to set up mobile_kunji environment
    	createCircleWithLanguage();
    	
        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111", //callingNumber
                true, "OP",         //operator
                true, "AA",         //circle
                false, null         //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<callId: Not Present>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }
    
    /*
     * To verify that getuserdetails API is rejected when mandatory parameter 
     * callingNumber is having invalid value
     */
    @Test
    public void verifyFT354() throws IOException, InterruptedException {
        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111",       //callingNumber
                true, "OP",             //operator
                true, "AA",             //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = createFailureResponseJson("<callingNumber: Invalid>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }
    
    /*
     * To verify that getuserdetails API is rejected when optional parameter circle is having invalid value
     */
    @Test
    public void verifyFT355() throws IOException, InterruptedException {
    	//Used this method to set up mobile_kunji environment
    	createCircleWithLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, "Invalid",       //circle
                true, VALID_CALL_ID //callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                rh.hindiLanguage().getCode(),  //defaultLanguageLocationCode
                null,  //locationCode
                Arrays.asList(rh.tamilLanguage().getCode(), rh.hindiLanguage().getCode(), rh.kannadaLanguage().getCode()), // allowedLanguageLocationCodes
                0L,    //currentUsageInPulses
                0L,    //endOfUsagePromptCounter
                false, //welcomePromptFlag
                -1,  //maxAllowedUsageInPulses
                2      //maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }
    
    /*
     * To verify that getuserdetails API is rejected when mandatory parameter 
     * callId is having invalid value
     */
    @Test
    public void verifyFT356() throws IOException, InterruptedException {
    	//Used this method to set up mobile_kunji environment
    	createCircleWithLanguage();

        HttpGet httpGet = createHttpGet(
                true, "mobilekunji",    //service
                true, "1111111111",     //callingNumber
                true, "OP",             //operator
                true, "AP",       		//circle
                true, "22222222222222222" //callId
        );

        
        String expectedJsonResponse = createFailureResponseJson("<callId: Invalid>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));
    }
    
    /*
     * To get the details of the inactive user using getuserdetails API 
     * when languageLocation code is retrieved based on state and district.
     */
    @Test
    public void verifyFT357() throws IOException, InterruptedException {
    	createSwcWithStatusInactive();

        HttpGet httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1111111111", // callingNumber
                true, "OP", // operator
                true, "KA",// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(rh
                        .kannadaLanguage().getCode(), // defaultLanguageLocationCode=circle default
                rh.tamilLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

    }
    
    /*
     * To get the details of the active user using getuserdetails API 
     * when languageLocation code is retrieved based on state and district.
     */
    @Test
    public void verifyFT358() throws IOException, InterruptedException {
    	createSwcWithStatusActive();
    	
        HttpGet httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1111111111", // callingNumber
                true, "OP", // operator
                true, "KA",// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(rh
                .kannadaLanguage().getCode(), // defaultLanguageLocationCode=circle
                                              // default
                rh.tamilLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
        
    }

    /**
     * To verify that Anonymous user belonging to a circle having single state
     * gets 403 for trying to access MA
     */
    @Test
    public void verifyFT428() throws IOException, InterruptedException {

        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                true, "OP", // operator
                true, rh.delhiCircle().getName(),// circle
                true, VALID_CALL_ID // callId
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
    }

    /**
     * To verify that Anonymous user belongs to circle having one state
     * shouldn't be able to listen MA content when service deploy status is set
     * to not deploy in that particular state.
     */
    @Test
    public void verifyFT437() throws IOException, InterruptedException {

        rh.newDelhiDistrict();
        rh.delhiCircle();
        // service not deployed in delhi state

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                true, "OP", // operator
                true, rh.delhiCircle().getName(),// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createFailureResponseJson("<WASH_ACADEMY: Not Deployed In State>");
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify that Anonymous user belongs to a circle having multiple states
     * gets 403 when MA is deployed in state
     */
    @Test
    public void verifyFT429() throws IOException, InterruptedException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        // setup delhi circle with two states delhi and karnataka

        rh.newDelhiDistrict();
        Circle c = rh.delhiCircle();

        State s = createState(7L, "New State in delhi");
        stateDataService.create(s);
        districtDataService.create(createDistrict(s, 1L, "Circle", rh.tamilLanguage(), c));

        // service deployed only in delhi state
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        transactionManager.commit(status);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                true, "OP", // operator
                true, rh.delhiCircle().getName(),// circle
                true, VALID_CALL_ID // callId
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
    }

    // Verify if a circle has multiple states and the service is not deployed in any of them than the call
    // should be rejected
    @Test
    public void verifyNIP160() throws IOException, InterruptedException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        // setup delhi circle with two states delhi and karnataka

        rh.newDelhiDistrict();
        Circle c = rh.delhiCircle();

        State s = createState(7L, "New State in karnataka");
        stateDataService.create(s);
        districtDataService.create(createDistrict(s, 1L, "Circle", rh.tamilLanguage(), c));
        transactionManager.commit(status);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                true, "OP", // operator
                true, rh.delhiCircle().getName(),// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createFailureResponseJson("<MOBILE_KUNJI: Not Deployed In State>");
        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());

        // Invoke set LLC API
        // Set LLC for which service is not deployed i.e karnataka
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/api/mobilekunji/languageLocationCode",
                TestContext.getJettyPort()));
        StringEntity params = new StringEntity(
                "{\"callingNumber\":1200000000,\"callId\":" + VALID_CALL_ID + ",\"languageLocationCode\":\""
                        + rh.tamilLanguage().getCode() + "\"}");
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");

        response = SimpleHttpClient.httpRequestAndResponse(httpPost,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine()
                .getStatusCode());
    }

    /**
     * To verify that Anonymous user should not be able to listen MA content if
     * service deploy status is set to not deploy in a particular state.
     */
    @Test
    public void verifyFT433() throws IOException, InterruptedException {
        // setup karnataka state for which service is not deployed
        rh.bangaloreDistrict();

        // invoke get user detail API without circle and operator
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                true, "KA",// circle
                true, VALID_CALL_ID // callId
        );

        List<String> allowedLLCCodes = new ArrayList<>();
        allowedLLCCodes.add(rh.tamilLanguage().getCode());
        allowedLLCCodes.add(rh.kannadaLanguage().getCode());


        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
    }

    /**
     * To verify that Active user should be able to listen MA content if service
     * deploy status is set to deploy in a particular state.
     */
    @Test
    public void verifyFT430() throws IOException, InterruptedException {
        // add FLW with active status
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1200000000l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcDataService.create(swc);

        // service deployed in Karnataka State
        deployedServiceDataService.create(new DeployedService(rh
                .karnatakaState(), Service.WASH_ACADEMY));

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.tamilLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Inactive user should be able to listen MA content if
     * service deploy status is set to deploy in a particular state.
     */
    @Test
    public void verifyFT431() throws IOException, InterruptedException {
        // add FLW with In active status
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright",
                1200000000l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcDataService.create(swc);

        // service deployed in Karnataka State
        deployedServiceDataService.create(new DeployedService(rh
                .karnatakaState(), Service.WASH_ACADEMY));

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.tamilLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Invalid user should be able to listen MA content if
     * service deploy status is set to deploy in a particular state.
     *
     * RL: Invalid users are now handled the same as anonymous
     *     https://applab.atlassian.net/browse/NMS-236
     */
    @Test
    public void verifyFT432() throws IOException, InterruptedException {
        // add FLW with Invalid status
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1200000000l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(DateTime.now().minusDays(50));
        swcDataService.create(swc);

        // service deployed in Karnataka State
        deployedServiceDataService.create(new DeployedService(rh.karnatakaState(), Service.WASH_ACADEMY));

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        List<String> allowedLLCCodes = new ArrayList<>();
        allowedLLCCodes.add(rh.tamilLanguage().getCode());
        allowedLLCCodes.add(rh.kannadaLanguage().getCode());

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
    }

    /**
     * To verify that Active user should not be able to listen MA content if
     * service deploy status is set to not deploy in a particular state.
     */
    @Test
    public void verifyFT434() throws IOException, InterruptedException {
        // add FLW with active status
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright",
                1200000000l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcDataService.create(swc);

        // service not deployed in Karnataka State

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createFailureResponseJson("<WASH_ACADEMY: Not Deployed In State>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine()
                .getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Inactive user should not be able to listen MA content if
     * service deploy status is set to not deploy in a particular state.
     */
    @Test
    public void verifyFT435() throws IOException, InterruptedException {
        // add FLW with In active status
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright",
                1200000000l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcDataService.create(swc);

        // service not deployed in Karnataka State

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createFailureResponseJson("<WASH_ACADEMY: Not Deployed In State>");

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine()
                .getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that Invalid user should not be able to listen MA content if
     * service deploy status is set to not deploy in a particular state.
     *
     * RL: Invalid users are now treated the same as non-existent or Anonymous
     *     https://applab.atlassian.net/browse/NMS-236
     */
    @Test
    public void verifyFT436() throws IOException, InterruptedException {
        // add FLW with Invalid status
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1200000000l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(DateTime.now().minusDays(50));
        swcDataService.create(swc);

        // service not deployed in Karnataka State

        // invoke get user detail API without circle and operator
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                true, "KA",// circle
                true, VALID_CALL_ID // callId
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusLine().getStatusCode());
    }

    /**
     * To verify that MA service is accessible usage when cappingType is set to
     * "National Capping" having usage pulses remaining.
     * <p>
     * To verify that MA maxallowedUsageInPulses counter is set successfully.
     */
    @Test
    public void verifyFT421_480() throws IOException, InterruptedException {
        District district = rh.newDelhiDistrict();
        deployedServiceDataService.create(new DeployedService(district.getState(), Service.WASH_ACADEMY));

        // National Capping set maxallowedUsageInPulses
        ServiceUsageCap serviceUsageCap = new ServiceUsageCap(null,
                Service.WASH_ACADEMY, 5000);
        serviceUsageCapDataService.create(serviceUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Jingo Jango", 1111111111l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setDistrict(district);
        swc.setState(district.getState());
        swc.setLanguage(district.getLanguage());
        swcService.add(swc);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.WASH_ACADEMY);
        cdr.setCallDurationInPulses(80);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1111111111", // callingNumber
                false, null, // operator
                true, "DE",// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(
                "hi", // defaultLanguageLocationCode
                "hi", // locationCode
                null, // allowedLanguageLocationCodes
                80L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                5000, // maxAllowedUsageInPulses=National cap
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that MA service is accessible usage when cappingType is set to
     * "State Capping" having usage pulses remaining.
     */
    @Test
    public void verifyFT423() throws IOException, InterruptedException {
        District d = rh.newDelhiDistrict();
        deployedServiceDataService.create(new DeployedService(rh
                .delhiState(), Service.WASH_ACADEMY));
        
        // State Capping
        ServiceUsageCap stateUsageCap = new ServiceUsageCap(rh.delhiState(),
                Service.WASH_ACADEMY, 6000);
        serviceUsageCapDataService.create(stateUsageCap);
        
        //national capping
        ServiceUsageCap nationalUsageCap = new ServiceUsageCap(null,
                Service.WASH_ACADEMY, 5000);
        serviceUsageCapDataService.create(nationalUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setDistrict(d);
        swc.setState(d.getState());
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.WASH_ACADEMY);
        cdr.setCallDurationInPulses(80);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                80L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                6000, // maxAllowedUsageInPulses=State cap
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that MA service shall maintain the pulses consumed by FLW for
     * MA usage.
     */
    // TODO https://applab.atlassian.net/browse/NMS-241
    @Test
    public void verifyFT523() throws IOException, InterruptedException {
        District district = rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));
        
        // Create FLW with no usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Lol Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setDistrict(district);
        swc.setState(district.getState());
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);
        
        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
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

        // Invoke Save call Detail

        HttpPost httpPost = ApiRequestHelper.createCallDetailsPost(
                "washacademy",
        /* callingNumber */true, 1200000000l,
        /* callId */true, VALID_CALL_ID,
        /* operator */false, null,
        /* circle */false, null,

        // This test will fail if run within 5 minutes of midnight on the first of the month.  I'm ok with that.
        /* callStartTime */true, (DateTime.now().minusMinutes(5).getMillis() / 1000),
        /* callEndTime */true, (DateTime.now().getMillis() / 1000),

        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK,
                ADMIN_USERNAME, ADMIN_PASSWORD));

        CallDetailRecord cdr = callDetailRecordService
                .getByCallingNumber(1200000000l);

        // assert call detail record
        assertNotNull(cdr);
        assertEquals(1200000000l, cdr.getCallingNumber());
        assertEquals(60, cdr.getCallDurationInPulses());
        assertEquals(1, cdr.getEndOfUsagePromptCounter());

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                60L, // currentUsageInPulses=updated
                1L, // endOfUsagePromptCounter=updated
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
     * To verify that current usage pulses is resetted after the end of month.
     * For national capping
     */
    @Test
    public void verifyFT498() throws IOException, InterruptedException {
        District district = rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));
        
        // national capping
        ServiceUsageCap nationalUsageCap = new ServiceUsageCap(null,
                Service.WASH_ACADEMY, 500);
        serviceUsageCapDataService.create(nationalUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Lol Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setDistrict(district);
        swc.setState(district.getState());
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.WASH_ACADEMY);
        cdr.setCallDurationInPulses(80);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now().withDayOfMonth(1).withTimeAtStartOfDay());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                80L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                500, // maxAllowedUsageInPulses=National capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        // Update FLW usage to previous month last day time such that it is resetted now

        cdr.setCallStartTime(DateTime.now().withDayOfMonth(1).withTimeAtStartOfDay().minusMinutes(1));
        callDetailRecordDataService.update(cdr);

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses=updated
                0L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                500, // maxAllowedUsageInPulses=National capping
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that endOfusagePrompt counter incremented when cappingType is
     * set to "National Capping" having usage pulses exhausted.
     */
    @Test
    public void verifyFT422() throws IOException, InterruptedException {
        District d = rh.newDelhiDistrict();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        // national capping
        ServiceUsageCap nationalUsageCap = new ServiceUsageCap(null,
                Service.WASH_ACADEMY, 100);
        serviceUsageCapDataService.create(nationalUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setDistrict(d);
        swc.setState(d.getState());
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        // Assume IVR already played endo of usage 1 time.
        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1200000000l);
        cdr.setService(Service.WASH_ACADEMY);
        cdr.setCallDurationInPulses(110);// greater than max allowed pulses
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                110L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                100, // maxAllowedUsageInPulses=National capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        // Invoke Save call Detail by assuming IVR increment
        // endOfUsagePromptCounter 1

        HttpPost httpPost = ApiRequestHelper.createCallDetailsPost(
                "washacademy",
                /* callingNumber */true, 1200000000l,
                /* callId */true, VALID_CALL_ID,
                /* operator */false, null,
                /* circle */false, null,

                // This test will fail if run within 5 minutes of midnight on
                // the first of the month. I'm ok with that.
                /* callStartTime */true, (DateTime.now().minusMinutes(5)
                        .getMillis() / 1000),
                /* callEndTime */true, (DateTime.now().getMillis() / 1000),

                /* callDurationInPulses */true, 40,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */false, null,
                /* callStatus */true, 1,
                /* callDisconnectReason */true, 2,
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK,
                ADMIN_USERNAME, ADMIN_PASSWORD));

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                150L, // currentUsageInPulses=updated
                2L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                100, // maxAllowedUsageInPulses=National capping
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that endOfUsagePromptCounter is incremented when cappingType is
     * set to "State Capping" having usage pulses exhausted.
     */
    @Test
    public void verifyFT425() throws IOException, InterruptedException {
        District district = rh.newDelhiDistrict();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        // State Capping
        ServiceUsageCap stateUsageCap = new ServiceUsageCap(rh.delhiState(),
                Service.WASH_ACADEMY, 150);
        serviceUsageCapDataService.create(stateUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setDistrict(district);
        swc.setState(district.getState());
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        // Assume IVR already played endo of usage 1 time.
        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1200000000l);
        cdr.setService(Service.WASH_ACADEMY);
        cdr.setCallDurationInPulses(160);// greater than max allowed pulses
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                160L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                150, // maxAllowedUsageInPulses=State capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        // Invoke Save call Detail by assuming IVR increment
        // endOfUsagePromptCounter 1

        HttpPost httpPost = ApiRequestHelper.createCallDetailsPost(
                "washacademy",
                /* callingNumber */true, 1200000000l,
                /* callId */true, VALID_CALL_ID,
                /* operator */false, null,
                /* circle */false, null,

                // This test will fail if run within 5 minutes of midnight on
                // the first of the month. I'm ok with that.
                /* callStartTime */true, (DateTime.now().minusMinutes(5)
                        .getMillis() / 1000),
                /* callEndTime */true, (DateTime.now().getMillis() / 1000),

                /* callDurationInPulses */true, 40,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */false, null,
                /* callStatus */true, 1,
                /* callDisconnectReason */true, 2,
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK,
                ADMIN_USERNAME, ADMIN_PASSWORD));

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                200L, // currentUsageInPulses=updated
                2L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                150, // maxAllowedUsageInPulses=state capping
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that endOfusagePrompt counter incremented when cappingType is
     * set to "National Capping" having usage pulses exhausted.
     */
    @Test
    public void verifyFT327() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.MOBILE_KUNJI));

        // national capping for MOBILEKUNJI
        ServiceUsageCap nationalUsageCap = new ServiceUsageCap(null, Service.MOBILE_KUNJI, 100);
        serviceUsageCapDataService.create(nationalUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1200000000l, "123", SwachchagrahiStatus.ANONYMOUS);
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        // Assume IVR already played end of usage 1 time.
        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1200000000l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(110);// greater than max allowed pulses
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(true);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                110L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                true, // welcomePromptFlag
                100, // maxAllowedUsageInPulses=National capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse, EntityUtils.toString(response.getEntity()));

        // Invoke Save call Detail by assuming IVR increment
        // endOfUsagePromptCounter 1

        HttpPost httpPost = ApiRequestHelper.createCallDetailsPost(
                "mobilekunji",
                /* callingNumber */true, 1200000000l,
                /* callId */true, VALID_CALL_ID,
                /* operator */false, null,
                /* circle */false, null,

                // This test will fail if run within 5 minutes of midnight on
                // the first of the month. I'm ok with that.
                /* callStartTime */true, (DateTime.now().minusMinutes(5)
                        .getMillis() / 1000),
                /* callEndTime */true, (DateTime.now().getMillis() / 1000),

                /* callDurationInPulses */true, 40,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */true, true,
                /* callStatus */true, 1,
                /* callDisconnectReason */true, 2,
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK,
                ADMIN_USERNAME, ADMIN_PASSWORD));

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                150L, // currentUsageInPulses=updated
                2L, // endOfUsagePromptCounter=updated
                true, // welcomePromptFlag
                100, // maxAllowedUsageInPulses=National capping
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that endOfUsagePromptCounter is incremented when cappingType is
     * set to "State Capping" having usage pulses exhausted.
     */
    @Test
    public void verifyFT330() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.MOBILE_KUNJI));

        // State Capping
        ServiceUsageCap stateUsageCap = new ServiceUsageCap(rh.delhiState(),
                Service.MOBILE_KUNJI, 150);
        serviceUsageCapDataService.create(stateUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        // Assume IVR already played endo of usage 1 time.
        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1200000000l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(160);// greater than max allowed pulses
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(true);
        cdr.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                160L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                true, // welcomePromptFlag
                150, // maxAllowedUsageInPulses=State capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        // Invoke Save call Detail by assuming IVR increment
        // endOfUsagePromptCounter 1

        HttpPost httpPost = ApiRequestHelper.createCallDetailsPost(
                "mobilekunji",
                /* callingNumber */true, 1200000000l,
                /* callId */true, VALID_CALL_ID,
                /* operator */false, null,
                /* circle */false, null,

                // This test will fail if run within 5 minutes of midnight on
                // the first of the month. I'm ok with that.
                /* callStartTime */true, (DateTime.now().minusMinutes(5)
                        .getMillis() / 1000),
                /* callEndTime */true, (DateTime.now().getMillis() / 1000),

                /* callDurationInPulses */true, 40,
                /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */true, true,
                /* callStatus */true, 1,
                /* callDisconnectReason */true, 2,
                /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK,
                ADMIN_USERNAME, ADMIN_PASSWORD));

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                200L, // currentUsageInPulses=updated
                2L, // endOfUsagePromptCounter=updated
                true, // welcomePromptFlag
                150, // maxAllowedUsageInPulses=state capping
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that current usage pulses is resetted after the end of month.
     */
    @Test
    public void verifyFT328() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.MOBILE_KUNJI));

        // State Capping
        ServiceUsageCap stateUsageCap = new ServiceUsageCap(rh.delhiState(),
                Service.MOBILE_KUNJI, 150);
        serviceUsageCapDataService.create(stateUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(80);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(true);
        cdr.setCallStartTime(DateTime.now().withDayOfMonth(1).withTimeAtStartOfDay());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                80L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                true, // welcomePromptFlag
                150, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        // Update FLW usage to previous month last day time such that it is resetted now

        cdr.setCallStartTime(DateTime.now().withDayOfMonth(1).withTimeAtStartOfDay().minusMinutes(1));
        callDetailRecordDataService.update(cdr);

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses=updated
                0L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                150, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that current usage pulses is resetted after the end of month.
     */
    @Test
    public void verifyFT331() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.MOBILE_KUNJI));

        // National Capping
        ServiceUsageCap stateUsageCap = new ServiceUsageCap(null,
                Service.MOBILE_KUNJI, 150);
        serviceUsageCapDataService.create(stateUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Llyod Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.MOBILE_KUNJI);
        cdr.setCallDurationInPulses(80);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(true);
        cdr.setCallStartTime(DateTime.now().withDayOfMonth(1).withTimeAtStartOfDay());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                80L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                true, // welcomePromptFlag
                150, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        // Update FLW usage to previous month last day time such that it is resetted now

        cdr.setCallStartTime(DateTime.now().withDayOfMonth(1).withTimeAtStartOfDay().minusMinutes(1));
        callDetailRecordDataService.update(cdr);

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses=updated
                0L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                150, // maxAllowedUsageInPulses
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void verifyDenialWhenMctsSwcIdMissing() throws IOException, InterruptedException {
        District d = rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        // National Capping
        ServiceUsageCap stateUsageCap = new ServiceUsageCap(null,
                Service.MOBILE_KUNJI, 150);
        serviceUsageCapDataService.create(stateUsageCap);

        // FLW
        Swachchagrahi swc = ApiTestHelper.createSwc("Claire Underwood", 1200000000l, null, SwachchagrahiStatus.ACTIVE);
        swc.setLanguage(rh.hindiLanguage());
        swc.setDistrict(d);
        swc.setState(d.getState());
        swcService.add(swc);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                true, "DE",// circle
                true, VALID_CALL_ID // callId
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
    }

    /**
     * To verify that current usage pulses is resetted after the end of month.
     * For state capping.
     */
    @Test
    public void verifyFT534() throws IOException, InterruptedException {
        District district = rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.WASH_ACADEMY));

        // national capping
        ServiceUsageCap nationalUsageCap = new ServiceUsageCap(null,
                Service.WASH_ACADEMY, 500);
        serviceUsageCapDataService.create(nationalUsageCap);

        // State capping
        ServiceUsageCap stateUsageCap = new ServiceUsageCap(rh.delhiState(),
                Service.WASH_ACADEMY, 250);
        serviceUsageCapDataService.create(stateUsageCap);

        // FLW usage
        Swachchagrahi swc = ApiTestHelper.createSwc("Frank Lol Wright", 1200000000l, "123", SwachchagrahiStatus.ACTIVE);
        swc.setDistrict(district);
        swc.setState(district.getState());
        swc.setLanguage(rh.hindiLanguage());
        swcService.add(swc);

        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(1111111111l);
        cdr.setService(Service.WASH_ACADEMY);
        cdr.setCallDurationInPulses(80);
        cdr.setEndOfUsagePromptCounter(1);
        cdr.setWelcomePrompt(false);
        cdr.setCallStartTime(DateTime.now().withDayOfMonth(1)
                .withTimeAtStartOfDay());
        callDetailRecordDataService.create(cdr);

        // invoke get user detail API
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        String expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                80L, // currentUsageInPulses
                1L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                250, // maxAllowedUsageInPulses=State capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        // Update FLW usage to previous month last day time such that it is
        // resetted now

        cdr.setCallStartTime(DateTime.now().withDayOfMonth(1)
                .withTimeAtStartOfDay().minusMinutes(1));
        callDetailRecordDataService.update(cdr);

        // invoke get user detail API To check updated usage and prompt
        httpGet = createHttpGet(
                true, "washacademy",  // service
                true, "1200000000",     // callingNumber
                false, null,            // operator
                false, null,            // circle
                true, VALID_CALL_ID     //callId
        );

        expectedJsonResponse = createSwcUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses=updated
                0L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                250, // maxAllowedUsageInPulses=State capping
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verify that status of swc must be set to "Anonymous" when user call first time
     * and its information does not exist in NMS DB.
     */
    @Test
    public void verifyFT511() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.MOBILE_KUNJI));

        // invoke get user detail API To check updated usage and prompt
        HttpGet httpGet = createHttpGet(true, "mobilekunji", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                true, rh.delhiCircle().getName(),// circle
                true, VALID_CALL_ID //callId
        );
        List<String> allowedLLCCodes = new ArrayList<>();
        allowedLLCCodes.add(rh.hindiLanguage().getCode());

        String expectedJsonResponse = createSwcUserResponseJson(rh.hindiLanguage().getCode(), // defaultLanguageLocationCode
                null, // locationCode
                allowedLLCCodes, // allowedLanguageLocationCodes
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

        HttpPost httpPost = createHttpPost("mobilekunji", new UserLanguageRequest(1200000000L, "1234567890123451234512345",
                rh.hindiLanguage().getCode()));

        response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Swachchagrahi swc = swcService.getByContactNumber(1200000000l);
        assertEquals(SwachchagrahiStatus.ANONYMOUS, swc.getCourseStatus());
    }

    /** To verify if the anonymous call audit is done if an anonymous user
     * attempted to call - two users are tested.
     */

    @Test
    public void verifyAnonymousCallAuditRecord() throws IOException, InterruptedException{
        // add FLW with Anonymous status
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1200000000l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
        swc.setInvalidationDate(DateTime.now().minusDays(50));
        swcDataService.create(swc);

        // add another FLW with Anonymous status
        Swachchagrahi swc1 = new Swachchagrahi("Aisha Bibi", 1234567899l);
        swc1.setLanguage(rh.tamilLanguage());
        swc1.setDistrict(rh.southDelhiDistrict());
        swc1.setState(rh.delhiState());
        swc1.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
        swc1.setInvalidationDate(DateTime.now().minusDays(50));
        swcDataService.create(swc1);


        // service deployed in Karnataka State
        deployedServiceDataService.create(new DeployedService(rh.karnatakaState(), Service.WASH_ACADEMY));
        // service deployed in Delhi State
        deployedServiceDataService.create(new DeployedService(rh.delhiState(), Service.WASH_ACADEMY));

        // invoke get user detail API for first swc user
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        // invoke get user detail API for second swc user
        HttpGet httpGet1 = createHttpGet(true, "washacademy", // service
                true, "1234567899", // callingNumber
                false, null, // operator
                true, "DE",// circle
                true, VALID_CALL_ID // callId
        );


        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);

        // checking if record is created for first swc anonymous call
        assertEquals(anonymousCallAuditDataService.count(),1l);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());

        HttpResponse response1 = SimpleHttpClient.httpRequestAndResponse(
                httpGet1, ADMIN_USERNAME, ADMIN_PASSWORD);

        // checking if record is created for first swc anonymous call
        assertEquals(anonymousCallAuditDataService.count(),2l);
        assertEquals(HttpStatus.SC_FORBIDDEN, response1.getStatusLine().getStatusCode());

        HttpResponse response2 = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);

        // checking if record is created for first swc's second anonymous call
        assertEquals(anonymousCallAuditDataService.count(),3l);
        assertEquals(anonymousCallAuditDataService.findByNumber(1200000000l).size(), 2);
        assertEquals(HttpStatus.SC_FORBIDDEN, response2.getStatusLine().getStatusCode());

    }

    /** To verify if the inactive job  call audit is done if an inactive job user
     * attempted to call.
     */

    @Test
    public void verifyInactiveJobUserCallAuditRecord() throws IOException, InterruptedException {

        // add FLW with Anonymous status
        Swachchagrahi swc = new Swachchagrahi("Frank Llyod Wright", 1200000000l);
        swc.setLanguage(rh.tamilLanguage());
        swc.setDistrict(rh.bangaloreDistrict());
        swc.setState(rh.karnatakaState());
        swc.setInvalidationDate(DateTime.now().minusDays(50));
        swc.setJobStatus(SwcJobStatus.INACTIVE);
        swcDataService.create(swc);


        // service deployed in Karnataka State
        deployedServiceDataService.create(new DeployedService(rh.karnatakaState(), Service.WASH_ACADEMY));


        // invoke get user detail API for first swc user
        HttpGet httpGet = createHttpGet(true, "washacademy", // service
                true, "1200000000", // callingNumber
                false, null, // operator
                false, null,// circle
                true, VALID_CALL_ID // callId
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);

        assertEquals(inactiveJobCallAuditDataService.findByNumber(swc.getContactNumber()).size(), 1);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());


    }

}
