package org.motechproject.nms.washacademy.ut;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.mtraining.domain.ActivityRecord;
import org.motechproject.mtraining.domain.Bookmark;
import org.motechproject.mtraining.repository.ActivityDataService;
import org.motechproject.mtraining.service.ActivityService;
import org.motechproject.mtraining.service.BookmarkService;
import org.motechproject.mtraining.service.MTrainingService;
import org.motechproject.nms.swc.domain.Swachchagrahi;
import org.motechproject.nms.swc.service.SwcService;
import org.motechproject.nms.imi.service.SmsNotificationService;
import org.motechproject.nms.washacademy.domain.CourseCompletionRecord;
import org.motechproject.nms.washacademy.domain.NmsCourse;
import org.motechproject.nms.washacademy.dto.MaBookmark;
import org.motechproject.nms.washacademy.exception.CourseNotCompletedException;
import org.motechproject.nms.washacademy.repository.CourseCompletionRecordDataService;
import org.motechproject.nms.washacademy.repository.MtrainingModuleActivityRecordAuditDataService;
import org.motechproject.nms.washacademy.repository.NmsCourseDataService;
import org.motechproject.nms.washacademy.service.WashAcademyService;
import org.motechproject.nms.washacademy.service.impl.CourseNotificationServiceImpl;
import org.motechproject.nms.washacademy.service.impl.WashAcademyServiceImpl;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.Language;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.scheduler.contract.RepeatingSchedulableJob;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Unit tests for Mobile Academy Service
 */
public class WashAcademyServiceUnitTest {

    private static final String VALID_CALL_ID = "1234567890123456789012345";

    @Mock
    private WashAcademyService washAcademyService;

    @Mock
    private MTrainingService mTrainingService;

    @Mock
    private BookmarkService bookmarkService;

    @Mock
    private ActivityService activityService;

    @Mock
    private SwcService swcService;

    @Mock
    private NmsCourseDataService nmsCourseDataService;

    @Mock
    private CourseCompletionRecordDataService courseCompletionRecordDataService;

    @Mock
    private ActivityDataService activityDataService;

    @Mock
    private EventRelay eventRelay;

    @Mock
    private CourseNotificationServiceImpl courseNotificationService;

    @Mock
    private SmsNotificationService smsNotificationService;

    @Mock
    private SettingsFacade settingsFacade;

    @Mock
    private AlertService alertService;

    @Mock
    private MotechSchedulerService schedulerService;

    @Mock
    private MtrainingModuleActivityRecordAuditDataService mtrainingModuleActivityRecordAuditDataService;

    private Validator validator;

