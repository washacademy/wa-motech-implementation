
package org.motechproject.wa.api.web;

import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.wa.api.web.contract.washAcademy.*;
import org.motechproject.wa.api.web.contract.washAcademy.sms.DeliveryInfo;
import org.motechproject.wa.api.web.converter.WashAcademyConverter;
import org.motechproject.wa.api.web.validator.WashAcademyValidator;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.washacademy.dto.WaBookmark;
import org.motechproject.wa.washacademy.dto.WaCourse;
import org.motechproject.wa.washacademy.exception.CourseNotCompletedException;
import org.motechproject.wa.washacademy.service.WashAcademyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


/**
 * Mobile Academy controller
 */
@RequestMapping("washacademy")
@Controller
public class WashAcademyController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WashAcademyController.class);

    private static final String SMS_STATUS_SUBJECT = "wa.wa.sms.deliveryStatus";

    /**
     * MA service to handle all business logic
     */
    @Autowired
    private WashAcademyService washAcademyService;

    @Autowired
    private SwcService swcService;

    /**
     * Event relay service to handle async notifications
     */
    private EventRelay eventRelay;

    // Default constructor for CGLIB generation
    public WashAcademyController() {
        super();
    }
    /**
     * Constructor for controller
     * @param washAcademyService mobile academy service
     * @param eventRelay event relay service
     */
    @Autowired
    public WashAcademyController(WashAcademyService washAcademyService, EventRelay eventRelay) {
        this.washAcademyService = washAcademyService;
        this.eventRelay = eventRelay;
    }

    /**
     *
     * 2.2.2.1 Get MA Course – Request
     *
     * Get course
     * @return course response object
     */
    @Transactional(readOnly = true)
    @RequestMapping(
            value = "/course",
            method = RequestMethod.GET)
    @ResponseBody
    public CourseResponse getCourse() {

        log("REQUEST: /washacademy/course");

        WaCourse getCourse = washAcademyService.getCourse();

        if (getCourse == null) {
            LOGGER.error("No course found in database. Check course ingestion and name");
            throw new InternalError(String.format(NOT_FOUND, "course"));
        }

        CourseResponse response = WashAcademyConverter.convertCourseDto(getCourse);

        if (response == null) {
            LOGGER.error("Failed dto mapping, check object mapping");
            throw new InternalError(String.format(INVALID, "CourseResponse"));
        }

        log("RESPONSE: /washacademy/course", response.toString());
        return response;
    }

    /**
     * Get the version of the course
     * @return Integer representing the timestamp since epoch
     * ***NOTE::*** THIS IS USED by the LOAD BALANCER and NAGIOS monitoring to certify server health,
     * do not delete, or be careful when you refactor!! With great power comes great responsibility!!
     */
    @Transactional(readOnly = true)
    @RequestMapping(
            value = "/courseVersion",
            method = RequestMethod.GET)
    @ResponseBody
    public CourseVersionResponse getCourseVersion() {

        log("REQUEST: /washacademy/courseVersion");

        CourseVersionResponse response = new CourseVersionResponse(washAcademyService.getCourseVersion());
        log("RESPONSE: /washacademy/courseVersion", response.toString());
        return response;
    }

    /**
     * Get bookmark for a user
     * @param callingNumber number of the caller
     * @param callId unique tracking id for the call
     * @return serialized json bookmark response
     */
    @Transactional(readOnly = true)
    @RequestMapping(
            value = "/bookmarkWithScore",
            method = RequestMethod.GET,
            headers = { "Content-type=application/json" })
    @ResponseBody
    public GetBookmarkResponse getBookmarkWithScore(@RequestParam(required = false) Long callingNumber,
                                                    @RequestParam(required = false) String callId) {

        log("REQUEST: /washacademy/bookmarkWithScore", String.format("callingNumber=%s, callId=%s",
                LogHelper.obscure(callingNumber), callId));

        StringBuilder errors = new StringBuilder();
        validateField10Digits(errors, "callingNumber", callingNumber);
        if (errors.length() != 0) {
            throw new IllegalArgumentException(errors.toString());
        }

        validateCallId(errors, callId);
        if (errors.length() != 0) {
            throw new IllegalArgumentException(errors.toString());
        }

        WaBookmark bookmark = washAcademyService.getBookmark(callingNumber, callId);

        GetBookmarkResponse ret = WashAcademyConverter.convertBookmarkDto(bookmark);
        log("RESPONSE: /washacademy/bookmarkWithScore", String.format("callId=%s, %s", callId, ret.toString()));
        return ret;
    }

    /**
     * Save a bookmark for a user
     * @param bookmarkRequest info about the user for bookmark save
     * @return OK or exception
     */
    @RequestMapping(
            value = "/bookmarkWithScore",
            method = RequestMethod.POST,
            headers = { "Content-type=application/json" })
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void saveBookmarkWithScore(@RequestBody SaveBookmarkRequest bookmarkRequest) {

        log("REQUEST: /washacademy/bookmarkWithScore (POST)", LogHelper.nullOrString(bookmarkRequest));

        // validate bookmark
        if (bookmarkRequest == null) {
            throw new IllegalArgumentException(String.format(INVALID, "bookmarkRequest"));
        }

        // validate calling number
        StringBuilder errors = new StringBuilder();
        validateField10Digits(errors, "callingNumber", bookmarkRequest.getCallingNumber());
        if (errors.length() != 0) {
            throw new IllegalArgumentException(errors.toString());
        }

        // validate call id
        validateCallId(errors, bookmarkRequest.getCallId());
        if (errors.length() != 0) {
            throw new IllegalArgumentException(errors.toString());
        }

        // validate scores
        if (validateWAScores(bookmarkRequest.getScoresByChapter())) {
            Swachchagrahi swc = swcService.getByContactNumber(bookmarkRequest.getCallingNumber());
            Long swcId = swc.getId();
            WaBookmark bookmark = WashAcademyConverter.convertSaveBookmarkRequest(bookmarkRequest, swcId);
            washAcademyService.setBookmark(bookmark);
        }
    }

    /**
     * Save sms status. The request mapping value is not ideal here but updating it since it would cost
     * more effort to do a CR with IMI at this point
     * @param smsDeliveryStatus sms delivery details
     * @return OK or exception
     */
    @Transactional
    @RequestMapping(
            value = "/sms/status/imi",
            method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void saveSmsStatus(@RequestBody SmsStatusRequest smsDeliveryStatus) {

        log("REQUEST: /washacademy/sms/status/imi (POST)", LogHelper.nullOrString(smsDeliveryStatus));

        String errors = WashAcademyValidator.validateSmsStatus(smsDeliveryStatus);

        if (errors != null) {
            throw new IllegalArgumentException(errors);
        }

        //TODO: should this be refactored into IMI module or sms module?
        // we updated the completion record. Start event message to trigger notification workflow
        DeliveryInfo deliveryInfo = smsDeliveryStatus.getRequestData()
                .getDeliveryInfoNotification().getDeliveryInfo();
        Map<String, Object> eventParams = new HashMap<>();
        eventParams.put("address", deliveryInfo.getAddress());
        eventParams.put("deliveryStatus", deliveryInfo.getDeliveryStatus().toString());
        MotechEvent motechEvent = new MotechEvent(SMS_STATUS_SUBJECT, eventParams);
        eventRelay.sendEventMessage(motechEvent);
        LOGGER.debug("Sent event message to process completion notification");
    }

    @Transactional
    @RequestMapping(
            value = "/notify",
            method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void sendNotification(@RequestBody Long swcId) {

        log("REQUEST: /washacademy/notify (POST)", String.format("swcId=%s", String.valueOf(swcId)));

        // done with validation
        try {
            washAcademyService.triggerCompletionNotification(swcId);
        } catch (CourseNotCompletedException cnc) {
            LOGGER.error("Could not send notification: " + cnc.toString());
            throw cnc;
        }
    }

}
