package org.motechproject.wa.api.web.validator;

import org.motechproject.wa.api.web.contract.washAcademy.CourseResponse;
import org.motechproject.wa.api.web.contract.washAcademy.SmsStatusRequest;
import org.motechproject.wa.api.web.contract.washAcademy.sms.DeliveryInfo;
import org.motechproject.wa.api.web.contract.washAcademy.sms.DeliveryInfoNotification;
import org.motechproject.wa.api.web.contract.washAcademy.sms.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Validator helper class for API request and response
 */
public final class WashAcademyValidator {

    public static final String INVALID = "<%s: Invalid>";

    public static final String ADDRESS_PATTERN = "tel: \\d{10}$";

    private static final Logger LOGGER = LoggerFactory.getLogger(WashAcademyValidator.class);

    /**
     * Private constructor for static validation helpers
     */
    private WashAcademyValidator() {

    }

    public static String validateCourseResponse(CourseResponse courseResponse) {

        LOGGER.debug("Validating course response request");

        if (courseResponse.getName() == null) {
            return "Course.Name : " + "cannot be null";
        }

        if (courseResponse.getCourseVersion() < 1) {
            return "Course.Version : " + "cannot be < 1";
        }

        if (courseResponse.getChapters() == null) {
            return "Course.Content : " + "cannot be null";
        }

        return null;
    }

    public static String validateSmsStatus(SmsStatusRequest smsStatusRequest) {

        LOGGER.debug("validating sms status request");

        if (smsStatusRequest == null) {
            return String.format(INVALID, "smsRequest");
        }

        RequestData rd = smsStatusRequest.getRequestData();
        if (rd == null) {
            return String.format(INVALID, "RequestData");
        }

        DeliveryInfoNotification din = rd.getDeliveryInfoNotification();
        if (din == null) {
            return String.format(INVALID, "DeliveryInfoNotification");
        }

        if (din.getClientCorrelator() == null|| din.getClientCorrelator().isEmpty()) {
            return String.format(INVALID, "ClientCorrelator");
        }

        DeliveryInfo di = din.getDeliveryInfo();
        if (di == null) {
            return String.format(INVALID, "DeliveryInfo");
        }

        if (di.getAddress() == null || !Pattern.matches(ADDRESS_PATTERN, di.getAddress())) {
            return String.format(INVALID, "Address");
        }

        if (di.getDeliveryStatus() == null) {
            return String.format(INVALID, "DeliveryStatus");
        }

        return null;
    }

}
