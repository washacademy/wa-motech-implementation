package org.motechproject.wa.api.web;

import org.joda.time.DateTime;
import org.motechproject.wa.api.web.contract.CallContentRequest;
import org.motechproject.wa.api.web.contract.CallDetailRecordRequest;
import org.motechproject.wa.props.domain.FinalCallStatus;
import org.motechproject.wa.props.domain.Service;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.swc.domain.*;
import org.motechproject.wa.swc.repository.SwcStatusUpdateAuditDataService;
import org.motechproject.wa.swc.service.CallContentService;
import org.motechproject.wa.swc.service.CallDetailRecordService;
import org.motechproject.wa.swc.service.SwcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Controller
public class CallDetailsController extends BaseController {

    public static final int MILLISECONDS_PER_SECOND = 1000;
    private static final String QUESTION_TYPE = "question";
    private static final String LESSON_TYPE = "lesson";
    private static final String CHAPTER_TYPE = "chapter";

    @Autowired
    private CallDetailRecordService callDetailRecordService;

    @Autowired
    private CallContentService callContentService;

    @Autowired
    private SwcService swcService;

    @Autowired
    private SwcStatusUpdateAuditDataService swcStatusUpdateAuditDataService;

    /**
     * 2.2.6 Save CallDetails API
     * IVR shall invoke this API to send MA call details to MoTech.
     * /api/washacademy/callDetails
     *
     * 3.2.2 Save Call Details API
     * This API enables IVR to send call details to wa_MoTech_MK. This data is further saved in wa database and used
     *    for reporting purpose.
     * /api/mobilekunji/callDetails
     *
     */
    @RequestMapping(value = "/{serviceName}/callDetails", // NO CHECKSTYLE Cyclomatic Complexity
            method = RequestMethod.POST,
            headers = { "Content-type=application/json" })
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void saveCallDetails(@PathVariable String serviceName,
                                @RequestBody CallDetailRecordRequest callDetailRecordRequest) {

        log(String.format("REQUEST: /%s/callDetails (POST)", serviceName), LogHelper.nullOrString(callDetailRecordRequest));

        Service service = null;
        StringBuilder failureReasons;

        if (!(WASH_ACADEMY.equals(serviceName))) {
            throw new IllegalArgumentException(String.format(INVALID, "serviceName"));
        }

        failureReasons = validate(callDetailRecordRequest.getCallingNumber(),
                callDetailRecordRequest.getCallId(),callDetailRecordRequest.getCourseId(), callDetailRecordRequest.getOperator(),
                callDetailRecordRequest.getCircle());

        if (failureReasons.length() > 0) {
            throw new IllegalArgumentException(failureReasons.toString());
        }

        // Verify common elements
        // (callStartTime, callEndTime, callDurationInPulses, endOfUsagePromptCount, callStatus,
        // callDisconnectReason)
        failureReasons.append(validateCallDetailsCommonElements(callDetailRecordRequest));

        if (WASH_ACADEMY.equals(serviceName)) {
            service = Service.WASH_ACADEMY;

            // Verify MA elements
            failureReasons.append(validateCallDetailsWashAcademyElements(callDetailRecordRequest));
        }

//        if (MOBILE_KUNJI.equals(serviceName)) {
//            service = Service.MOBILE_KUNJI;
//
//            // Verify MK elements (welcomeMessagePromptFlag)
//            failureReasons.append(validateCallDetailsMobileKunjiElements(callDetailRecordRequest));
//        }

        for (CallContentRequest callContentRequest : callDetailRecordRequest.getContent()) {
            failureReasons.append(validateCallContentRequest(service, callContentRequest));
        }

        if (failureReasons.length() > 0) {
            throw new IllegalArgumentException(failureReasons.toString());
        }

        Swachchagrahi swc = swcService.getByContactNumber(callDetailRecordRequest.getCallingNumber());
        if (null == swc) {
            // If the swc doesn't exist it is possible they hung up before providing their language.  We
            // create an anonymous stub swc here
            swc = new Swachchagrahi(callDetailRecordRequest.getCallingNumber());
            swc.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
            swcService.add(swc);

            // reload so the record can be linked to later.
            swc = swcService.getByContactNumber(callDetailRecordRequest.getCallingNumber());
        }

        createCallDetailRecord(swc, callDetailRecordRequest, service);

        // if this is the SWC's first time calling the service, set her status to ACTIVE based on wa.GEN.SWC.003
        if (swc.getCourseStatus() == SwachchagrahiStatus.INACTIVE &&
                validateSwcNameAndNumber(swc) &&
                validateSwcLocation(swc)) {
            swc.setCourseStatus(SwachchagrahiStatus.ACTIVE);
            swcService.update(swc);
            SwcStatusUpdateAudit swcStatusUpdateAudit = new SwcStatusUpdateAudit(DateTime.now(), swc.getSwcId(), swc.getContactNumber(), UpdateStatusType.INACTIVE_TO_ACTIVE);
            swcStatusUpdateAuditDataService.create(swcStatusUpdateAudit);
        }
    }

