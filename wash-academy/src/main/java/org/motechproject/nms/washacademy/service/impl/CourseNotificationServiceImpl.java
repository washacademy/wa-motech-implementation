package org.motechproject.nms.washacademy.service.impl;

import org.joda.time.DateTime;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.mtraining.service.ActivityService;
import org.motechproject.nms.swc.domain.Swachchagrahi;
import org.motechproject.nms.swc.service.SwcService;
import org.motechproject.nms.imi.service.SmsNotificationService;
import org.motechproject.nms.washacademy.domain.CourseCompletionRecord;
import org.motechproject.nms.washacademy.repository.CourseCompletionRecordDataService;
import org.motechproject.nms.washacademy.service.CourseNotificationService;
import org.motechproject.scheduler.contract.RepeatingSchedulableJob;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This handles all the integration pieces between MA and sms module to trigger and handle notifications
 * for course completion
 */
@Service("courseNotificationService")
public class CourseNotificationServiceImpl implements CourseNotificationService {

    private static final String COURSE_COMPLETED_SUBJECT = "nms.ma.course.completed";
    private static final String SMS_STATUS_SUBJECT = "nms.ma.sms.deliveryStatus";
    private static final String SMS_RETRY_COUNT = "sms.retry.count";
    private static final String DELIVERY_IMPOSSIBLE = "DeliveryImpossible";
    private static final String RETRY_FLAG = "retry.flag";
    private static final String FLWID = "flwId";
    private static final String SMS_CONTENT = "smsContent";
    private static final String DELIVERY_STATUS = "deliveryStatus";
    private static final String ADDRESS = "address";
    private static final String SMS_CONTENT_PREFIX = "sms.content.";
    private static final String SMS_DEFAULT_LANGUAGE_PROPERTY = "default";
    private static final Logger LOGGER = LoggerFactory.getLogger(CourseNotificationServiceImpl.class);

    /**
     * Data service with course completion info
     */

    /**
     * SMS bridge used to talk to IMI
     */
    private SmsNotificationService smsNotificationService;

    /**
     * Used to retrieve course data
     */
    private SettingsFacade settingsFacade;

    /**
     * Used to get flw information
     */
    private SwcService swcService;

    private CourseCompletionRecordDataService courseCompletionRecordDataService;

    /**
     * scheduler for future sms retries
     */
    private MotechSchedulerService schedulerService;

    /**
     * Used to pull completion activity for flw
     */
    private ActivityService activityService;

    /**
     * Used for raising alerts
     */
    private AlertService alertService;

    @Autowired
    public CourseNotificationServiceImpl(SmsNotificationService smsNotificationService,
                                         @Qualifier("maSettings") SettingsFacade settingsFacade,
                                         ActivityService activityService,
                                         MotechSchedulerService schedulerService,
                                         CourseCompletionRecordDataService courseCompletionRecordDataService,
                                         AlertService alertService,
                                         SwcService swcService) {

        this.smsNotificationService = smsNotificationService;
        this.settingsFacade = settingsFacade;
        this.schedulerService = schedulerService;
        this.alertService = alertService;
        this.activityService = activityService;
        this.swcService = swcService;
        this.courseCompletionRecordDataService = courseCompletionRecordDataService;
    }

    @MotechListener(subjects = { COURSE_COMPLETED_SUBJECT })
    @Transactional
    public void sendSmsNotification(MotechEvent event) {

        try {
            LOGGER.debug("Handling course completion notification event");
            Long flwId = (Long) event.getParameters().get(FLWID);

            List<CourseCompletionRecord> ccrs = courseCompletionRecordDataService.findByFlwId(flwId);
            if (ccrs == null || ccrs.isEmpty()) {
                // this should never be possible since the event dispatcher upstream adds the record
                LOGGER.error("No completion record found for flwId: " + flwId);
                return;
            }

            CourseCompletionRecord ccr = ccrs.get(ccrs.size() - 1);

            if (event.getParameters().containsKey(RETRY_FLAG)) {
                LOGGER.debug("Handling retry for SMS notification");
                ccr.setNotificationRetryCount(ccr.getNotificationRetryCount() + 1);
            }

            String smsContent = buildSmsContent(flwId, ccr);
            long callingNumber = swcService.getById(flwId).getContactNumber();
            ccr.setSentNotification(smsNotificationService.sendSms(callingNumber, smsContent));
            courseCompletionRecordDataService.update(ccr);
        } catch (IllegalStateException se) {
            LOGGER.error("Unable to send sms notification. Stack: " + se.toString());
            alertService.create("SMS Content", "MA SMS", se.getMessage(), AlertType.CRITICAL, AlertStatus.NEW, 0, null);
        }

    }