    @Before
    public void setup() {
        initMocks(this);
        nmsCourseDataService.deleteAll();
        when(settingsFacade.getRawConfig("nmsCourse.json")).thenReturn(getFileInputStream("nmsCourseTest.json"));
        washAcademyService = new WashAcademyServiceImpl(bookmarkService, activityService,
                nmsCourseDataService, activityDataService, courseCompletionRecordDataService, swcService, eventRelay, mtrainingModuleActivityRecordAuditDataService, settingsFacade, alertService);
        courseNotificationService = new CourseNotificationServiceImpl(smsNotificationService,
                    settingsFacade, activityService, schedulerService, courseCompletionRecordDataService, alertService,
                swcService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        when(activityService.createActivity(any(ActivityRecord.class))).thenReturn(new ActivityRecord());
    }

    @Test
    public void getCourseTest() {

        NmsCourse newCourse = new NmsCourse("MobileAcademyCourse", "[]");
        newCourse.setModificationDate(DateTime.now());
        nmsCourseDataService.create(newCourse);
        when(nmsCourseDataService.getCourseByName("MobileAcademyCourse")).thenReturn(newCourse);
        assertTrue(washAcademyService.getCourse().getContent().equals(newCourse.getContent()));
    }

    @Test
    public void getBookmarkTest() {
        Bookmark newBookmark = new Bookmark("55", "getBookmarkTest", null, null, null);

        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setId(55L);
        when(bookmarkService.getLatestBookmarkByUserId(anyString()))
                .thenReturn(newBookmark);
        when(swcService.getByContactNumber(anyLong())).thenReturn(flw);

        MaBookmark mab = washAcademyService.getBookmark(1234567890L, VALID_CALL_ID);
        assertTrue(mab.getFlwId() == 55L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setBookmarkNullTest() {

        washAcademyService.setBookmark(null);
    }

    @Test
    public void setNewBookmarkTest() {
        MaBookmark mab = new MaBookmark(123456L, VALID_CALL_ID, "Chapter1_Lesson1", null);

        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setId(123456L);
        when(bookmarkService.createBookmark(any(Bookmark.class))).thenReturn(new Bookmark());
        when(bookmarkService.getLatestBookmarkByUserId(anyString())).thenReturn(null);
        when(swcService.getById(anyLong())).thenReturn(flw);
        washAcademyService.setBookmark(mab);
    }

    @Test
    public void setUpdateBookmarkTest() {
        MaBookmark mab = new MaBookmark(123456L, VALID_CALL_ID, "Chapter1_Lesson1", null);

        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setId(123456L);
        when(bookmarkService.createBookmark(any(Bookmark.class)))
                .thenReturn(new Bookmark());
        when(bookmarkService.getLatestBookmarkByUserId(anyString()))
                .thenReturn(new Bookmark());
        when(swcService.getById(anyLong())).thenReturn(flw);
        washAcademyService.setBookmark(mab);
    }

    @Test
    public void setLastBookmark() {
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), ((int) (Math.random() * 100)) % 5);
        }
        MaBookmark mab = new MaBookmark(123456L, VALID_CALL_ID, "COURSE_COMPLETED", scores);
        doNothing().when(eventRelay).sendEventMessage(any(MotechEvent.class));

        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 22, scores.toString(), false);
        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findByFlwId(anyLong())).thenReturn(records);
        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setId(123456L);
        when(swcService.getById(anyLong())).thenReturn(flw);
        washAcademyService.setBookmark(mab);
    }

    @Test
    public void setLastBookmarkFailingScore() {
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 0);
        }
        MaBookmark mab = new MaBookmark(123456L, VALID_CALL_ID, "COURSE_COMPLETED", scores);
        doNothing().when(eventRelay).sendEventMessage(any(MotechEvent.class));
        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setId(123456L);
        when(swcService.getById(anyLong())).thenReturn(flw);
        washAcademyService.setBookmark(mab);
    }

    @Test
    public void getLastBookmarkReset() {

        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 4);
        }

        Map<String, Object> progress = new HashMap<>();
        progress.put("scoresByChapter", scores);
        progress.put("bookmark", "COURSE_COMPLETED");
        Bookmark newBookmark = new Bookmark("55", "getBookmarkTest", null, null, progress);
        when(bookmarkService.getLatestBookmarkByUserId(anyString()))
                .thenReturn(newBookmark);
        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setId(55L);
        when(swcService.getByContactNumber(anyLong())).thenReturn(flw);
        MaBookmark retrieved = washAcademyService.getBookmark(1234567890L, VALID_CALL_ID);
        assertNull(retrieved.getBookmark());
        assertNull(retrieved.getScoresByChapter());
    }

    @Test
    public void testStatusUpdateNotification() {
        MotechEvent event = new MotechEvent();
        event.getParameters().put("address", "tel: 9876543210");
        event.getParameters().put("deliveryStatus", "DeliveredToTerminal");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 34, "score", true);
        ccr.setModificationDate(DateTime.now());
        assertNull(ccr.getLastDeliveryStatus());

        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findByFlwId(anyLong())).thenReturn(records);
        when(settingsFacade.getProperty(anyString())).thenReturn("1");
        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setSwcId("123456");
        when(swcService.getByContactNumber(anyLong())).thenReturn(flw);
        courseNotificationService.updateSmsStatus(event);
        assertTrue(ccr.getLastDeliveryStatus().equals("DeliveredToTerminal"));
    }

    @Test
    public void testStatusUpdateNotificationRetry() {
        MotechEvent event = new MotechEvent();
        event.getParameters().put("address", "tel: 9876543210");
        event.getParameters().put("deliveryStatus", "DeliveryImpossible");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 34, "score", true);
        ccr.setModificationDate(DateTime.now().minusDays(1));
        assertNull(ccr.getLastDeliveryStatus());
        assertEquals(0, ccr.getNotificationRetryCount());

        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findByFlwId(anyLong())).thenReturn(records);
        when(settingsFacade.getProperty(anyString())).thenReturn("1");
        doNothing().when(schedulerService).safeScheduleRepeatingJob(any(RepeatingSchedulableJob.class));
        when(swcService.getById(anyLong())).thenReturn(getFrontLineWorker());
        when(swcService.getByContactNumber(anyLong())).thenReturn(getFrontLineWorker());
        when(swcService.getById(anyLong())).thenReturn(getFrontLineWorker());
        courseNotificationService.updateSmsStatus(event);
        assertTrue(ccr.getLastDeliveryStatus().equals("DeliveryImpossible"));
        assertEquals(1, ccr.getNotificationRetryCount());
    }

    private Swachchagrahi getFrontLineWorker() {
        Swachchagrahi flw = new Swachchagrahi("Unit Test Babu", 12L);
        State state = new State("TN", 333L);
        District district = new District();
        district.setState(state);
        district.setCode(444L);
        state.setDistricts(new HashSet<>(Arrays.asList(district)));
        flw.setId(123456L);
        flw.setState(state);
        flw.setDistrict(district);
        return flw;
    }

    @Test
    public void testStatusUpdateNotificationMaxNoRetry() {
        MotechEvent event = new MotechEvent();
        event.getParameters().put("address", "tel: 9876543210");
        event.getParameters().put("deliveryStatus", "DeliveryImpossible");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 34, "score", true, true, 1);
        ccr.setModificationDate(DateTime.now().minusDays(1));
        assertNull(ccr.getLastDeliveryStatus());
        assertEquals(1, ccr.getNotificationRetryCount());

        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findByFlwId(anyLong())).thenReturn(records);
        when(settingsFacade.getProperty(anyString())).thenReturn("1");
        doNothing().when(schedulerService).safeScheduleRepeatingJob(any(RepeatingSchedulableJob.class));
        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setId(123456L);
        when(swcService.getById(anyLong())).thenReturn(flw);
        when(swcService.getByContactNumber(anyLong())).thenReturn(flw);
        when(swcService.getById(anyLong())).thenReturn(flw);
        courseNotificationService.updateSmsStatus(event);
        assertTrue(ccr.getLastDeliveryStatus().equals("DeliveryImpossible"));
        assertEquals(1, ccr.getNotificationRetryCount());
    }

    @Test
    public void testStatusUpdateNotificationScheduler() {
        MotechEvent event = new MotechEvent();
        event.getParameters().put("address", "tel: 9876543210");
        event.getParameters().put("deliveryStatus", "DeliveryImpossible");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 34, "score", true);
        ccr.setModificationDate(DateTime.now());
        assertNull(ccr.getLastDeliveryStatus());
        assertEquals(0, ccr.getNotificationRetryCount());

        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findByFlwId(anyLong())).thenReturn(records);
        when(settingsFacade.getProperty(anyString())).thenReturn("1");
        doNothing().when(schedulerService).safeScheduleRepeatingJob(any(RepeatingSchedulableJob.class));
        Swachchagrahi flw = new Swachchagrahi(1234567890L);
        flw.setId(123456L);
        when(swcService.getById(anyLong())).thenReturn(flw);
        when(swcService.getByContactNumber(anyLong())).thenReturn(flw);
        when(swcService.getById(anyLong())).thenReturn(flw);
        courseNotificationService.updateSmsStatus(event);
        assertTrue(ccr.getLastDeliveryStatus().equals("DeliveryImpossible"));
        assertEquals(0, ccr.getNotificationRetryCount());
    }

    @Test(expected = CourseNotCompletedException.class)
    public void testNotificationTriggerException() {
        when(courseCompletionRecordDataService.findByFlwId(anyLong())).thenReturn(null);
        washAcademyService.triggerCompletionNotification(123456L);
    }

    @Test
    public void testNotificationTriggerValidNew() {
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 22, "score");
        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findByFlwId(anyLong())).thenReturn(records);
        washAcademyService.triggerCompletionNotification(123456L);
        washAcademyService.triggerCompletionNotification(123456L);
        assertFalse(ccr.isSentNotification());
    }

    @Test
    public void testNotificationTriggerValidExisting() {
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 22, "score", true);
        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findByFlwId(anyLong())).thenReturn(records);

        when(courseCompletionRecordDataService.update(any(CourseCompletionRecord.class))).thenAnswer(
                new Answer<CourseCompletionRecord>() {
                    @Override
                    public CourseCompletionRecord answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        return (CourseCompletionRecord) args[0];
                    }
                }
        );

    }

    private InputStream getFileInputStream(String fileName) {

        try {
            return new FileInputStream(
                    new File(
                            Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResource("nmsCourseTest.json")
                                    .getPath()));
        } catch (IOException io) {
            return null;
        }
    }
}