    private boolean validateSwcLocation(Swachchagrahi swc) {
        return swc.getState() != null && swc.getDistrict() != null;
    }

    private boolean validateSwcNameAndNumber(Swachchagrahi swc) {
        return swc.getName() != null && swc.getContactNumber() != null;
    }

    private void createCallDetailRecord(Swachchagrahi swc, CallDetailRecordRequest callDetailRecordRequest,
                                        Service service) {
        CallDetailRecord cdr = new CallDetailRecord();
        cdr.setService(service);
        cdr.setSwachchagrahi(swc);
        cdr.setCallingNumber(callDetailRecordRequest.getCallingNumber());
        cdr.setCallId(callDetailRecordRequest.getCallId());
        cdr.setOperator(callDetailRecordRequest.getOperator());
        cdr.setCourseId(callDetailRecordRequest.getCourseId());
        cdr.setCircle(callDetailRecordRequest.getCircle());
        cdr.setCallStartTime(new DateTime(callDetailRecordRequest.getCallStartTime() * MILLISECONDS_PER_SECOND));
        cdr.setCallEndTime(new DateTime(callDetailRecordRequest.getCallEndTime() * MILLISECONDS_PER_SECOND));
        cdr.setCallDurationInPulses(callDetailRecordRequest.getCallDurationInPulses());
        cdr.setEndOfUsagePromptCounter(callDetailRecordRequest.getEndOfUsagePromptCounter());
        cdr.setFinalCallStatus(FinalCallStatus.fromInt(callDetailRecordRequest.getCallStatus()));
        cdr.setCallDisconnectReason(callDetailRecordRequest.getCallDisconnectReason());

//        if (service == Service.MOBILE_KUNJI) {
//            cdr.setWelcomePrompt(callDetailRecordRequest.getWelcomeMessagePromptFlag());
//        }

        callDetailRecordService.add(cdr);

        for (CallContentRequest callContentRequest : callDetailRecordRequest.getContent()) {
            CallContent content = new CallContent();

            content.setContentName(callContentRequest.getContentName());
            content.setContentFile(callContentRequest.getContentFileName());
            content.setStartTime(new DateTime(callContentRequest.getStartTime() * MILLISECONDS_PER_SECOND));
            content.setEndTime(new DateTime(callContentRequest.getEndTime() * MILLISECONDS_PER_SECOND));

//            if (service == Service.MOBILE_KUNJI) {
//                content.setMobileKunjiCardCode(callContentRequest.getMkCardCode());
//            }

            if (service == Service.WASH_ACADEMY) {
                content.setType(callContentRequest.getType());
                content.setCorrectAnswerEntered(callContentRequest.isCorrectAnswerEntered()); // this could be null, if not question
                content.setCompletionFlag(callContentRequest.getCompletionFlag());
            }

            content.setCallDetailRecord(cdr);

            callContentService.add(content);
        }
    }