    @MotechListener(subjects = { SMS_STATUS_SUBJECT })
    @Transactional
    public void updateSmsStatus(MotechEvent event) {

        LOGGER.debug("Handling update sms delivery status event");
        String callingNumber = (String) event.getParameters().get(ADDRESS);
        int startIndex = callingNumber.indexOf(':') + 2;
        callingNumber = callingNumber.substring(startIndex);
        Swachchagrahi flw = swcService.getByContactNumber(Long.parseLong(callingNumber));
        Long flwId = null;
        if (flw != null) {
             flwId= flw.getId();
        }
        List<CourseCompletionRecord> ccrs = courseCompletionRecordDataService.findByFlwId(flwId);

        if (ccrs == null || ccrs.isEmpty()) {
            // this should never be possible since the event dispatcher upstream adds the record
            LOGGER.error("No completion record found for flwId: " + flwId);
            return;
        }
        CourseCompletionRecord ccr = ccrs.get(ccrs.size() - 1);

        // read properties
        String deliveryStatus = (String) event.getParameters().get(DELIVERY_STATUS);
        DateTime currentTime = DateTime.now();
        DateTime nextRetryTime = ccr.getModificationDate().plusDays(1);

        // update completion record and status
        ccr.setLastDeliveryStatus(deliveryStatus);
        courseCompletionRecordDataService.update(ccr);

        // handle sms failures and retry
        if (DELIVERY_IMPOSSIBLE.equals(deliveryStatus) &&
                ccr.getNotificationRetryCount() < Integer.parseInt(settingsFacade.getProperty(SMS_RETRY_COUNT))) {

            try {
                String smsContent = buildSmsContent(flwId, ccr);
                MotechEvent retryEvent = new MotechEvent(COURSE_COMPLETED_SUBJECT);
                retryEvent.getParameters().put(FLWID, flwId);
                retryEvent.getParameters().put(SMS_CONTENT, smsContent);
                retryEvent.getParameters().put(RETRY_FLAG, true);

                if (nextRetryTime.isBefore(currentTime)) {
                    // retry right away
                    sendSmsNotification(retryEvent);
                } else {
                    RepeatingSchedulableJob job = new RepeatingSchedulableJob(
                            retryEvent,     // MOTECH event
                            1,              // repeatCount, null means infinity
                            1,              // repeatIntervalInSeconds
                            nextRetryTime.toDate(), //startTime
                            null,           // endTime, null means no end time
                            true);          // ignorePastFiresAtStart

                    schedulerService.safeScheduleRepeatingJob(job);
                }
            } catch (IllegalStateException se) {
                LOGGER.error("Unable to send sms notification. Stack: " + se.toString());
                alertService.create("SMS Content", "MA SMS", "Error generating SMS content", AlertType.CRITICAL, AlertStatus.NEW, 0, null);
            }
        }
    }

    /**
     * Helper to generate the completion sms content for an flw
     * @param flwId calling number of the flw
     * @return localized sms content based on flw preferences or national default otherwise
     */
    private String buildSmsContent(Long flwId, CourseCompletionRecord ccr) {

        Swachchagrahi flw = swcService.getById(flwId);
        String locationCode = "XX"; // unknown location id
        String smsLanguageProperty = null;

        if (flw == null) {
            throw new IllegalStateException("Unable to find flw for flwId: " + flwId);
        }

        // Build location code
        if (flw.getState() != null && flw.getDistrict() != null) {
            locationCode = flw.getState().getCode().toString() + flw.getDistrict().getCode();
        }

        if (smsLanguageProperty == null || smsLanguageProperty.isEmpty()) {
            LOGGER.debug("No language code found in FLW. Reverting to national default");
            smsLanguageProperty = SMS_DEFAULT_LANGUAGE_PROPERTY;
        }

        // fetch sms content
        String smsContent = settingsFacade.getProperty(SMS_CONTENT_PREFIX + smsLanguageProperty);
        if (smsContent == null) {
            throw new IllegalStateException("Unable to get sms content for flw language: " +
                    SMS_CONTENT_PREFIX + smsLanguageProperty);
        }

        Long callingNumber = flw.getContactNumber();
        int attempts = activityService.getCompletedActivityForUser(callingNumber.toString()).size();
        String smsReferenceNumber = locationCode + callingNumber + attempts;
        ccr.setSmsReferenceNumber(smsReferenceNumber);
        courseCompletionRecordDataService.update(ccr);
        return smsContent + smsReferenceNumber;
    }
}
