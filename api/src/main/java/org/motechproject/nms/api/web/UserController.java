package org.motechproject.nms.api.web;

import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.nms.api.web.contract.SwcUserResponse;
import org.motechproject.nms.api.web.contract.UserResponse;
import org.motechproject.nms.api.web.domain.InactiveJobCallAudit;
import org.motechproject.nms.api.web.exception.NotAuthorizedException;
import org.motechproject.nms.api.web.exception.NotDeployedException;
import org.motechproject.nms.api.web.repository.InactiveJobCallAuditDataService;
import org.motechproject.nms.swc.domain.*;
import org.motechproject.nms.swc.service.SwcService;
import org.motechproject.nms.swc.service.ServiceUsageCapService;
import org.motechproject.nms.swc.service.ServiceUsageService;
import org.motechproject.nms.props.domain.Service;
import org.motechproject.nms.props.service.LogHelper;
import org.motechproject.nms.region.domain.Circle;
import org.motechproject.nms.region.domain.Language;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.service.CircleService;
import org.motechproject.nms.region.service.LanguageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashSet;
import java.util.Set;


@SuppressWarnings("PMD")
@Controller
public class UserController extends BaseController {

    public static final String SERVICE_NAME = "serviceName";


    @Autowired
    private SwcService swcService;

    @Autowired
    private ServiceUsageService serviceUsageService;

    @Autowired
    private ServiceUsageCapService serviceUsageCapService;

    @Autowired
    private CircleService circleService;

    @Autowired
    private LanguageService languageService;

    @Autowired
    private InactiveJobCallAuditDataService inactiveJobCallAuditDataService;


    /**
     * 2.2.1 Get User Details API
     * IVR shall invoke this API when to retrieve details specific to the user identified by callingNumber.
     * In case user specific details are not available in the database, the API will attempt to load system
     * defaults based on the operator and circle provided.
     * /api/washacademy/user?callingNumber=9999999900&operator=A&circle=AP&callId=123456789012345

     * 3.2.1 Get User Details API
     * IVR shall invoke this API when to retrieve details specific to the user identified by callingNumber.
     * In case user specific details are not available in the database, the API will attempt to load system
     * defaults based on the operator and circle provided.
     * /api/mobilekunji/user?callingNumber=9999999900&operator=A&circle=AP&callId=234000011111111
     *
     */
    @RequestMapping("/{serviceName}/user") // NO CHECKSTYLE Cyclomatic Complexity
    @ResponseBody
    @Transactional(noRollbackFor = NotAuthorizedException.class)
    public UserResponse getUserDetails(@PathVariable String serviceName,
                             @RequestParam(required = false) Long callingNumber,
                             @RequestParam(required = false) String operator,
                             @RequestParam(required = false) String circle,
                             @RequestParam(required = false) String callId) {

        log(String.format("REQUEST: /%s/user", serviceName), String.format(
                "callingNumber=%s, callId=%s, operator=%s, circle=%s",
                LogHelper.obscure(callingNumber), callId, operator, circle));

        StringBuilder failureReasons = validate(callingNumber, callId, operator, circle);
        if (failureReasons.length() > 0) {
            throw new IllegalArgumentException(failureReasons.toString());
        }

        Circle circleObj = circleService.getByName(circle);

        UserResponse user = null;

        /*
        Make sure the url the user hit corresponds to a service we are expecting
         */
        if (!(WASH_ACADEMY.equals(serviceName))) {
            failureReasons.append(String.format(INVALID, SERVICE_NAME));
        }

        if (failureReasons.length() > 0) {
            throw new IllegalArgumentException(failureReasons.toString());
        }

        /*
        Handle the SWC services
         */
        if (WASH_ACADEMY.equals(serviceName)) {
            user = getFrontLineWorkerResponseUser(serviceName, callingNumber, circleObj);
        }

          Language defaultLanguage = null;
        if (circleObj != null) {
            defaultLanguage = circleObj.getDefaultLanguage();
        }

        // If no circle was provided, or if the provided circle doesn't have a default language, use the national
        if (defaultLanguage == null) {
            defaultLanguage = languageService.getNationalDefaultLanguage();
        }

        if (defaultLanguage != null && user != null) {
            user.setDefaultLanguageLocationCode(defaultLanguage.getCode());
        }

        // If the user does not have a language location code we want to return the allowed language location
        // codes for the provided circle, or all if no circle was provided
        Set<Language> languages = new HashSet<>();
        if (user.getLanguageLocationCode() == null && circleObj != null) {
            languages = languageService.getAllForCircle(circleObj);
        }

        if (user.getLanguageLocationCode() == null && circleObj == null) {
            languages = languageService.getAll();
        }

        if (languages.size() > 0) {
            Set<String> allowedLanguageLocations = new HashSet<>();
            for (Language language : languages) {
                allowedLanguageLocations.add(language.getCode());
            }
            user.setAllowedLanguageLocationCodes(allowedLanguageLocations);
        }

        log(String.format("RESPONSE: /%s/user", serviceName), String.format("callId=%s, %s", callId, user.toString()));
        return user;
    }

