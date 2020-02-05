package org.motechproject.wa.washacademy.service.impl;

import org.joda.time.DateTime;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.mtraining.domain.ActivityRecord;
import org.motechproject.mtraining.service.ActivityService;
import org.motechproject.scheduler.contract.RepeatingSchedulableJob;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.wa.imi.service.SmsNotificationService;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwachchagrahiStatus;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.washacademy.domain.CourseCompletionRecord;
import org.motechproject.wa.washacademy.repository.CourseCompletionRecordDataService;
import org.motechproject.wa.washacademy.repository.WaCourseDataService;
import org.motechproject.wa.washacademy.service.CourseNotificationService;
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

    private static final String COURSE_COMPLETED_SUBJECT = "wa.wa.course.completed";
    private static final String SMS_STATUS_SUBJECT = "wa.wa.sms.deliveryStatus";
    private static final String SMS_RETRY_COUNT = "sms.retry.count";
    private static final String DELIVERY_IMPOSSIBLE = "DeliveryImpossible";
    private static final String RETRY_FLAG = "retry.flag";
    private static final String SWCID = "swcId";
    private static final String SMS_CONTENT = "smsContent";
    private static final String DELIVERY_STATUS = "deliveryStatus";
    private static final String ADDRESS = "address";
    private static final String SMS_CONTENT_PREFIX = "sms.content.";
    private static final String SMS_CONTENT_PREFIX_ANONYMOUS = "sms.content.anonymous.";
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
     * Used to get swc information
     */
    private SwcService swcService;

    private CourseCompletionRecordDataService courseCompletionRecordDataService;

    /**
     * scheduler for future sms retries
     */
    private MotechSchedulerService schedulerService;

    private DistrictDataService districtDataService;

    /**
     * Used to pull completion activity for swc
     */
    private ActivityService activityService;

    /**
     * Used for raising alerts
     */
    private AlertService alertService;

    /**
     * Used for fetching courseName
     */
    private WaCourseDataService waCourseDataService;

    @Autowired
    public CourseNotificationServiceImpl(SmsNotificationService smsNotificationService,
                                         @Qualifier("maSettings") SettingsFacade settingsFacade,
                                         ActivityService activityService,
                                         MotechSchedulerService schedulerService,
                                         CourseCompletionRecordDataService courseCompletionRecordDataService,
                                         AlertService alertService,
                                         WaCourseDataService waCourseDataService,
                                         SwcService swcService,DistrictDataService districtDataService) {

        this.smsNotificationService = smsNotificationService;
        this.settingsFacade = settingsFacade;
        this.schedulerService = schedulerService;
        this.alertService = alertService;
        this.activityService = activityService;
        this.swcService = swcService;
        this.courseCompletionRecordDataService = courseCompletionRecordDataService;
        this.districtDataService = districtDataService;
        this.waCourseDataService = waCourseDataService;
    }

    @MotechListener(subjects = { COURSE_COMPLETED_SUBJECT })
    @Transactional
    public void sendSmsNotification(MotechEvent event) {

        try {
            LOGGER.debug("Handling course completion notification event");
            Long swcId = (Long) event.getParameters().get(SWCID);
            String courseName = (String) event.getParameters().get("courseName");
            Integer courseId = (Integer) event.getParameters().get("courseId");


            List<CourseCompletionRecord> ccrs = courseCompletionRecordDataService.findBySwcIdAndCourseId(swcId, courseId);
            if (ccrs == null || ccrs.isEmpty()) {
                // this should never be possible since the event dispatcher upstream adds the record
                LOGGER.error("No completion record found for swcId: " + swcId + "for courseId: " + courseId );
                return;
            }

            CourseCompletionRecord ccr = ccrs.get(ccrs.size() - 1);

            if (event.getParameters().containsKey(RETRY_FLAG)) {
                LOGGER.debug("Handling retry for SMS notification");
                ccr.setNotificationRetryCount(ccr.getNotificationRetryCount() + 1);
            }

            String smsContent = buildSmsContent(swcId, ccr, courseName);
            long callingNumber = swcService.getById(swcId).getContactNumber();
            ccr.setSentNotification(smsNotificationService.sendSms(callingNumber, smsContent, courseId));
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
        String clientCorrelator = (String)event.getParameters().get("clientCorrelator");
        Integer courseId = (Integer) event.getParameters().get("courseId");
        String courseName = waCourseDataService.getCourseById(courseId).getName();

        int startIndex = callingNumber.indexOf(':') + 2;
        callingNumber = callingNumber.substring(startIndex);
        Swachchagrahi swc = swcService.getByContactNumber(Long.parseLong(callingNumber));
        Long swcId = null;
        if (swc != null) {
             swcId= swc.getId();
        }
        List<CourseCompletionRecord> ccrs = courseCompletionRecordDataService.findBySwcIdAndCourseId(swcId, courseId);

        if (ccrs == null || ccrs.isEmpty()) {
            // this should never be possible since the event dispatcher upstream adds the record
            LOGGER.error("No completion record found for swcId: " + swcId);
            return;
        }
        CourseCompletionRecord ccr = ccrs.get(ccrs.size() - 1);

        ccr.setClientCorrelator(clientCorrelator);

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
                String smsContent = buildSmsContent(swcId, ccr, courseName);
                MotechEvent retryEvent = new MotechEvent(COURSE_COMPLETED_SUBJECT);
                retryEvent.getParameters().put(SWCID, swcId);
                retryEvent.getParameters().put(SMS_CONTENT, smsContent);
                retryEvent.getParameters().put(RETRY_FLAG, true);
                retryEvent.getParameters().put("courseId", courseId);

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
     * Helper to generate the completion sms content for an swc
     * @param swcId calling number of the swc
     * @return localized sms content based on swc preferences or national default otherwise
     */
    private String buildSmsContent(Long swcId, CourseCompletionRecord ccr, String courseName) {

        Swachchagrahi swc = swcService.getById(swcId);
        String locationCode = "XX"; // unknown location id
        String smsLanguageProperty = null;
        String smsContent = null;


        if (swc == null) {
            throw new IllegalStateException("Unable to find swc for Id: " + swcId);
        }

        // Build location code
        if (swc.getState() != null && swc.getDistrict() != null) {
            locationCode = swc.getState().getCode().toString() + swc.getDistrict().getCode();
        }
        // set sms content language
        if (swc.getLanguage() != null) {
            // get language from flw, if exists
            smsLanguageProperty = swc.getLanguage().getCode();
        } else {
            District flwDistrict = swc.getDistrict();
            if (flwDistrict != null) {
                // get language from flw location (district), if exists
                Language flwLanguage = (Language) districtDataService.getDetachedField(flwDistrict, "language");
                if (flwLanguage != null) {
                    smsLanguageProperty = flwLanguage.getCode();
                }
            }
        }

        if (smsLanguageProperty == null || smsLanguageProperty.isEmpty()) {
            LOGGER.debug("No language code found in SWC. Reverting to national default");
            smsLanguageProperty = SMS_DEFAULT_LANGUAGE_PROPERTY;
        }

        // fetch sms content
        if(swc.getCourseStatus() == SwachchagrahiStatus.ANONYMOUS) {
             smsContent = settingsFacade.getProperty(SMS_CONTENT_PREFIX_ANONYMOUS + smsLanguageProperty);
            if (smsContent == null) {
                throw new IllegalStateException("Unable to get sms content for swc language: " +
                        SMS_CONTENT_PREFIX_ANONYMOUS + smsLanguageProperty);
            }
        } else {
             smsContent = settingsFacade.getProperty(SMS_CONTENT_PREFIX + smsLanguageProperty);
            if (smsContent == null) {
                throw new IllegalStateException("Unable to get sms content for swc language: " +
                        SMS_CONTENT_PREFIX + smsLanguageProperty);
            }
        }



        Long callingNumber = swc.getContactNumber();
        List<ActivityRecord> activityRecords = activityService.getCompletedActivityForUser(callingNumber.toString());
        List<ActivityRecord> givenCourseActivityRecord = null;
        for(int i=1;i<activityRecords.size();i++){
            if (activityRecords.get(i).getCourseName() == courseName){
                givenCourseActivityRecord.add(activityRecords.get(i));
            }

        }
        int attempts = 0;
        if (givenCourseActivityRecord != null){
             attempts = givenCourseActivityRecord.size();
        }

        String smsReferenceNumber = locationCode + callingNumber + attempts + courseName;
        ccr.setSmsReferenceNumber(smsReferenceNumber);
        courseCompletionRecordDataService.update(ccr);
        return smsContent + smsReferenceNumber;
    }
}
