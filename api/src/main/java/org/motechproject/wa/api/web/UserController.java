package org.motechproject.wa.api.web;

import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.wa.api.web.contract.SwcUserResponse;
import org.motechproject.wa.api.web.contract.UserResponse;
import org.motechproject.wa.api.web.domain.InactiveJobCallAudit;
import org.motechproject.wa.api.web.exception.NotAuthorizedException;
import org.motechproject.wa.api.web.exception.NotDeployedException;
import org.motechproject.wa.api.web.repository.InactiveJobCallAuditDataService;
import org.motechproject.wa.props.domain.Service;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.region.domain.Circle;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.service.*;
import org.motechproject.wa.swc.domain.*;
import org.motechproject.wa.swc.service.ServiceUsageCapService;
import org.motechproject.wa.swc.service.ServiceUsageService;
import org.motechproject.wa.swc.service.SwcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;


@SuppressWarnings("PMD")
@Controller
public class UserController extends BaseController {

    public static final String SERVICE_NAME = "serviceName";

    private LocationService locationService;

    @Autowired
    private SwcService swcService;

    @Autowired
    private ServiceUsageService serviceUsageService;

    @Autowired
    private ServiceUsageCapService serviceUsageCapService;

    @Autowired
    private CircleService circleService;

    @Autowired
    private DistrictService districtService;

    @Autowired
    private BlockService blockService;

    @Autowired
    private PanchayatService panchayatService;

    @Autowired
    private LanguageService languageService;

