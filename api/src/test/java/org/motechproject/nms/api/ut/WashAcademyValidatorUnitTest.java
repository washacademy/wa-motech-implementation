package org.motechproject.nms.api.ut;

import org.junit.Before;
import org.junit.Test;
import org.motechproject.nms.api.utils.CourseBuilder;
import org.motechproject.nms.api.web.contract.washAcademy.CourseResponse;
import org.motechproject.nms.api.web.contract.washAcademy.SmsStatusRequest;
import org.motechproject.nms.api.web.contract.washAcademy.sms.DeliveryInfo;
import org.motechproject.nms.api.web.contract.washAcademy.sms.DeliveryInfoNotification;
import org.motechproject.nms.api.web.contract.washAcademy.sms.DeliveryStatus;
import org.motechproject.nms.api.web.contract.washAcademy.sms.RequestData;
import org.motechproject.nms.api.web.validator.WashAcademyValidator;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Unit test for course structure validation
 */
public class WashAcademyValidatorUnitTest {

    private CourseResponse courseResponse;

    @Before
    public void setCourseResponse() {
        this.courseResponse = CourseBuilder.generateValidCourseResponse();
    }

    @Test
    public void TestValidCourseStructure() {

        assertNull(WashAcademyValidator.validateCourseResponse(courseResponse));
    }

    @Test
    public void TestValidateChapterNull() {

        courseResponse.setChapters(null);
        assertNotNull(WashAcademyValidator.validateCourseResponse(courseResponse));
    }

    @Test
    public void TestSmsStatusClientCorrelatorNull() {
        SmsStatusRequest status = GenerateValidSmsStatus();
        status.getRequestData().getDeliveryInfoNotification().setClientCorrelator(null);
        String errors = WashAcademyValidator.validateSmsStatus(status);
        assertNotNull(errors);
        assertTrue(errors.contains("ClientCorrelator"));
    }

    @Test
    public void TestSmsStatusDeliveryStatusNull() {

        SmsStatusRequest status = GenerateValidSmsStatus();
        status.getRequestData().getDeliveryInfoNotification().getDeliveryInfo().setDeliveryStatus(null);
        String errors = WashAcademyValidator.validateSmsStatus(status);
        assertNotNull(errors);
        assertTrue(errors.contains("DeliveryStatus"));
    }

    @Test
    public void TestSmsStatusAddressFormatInvalid() {

        SmsStatusRequest status = GenerateValidSmsStatus();
        status.getRequestData().getDeliveryInfoNotification().getDeliveryInfo().setAddress("987654321");
        String errors = WashAcademyValidator.validateSmsStatus(status);
        assertNotNull(errors);
        assertTrue(errors.contains("Address"));
    }

    @Test
    public void TestSmsStatusAddressValidNull() {

        SmsStatusRequest status = GenerateValidSmsStatus();
        status.getRequestData().getDeliveryInfoNotification().getDeliveryInfo().setAddress(null);
        String errors = WashAcademyValidator.validateSmsStatus(status);
        assertNotNull(errors);
        assertTrue(errors.contains("Address"));
    }

    private SmsStatusRequest GenerateValidSmsStatus() {
        SmsStatusRequest smsStatusRequest = new SmsStatusRequest();
        smsStatusRequest.setRequestData(new RequestData());
        smsStatusRequest.getRequestData().setDeliveryInfoNotification(new DeliveryInfoNotification());
        smsStatusRequest.getRequestData().getDeliveryInfoNotification().setClientCorrelator("FooBar");
        smsStatusRequest.getRequestData().getDeliveryInfoNotification().setDeliveryInfo(new DeliveryInfo());
        smsStatusRequest.getRequestData()
                .getDeliveryInfoNotification()
                .getDeliveryInfo()
                .setAddress("tel: 9876543219");
        smsStatusRequest.getRequestData()
                .getDeliveryInfoNotification()
                .getDeliveryInfo()
                .setDeliveryStatus(DeliveryStatus.DeliveredToTerminal);

        return smsStatusRequest;
    }
}