    // (callStartTime, callEndTime, callDurationInPulses, endOfUsagePromptCount, callStatus, callDisconnectReason)
    private String validateCallDetailsCommonElements(CallDetailRecordRequest callDetailRecordRequest) {
        StringBuilder failureReasons = new StringBuilder();

        if (null == callDetailRecordRequest.getCallStartTime()) {
            failureReasons.append(String.format(NOT_PRESENT, "callStartTime"));
        }

        if (null == callDetailRecordRequest.getCourseId()){
            failureReasons.append(String.format(NOT_PRESENT, "courseId"));
        }

        if (!(callDetailRecordRequest.getCourseId() ==1 || callDetailRecordRequest.getCourseId() ==2)){
            failureReasons.append(String.format("Incorrect CourseId", "courseId"));
        }

        if (null == callDetailRecordRequest.getCallEndTime()) {
            failureReasons.append(String.format(NOT_PRESENT, "callEndTime"));
        }

        if (null == callDetailRecordRequest.getCallDurationInPulses()) {
            failureReasons.append(String.format(NOT_PRESENT, "callDurationInPulses"));
        }

        if (null == callDetailRecordRequest.getEndOfUsagePromptCounter()) {
            failureReasons.append(String.format(NOT_PRESENT, "endOfUsagePromptCount"));
        }

        if (null == callDetailRecordRequest.getCallStatus()) {
            failureReasons.append(String.format(NOT_PRESENT, "callStatus"));
        }

        if (null == callDetailRecordRequest.getCallDisconnectReason()) {
            failureReasons.append(String.format(NOT_PRESENT, "callDisconnectReason"));
        }

        return failureReasons.toString();
    }

    // welcomeMessagePromptFlag
    private String validateCallDetailsMobileKunjiElements(CallDetailRecordRequest callDetailRecordRequest) {
        StringBuilder failureReasons = new StringBuilder();

        if (callDetailRecordRequest.getWelcomeMessagePromptFlag() == null) {
            failureReasons.append(String.format(NOT_PRESENT, "welcomeMessagePromptFlag"));
        }

        return failureReasons.toString();
    }

    private String validateCallDetailsWashAcademyElements(CallDetailRecordRequest callDetailRecordRequest) {
        StringBuilder failureReasons = new StringBuilder();

        // validate content type. No validation on correctAnswered because a disconnect during question
        // might be null
        for (CallContentRequest callContentRequest : callDetailRecordRequest.getContent()) {
            String contentType = callContentRequest.getType();
            if (contentType != null &&
                !contentType.equals(QUESTION_TYPE) &&
                !contentType.equals(CHAPTER_TYPE) &&
                !contentType.equals(LESSON_TYPE)) {
                    failureReasons.append(String.format(INVALID, "CallContent_type"));
            }
        }

        return failureReasons.toString();
    }

    private String validateCallContentRequest(Service service, CallContentRequest callContentRequest) { // NO CHECKSTYLE Cyclomatic Complexity
        StringBuilder failureReasons = new StringBuilder();

        // Common elements (contentName, contentFile, startTime, endTime)
        if (null == callContentRequest.getContentName()|| callContentRequest.getContentName().isEmpty()) {
            failureReasons.append(String.format(NOT_PRESENT, "contentName"));
        }

        if (null == callContentRequest.getContentFileName() || callContentRequest.getContentFileName().isEmpty()) {
            failureReasons.append(String.format(NOT_PRESENT, "contentFile"));
        }

        if (null == callContentRequest.getStartTime()) {
            failureReasons.append(String.format(NOT_PRESENT, "startTime"));
        }

        if (null == callContentRequest.getEndTime()) {
            failureReasons.append(String.format(NOT_PRESENT, "endTime"));
        }

        // MK elements (mkCardCode)
//        if (service == Service.MOBILE_KUNJI) {
//            if (null == callContentRequest.getMkCardCode() || callContentRequest.getMkCardCode().isEmpty()) {
//                failureReasons.append(String.format(NOT_PRESENT, "mkCardCode"));
//            }
//        }

        // MA elements (type, completionFlag)
        if (service == Service.WASH_ACADEMY) {
            if (null == callContentRequest.getType()) {
                failureReasons.append(String.format(NOT_PRESENT, "type"));
            }

            if (null == callContentRequest.getCompletionFlag()) {
                failureReasons.append(String.format(NOT_PRESENT, "completionFlag"));
            }
        }

        return failureReasons.toString();
    }
}