      private UserResponse getFrontLineWorkerResponseUser(String serviceName, Long callingNumber, Circle circle) {
        SwcUserResponse user = new SwcUserResponse();
        Service service = getServiceFromName(serviceName);
        ServiceUsage serviceUsage = new ServiceUsage(null, service, 0, 0, false);
        Swachchagrahi swc = swcService.getByContactNumber(callingNumber);
//        if (swc == null) {
//            swc = swcService.getInctiveByContactNumber(callingNumber);
//        }

        if(swc == null && circle != null && swcService.isAnonymousAllowed()){
            swc = new Swachchagrahi(callingNumber, circle);
            swc.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
            swcService.add(swc);
        }

        State state = getStateForFrontLineWorker(swc, circle);

        if (state != null) {
            if (!serviceDeployedInUserState(service, state)) {
                throw new NotDeployedException(String.format(NOT_DEPLOYED, service));
            }
        } else {
            // If we have no state for the user see if the service is deployed in at least one state in the circle
            if (!serviceDeployedInCircle(service, circle)) {
                throw new NotDeployedException(String.format(NOT_DEPLOYED, service));
            }
        }

        if (WASH_ACADEMY.equals(serviceName)) {
            // make sure that swc is authorized to use MA
            restrictInactiveJobUserCheck(swc);
        }

        if (swc != null) {
            Language language = swc.getLanguage();
            if (null != language) {
                user.setLanguageLocationCode(language.getCode());
            }

            serviceUsage = serviceUsageService.getCurrentMonthlyUsageForSWCAndService(swc, service);

            if (!frontLineWorkerAuthorizedForAccess(swc, state)) {
                throw new NotAuthorizedException(String.format(NOT_AUTHORIZED, CALLING_NUMBER));
            }
        }

        ServiceUsageCap serviceUsageCap = serviceUsageCapService.getServiceUsageCap(state, service);
        user.setCurrentUsageInPulses(serviceUsage.getUsageInPulses());
        user.setEndOfUsagePromptCounter(serviceUsage.getEndOfUsage());
        user.setWelcomePromptFlag(serviceUsage.getWelcomePrompt());
        user.setMaxAllowedUsageInPulses(serviceUsageCap.getMaxUsageInPulses());
        user.setMaxAllowedEndOfUsagePrompt(2);

        return user;
    }

    private void restrictInactiveJobUserCheck(Swachchagrahi swc) {

        if (swc != null && swc.getJobStatus() == SwcJobStatus.INACTIVE) {
            inactiveJobCallAuditDataService.create(new InactiveJobCallAudit(DateUtil.now(), swc.getSwcId(), swc.getContactNumber()));
            throw new NotAuthorizedException(String.format(NOT_AUTHORIZED, CALLING_NUMBER));
        } else if (swc == null) {
            throw new NotAuthorizedException(String.format(NOT_AUTHORIZED, CALLING_NUMBER));
        }
    }

}