    @Autowired
    private InactiveJobCallAuditDataService inactiveJobCallAuditDataService;

//    @Autowired
//    private WashAcademyService washAcademyService;
//

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
    @RequestMapping(value = "/{serviceName}/user",
            method = RequestMethod.GET,
            headers = { "Content-type=application/json" }) // NO CHECKSTYLE Cyclomatic Complexity
    @ResponseBody
    @Transactional(noRollbackFor = NotAuthorizedException.class)
    public UserResponse getUserDetails(@PathVariable String serviceName,
                             @RequestParam(required = false) Long callingNumber,
                             @RequestParam(required = false) String operator,
                             @RequestParam(required = false) String circle,
                             @RequestParam(required = false) String callId,
                             @RequestParam(required = false) Integer courseId){

        log(String.format("REQUEST: /%s/user", serviceName), String.format(
                "callingNumber=%s, callId=%s, courseId=%d, operator=%s, circle=%s",
                LogHelper.obscure(callingNumber), callId, courseId, operator, circle));

        StringBuilder failureReasons = validate(callingNumber, callId, courseId, operator, circle);
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
            user = getFrontLineWorkerResponseUser(serviceName, callingNumber, courseId, circleObj);
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

      private UserResponse getFrontLineWorkerResponseUser(String serviceName, Long callingNumber, Integer courseId, Circle circle) {
        SwcUserResponse user = new SwcUserResponse();
        Service service = getServiceFromName(serviceName);
        ServiceUsage serviceUsage = new ServiceUsage(null, service, 0, 0, false);
        Swachchagrahi swc = swcService.getByContactNumberAndCourseId(callingNumber,courseId);
//        log("courseId is:"+swc.getCourseId()+" and contact number:"+swc.getContactNumber().toString());
//        if (swc == null) {
//            swc = swcService.getInctiveByContactNumber(callingNumber);
//        }

        if(swc == null && swcService.isAnonymousAllowed(courseId)){
            swc = swcService.getInctiveByContactNumberAndCourseId(callingNumber,courseId);
            if (swc == null && circle != null) {
                swc = new Swachchagrahi(callingNumber, circle, courseId);
                swc.setJobStatus(SwcJobStatus.ACTIVE);
                swc.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
                swcService.add(swc);
            }

        }

        State state = getStateForFrontLineWorker(swc, circle);

        if (state != null) {
            if (!serviceDeployedInUserState(service, state, courseId)) {
                throw new NotDeployedException(String.format(NOT_DEPLOYED, service));
            }
        } else {
            // If we have no state for the user see if the service is deployed in at least one state in the circle
            if (!serviceDeployedInCircle(service, circle, courseId)) {
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

            serviceUsage = serviceUsageService.getCurrentMonthlyUsageForSWCAndService(swc, service, courseId);

            if (!frontLineWorkerAuthorizedForAccess(swc, state)) {
                throw new NotAuthorizedException(String.format(NOT_AUTHORIZED, CALLING_NUMBER));
            }
        }

        ServiceUsageCap serviceUsageCap = serviceUsageCapService.getServiceUsageCap(state, service, courseId);
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


    /** Api created for importing swc manually **/

    @RequestMapping(value = "/{serviceName}/swcImport",
            method = RequestMethod.GET,
            headers = { "Content-type=application/json" }) // NO CHECKSTYLE Cyclomatic Complexity
    @ResponseBody
    @Transactional(noRollbackFor = NotAuthorizedException.class)
    public String createUsers(@PathVariable String serviceName,
                                 @RequestParam(required = false) Integer courseId) {
//        List<String[]> importingUsers = new ArrayList<>();
//       try{
//           Scanner sc = new Scanner(new File("/home/beehyv/swc/swc.csv"));
//           sc.useDelimiter("\n");
//
//           while (sc.hasNext())  //returns a boolean value
//           {
//               importingUsers.add(sc.next().split(","));
//           }
//           sc.close();
//       }
//       catch (FileNotFoundException e){
//           log(e.getMessage());
//       }
//
//        log("No Od users who needs to be created is: " + importingUsers.size());
////        ArrayList<Long> rejectedNumbers= new ArrayList<>();
//        Circle circleObj = circleService.getByName("BI");
//
//        for(int i=0;i<importingUsers.size();i++){
//            log("record:"+importingUsers.get(i).toString()+"\n");
//            Long callingNumber = Long.valueOf(importingUsers.get(i)[0]);
//            String name = importingUsers.get(i)[1];
//            Long stateId = Long.valueOf(importingUsers.get(i)[2]);
//
//
//
////            try{
////                if (!(validate(callingNumber,courseId,circleObj.getName()).length()>0)){
//                    createRegisteredUser(callingNumber,circleObj, courseId,name,23L);
////                }
////                else {
////                    rejectedNumbers.add(callingNumber);
////                }
////            }
////            catch (Exception e){
////
////            }
//
//        }
        return "imported usres";
    }

//    private void createRegisteredUser(Long callingNumber,Circle circle, Integer courseId, String name,Long districtId) {
//
//
//        Swachchagrahi swc = swcService.getByContactNumberAndCourseId(callingNumber,courseId);
//
//        if(swc == null ){
//            swc = swcService.getInctiveByContactNumberAndCourseId(callingNumber,courseId);
//            if (swc == null ) {
//                swc = new Swachchagrahi(callingNumber,circle);
//            }
//
//            swc.setJobStatus(SwcJobStatus.ACTIVE);
//            swc.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
//            swc.setCourseId(courseId);
//            swc.setName(name);
//            log(swc.toString());
//
//            State st=new State();
//            District dt= new District();
//            Block bk= new Block();
//            Panchayat pt= new Panchayat();
//
//            //find out state, district, block, panchayat object form db
//
//
//            st=locationService.getState(20L);
//            districtService.findByStateAndCode(20L,s);
//            blockService.findByDistrictAndCode();
//            panchayatService.findByBlockAndVcodeAndSvid();
//            swc.setState(st);
//            swc.setDistrict(dt);
//            swc.setBlock(bk);
//            swc.setPanchayat(pt);
//
//            try{
//                swcService.createSwc(swc);
//                swcService.update(swc);
//            }
//            catch(Exception e){
//                e.printStackTrace();
//            }
//        }
//    }


}
