package org.motechproject.wa.api.web;

import org.motechproject.wa.api.web.contract.UserLanguageRequest;
import org.motechproject.wa.api.web.exception.NotAuthorizedException;
import org.motechproject.wa.api.web.exception.NotDeployedException;
import org.motechproject.wa.api.web.exception.NotFoundException;
import org.motechproject.wa.props.domain.Service;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.service.LanguageService;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwcJobStatus;
import org.motechproject.wa.swc.service.SwcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Controller
public class LanguageController extends BaseController {

    public static final String LANGUAGE_LOCATION_CODE = "languageLocationCode";
    public static final String SERVICE_NAME = "serviceName";

    @Autowired
    private SwcService swcService;

    @Autowired
    private LanguageService languageService;

    /**
     * 2.2.7 Set User Language Location Code API
     * IVR shall invoke this API to provide user languageLocation preference to MoTech.
     * /api/washacademy/languageLocationCode
     *
     * 3.2.3 Set User Language Location Code API
     * IVR shall invoke this API to set the language location code of the user in wa database.
     * /api/mobilekunji/languageLocationCode
     *
     */
    @RequestMapping(value = "/{serviceName}/languageLocationCode", // NO CHECKSTYLE Cyclomatic Complexity
            method = RequestMethod.POST,
            headers = { "Content-type=application/json" })
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void setUserLanguageLocationCode(@PathVariable String serviceName,
                                            @RequestBody UserLanguageRequest userLanguageRequest) {


        log(String.format("REQUEST: /%s/languageLocationCode (POST)", serviceName), LogHelper.nullOrString(userLanguageRequest));

        Long callingNumber = userLanguageRequest.getCallingNumber();
        String callId = userLanguageRequest.getCallId();
        String languageLocationCode = userLanguageRequest.getLanguageLocationCode();

        StringBuilder failureReasons = validate(callingNumber, callId);
        validateFieldPresent(failureReasons, LANGUAGE_LOCATION_CODE, userLanguageRequest.getLanguageLocationCode());

        if (!(WASH_ACADEMY.equals(serviceName))) {
            failureReasons.append(String.format(INVALID, SERVICE_NAME));
        }

        Service service = null;

        service = getServiceFromName(serviceName);

        if (failureReasons.length() > 0) {
            throw new IllegalArgumentException(failureReasons.toString());
        }

        Swachchagrahi swc = swcService.getByContactNumber(callingNumber);
        if (swc == null) {
            swc = new Swachchagrahi(callingNumber);
            swc.setJobStatus(SwcJobStatus.ACTIVE);
        }

        Language language = languageService.getForCode(languageLocationCode);
        if (null == language) {
            throw new NotFoundException(String.format(NOT_FOUND, LANGUAGE_LOCATION_CODE));
        }

        swc.setLanguage(language);

        State state =  getStateForFrontLineWorker(swc, null);

        //TODO: here in post Language location code we have to add courseID parameter, at the moment its not used,
        // but if we hit this api it will not upadte language for required swc.

        if (!serviceDeployedInUserState(service, state, swc.getCourseId())) {
            throw new NotDeployedException(String.format(NOT_DEPLOYED, service));
        }

        if (!frontLineWorkerAuthorizedForAccess(swc, state)) {
            throw new NotAuthorizedException(String.format(NOT_AUTHORIZED, CALLING_NUMBER));
        }

        // MOTECH-1667 added to get an upsert method included
        if (swc.getId() == null) {
            swcService.add(swc);
        } else {
            swcService.update(swc);
        }
    }

}
