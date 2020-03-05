package org.motechproject.wa.washacademy.ut;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
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
import org.motechproject.mtraining.repository.BookmarkDataService;
import org.motechproject.mtraining.service.ActivityService;
import org.motechproject.mtraining.service.BookmarkService;
import org.motechproject.mtraining.service.MTrainingService;
import org.motechproject.scheduler.contract.RepeatingSchedulableJob;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.wa.imi.service.SmsNotificationService;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.washacademy.domain.CourseCompletionRecord;
import org.motechproject.wa.washacademy.domain.WaCourse;
import org.motechproject.wa.washacademy.dto.WaBookmark;
import org.motechproject.wa.washacademy.exception.CourseNotCompletedException;
import org.motechproject.wa.washacademy.repository.CourseCompletionRecordDataService;
import org.motechproject.wa.washacademy.repository.MtrainingModuleActivityRecordAuditDataService;
import org.motechproject.wa.washacademy.repository.WaCourseDataService;
import org.motechproject.wa.washacademy.service.WashAcademyService;
import org.motechproject.wa.washacademy.service.impl.CourseNotificationServiceImpl;
import org.motechproject.wa.washacademy.service.impl.WashAcademyServiceImpl;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
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
    private WaCourseDataService WaCourseDataService;

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
    private DistrictDataService districtDataService;

    @Mock
    private MtrainingModuleActivityRecordAuditDataService mtrainingModuleActivityRecordAuditDataService;

    @Mock
    private BookmarkDataService bookmarkDataService;

    private Validator validator;

    @Before
    public void setup() {
        initMocks(this);
        WaCourseDataService.deleteAll();
        when(settingsFacade.getRawConfig("WaCourse.json")).thenReturn(getFileInputStream("WaCourseTest.json"));
        washAcademyService = new WashAcademyServiceImpl(bookmarkService, activityService,
                WaCourseDataService, activityDataService, courseCompletionRecordDataService, swcService, eventRelay, mtrainingModuleActivityRecordAuditDataService,bookmarkDataService, settingsFacade, alertService);
        courseNotificationService = new CourseNotificationServiceImpl(smsNotificationService,
                    settingsFacade, activityService, schedulerService, courseCompletionRecordDataService, alertService,
                swcService,districtDataService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        when(activityService.createActivity(any(ActivityRecord.class))).thenReturn(new ActivityRecord());
    }

    @Test
    public void getCourseTest() {


        WaCourse newCourse = new WaCourse("WashAcademyCourse", "[]");
        newCourse.setModificationDate(DateTime.now());
        WaCourseDataService.create(newCourse);
        when(WaCourseDataService.getCourseByName("WashAcademyCourse")).thenReturn(newCourse);
        assertTrue(washAcademyService.getCourse(1).getContent().equals(newCourse.getContent()));

        WaCourse newCoursePlus = new WaCourse("WashAcademyCoursePlus", "[]");
        newCoursePlus.setModificationDate(DateTime.now());
        WaCourseDataService.create(newCoursePlus);
        when(WaCourseDataService.getCourseByName("WashAcademyCoursePlus")).thenReturn(newCoursePlus);
        assertTrue(washAcademyService.getCourse(2).getContent().equals(newCoursePlus.getContent()));
    }
    @Ignore
    @Test
    public void getBookmarkTest() {
        Bookmark newBookmark = new Bookmark("55", "WashAcademyCourse", null, null, null);

        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setId(55L);
        when(bookmarkService.getLatestBookmarkByUserId(anyString()))
                .thenReturn(newBookmark);
        when(swcService.getByContactNumber(anyLong())).thenReturn(swc);

        WaBookmark mab = washAcademyService.getBookmark(1234567890L, VALID_CALL_ID, 1);
        assertTrue(mab.getSwcId() == 55L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setBookmarkNullTest() {

        washAcademyService.setBookmark(null, 1);
    }

    @Test
    public void setNewBookmarkTest() {
        WaBookmark mab = new WaBookmark(123456L, VALID_CALL_ID, "Chapter1_Lesson1", null);

        Swachchagrahi swc = new Swachchagrahi(1000000000L);
        swc.setId(123456L);
        when(bookmarkService.createBookmark(any(Bookmark.class))).thenReturn(new Bookmark());
        when(bookmarkService.getLatestBookmarkByUserId(anyString())).thenReturn(null);
        when(swcService.getById(anyLong())).thenReturn(swc);
        washAcademyService.setBookmark(mab, 1);
    }

    @Test
    public void setUpdateBookmarkTest() {
        WaBookmark mab = new WaBookmark(123456L, VALID_CALL_ID, "Chapter1_Lesson1", null);

        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setId(123456L);
        when(bookmarkService.createBookmark(any(Bookmark.class)))
                .thenReturn(new Bookmark());
        when(bookmarkService.getLatestBookmarkByUserId(anyString()))
                .thenReturn(new Bookmark());
        when(swcService.getById(anyLong())).thenReturn(swc);
        washAcademyService.setBookmark(mab, 1);
    }

    @Test
    public void setLastBookmark() {
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), ((int) (Math.random() * 100)) % 5);
        }
        WaBookmark mab = new WaBookmark(123456L, VALID_CALL_ID, "COURSE_COMPLETED", scores);
        doNothing().when(eventRelay).sendEventMessage(any(MotechEvent.class));
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 22, scores.toString(), false, 1);
        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findBySwcIdAndCourseId(anyLong(),anyInt())).thenReturn(records);
        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setId(123456L);
        when(swcService.getById(anyLong())).thenReturn(swc);
        washAcademyService.setBookmark(mab, 1);
    }

    @Test
    public void setLastBookmarkFailingScore() {
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 0);
        }
        WaBookmark mab = new WaBookmark(123456L, VALID_CALL_ID, "COURSE_COMPLETED", scores);
        doNothing().when(eventRelay).sendEventMessage(any(MotechEvent.class));
        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setId(123456L);
        when(swcService.getById(anyLong())).thenReturn(swc);
        washAcademyService.setBookmark(mab, 1);
    }
    @Ignore
    @Test
    public void getLastBookmarkReset() {

        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 4);
        }

        Map<String, Object> progress = new HashMap<>();
        progress.put("scoresByChapter", scores);
        progress.put("bookmark", "COURSE_COMPLETED");
        Bookmark newBookmark = new Bookmark("55", "WashAcademyCourse", null, null, progress);
        when(bookmarkService.getLatestBookmarkByUserId(anyString()))
                .thenReturn(newBookmark);
        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setId(55L);
        when(swcService.getByContactNumber(anyLong())).thenReturn(swc);
        WaBookmark retrieved = washAcademyService.getBookmark(1234567890L, VALID_CALL_ID,1);
        assertNull(retrieved.getBookmark());
        assertNull(retrieved.getScoresByChapter());
    }

    @Test
    public void testStatusUpdateNotification() {
        MotechEvent event = new MotechEvent();
        event.getParameters().put("address", "tel: 9876543210");
        event.getParameters().put("deliveryStatus", "DeliveredToTerminal");
        event.getParameters().put("courseName", "WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 34, "score", true,1);
        ccr.setModificationDate(DateTime.now());
        assertNull(ccr.getLastDeliveryStatus());

        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findBySwcIdAndCourseId(anyLong(),anyInt())).thenReturn(records);
        when(settingsFacade.getProperty(anyString())).thenReturn("1");
        Swachchagrahi swc = new Swachchagrahi(1000000000L);
        swc.setSwcId("123456");
        when(swcService.getByContactNumber(anyLong())).thenReturn(swc);
        courseNotificationService.updateSmsStatus(event);
        assertTrue(ccr.getLastDeliveryStatus().equals("DeliveredToTerminal"));
    }
    @Ignore
    @Test
    public void testStatusUpdateNotificationRetry() {
        MotechEvent event = new MotechEvent();
        event.getParameters().put("address", "tel: 9876543210");
        event.getParameters().put("deliveryStatus", "DeliveryImpossible");
        event.getParameters().put("courseName", "WashAcademyCourse");
        WaCourse waCourse = WaCourseDataService.getCourseByName("WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 34, "score", true, 1);
        ccr.setModificationDate(DateTime.now().minusDays(1));
        assertNull(ccr.getLastDeliveryStatus());
        assertEquals(0, ccr.getNotificationRetryCount());

        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findBySwcIdAndCourseId(anyLong(),anyInt())).thenReturn(records);
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
        Swachchagrahi swc = new Swachchagrahi("Unit Test Babu", 12L);
        State state = new State("TN", 333L);
        District district = new District();
        district.setState(state);
        district.setCode(444L);
        state.setDistricts(new HashSet<>(Arrays.asList(district)));
        swc.setId(123456L);
        swc.setState(state);
        swc.setDistrict(district);
        return swc;
    }
    @Ignore
    @Test
    public void testStatusUpdateNotificationMaxNoRetry() {
        MotechEvent event = new MotechEvent();
        event.getParameters().put("address", "tel: 9876543210");
        event.getParameters().put("deliveryStatus", "DeliveryImpossible");
        event.getParameters().put("courseName", "WashAcademyCourse");
        WaCourse waCourse = WaCourseDataService.getCourseByName("WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 34, "score", true,  1);
        ccr.setModificationDate(DateTime.now().minusDays(1));
        assertNull(ccr.getLastDeliveryStatus());
        assertEquals(0, ccr.getNotificationRetryCount());

        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findBySwcIdAndCourseId(123456L,1)).thenReturn(records);
        when(settingsFacade.getProperty(anyString())).thenReturn("1");
        doNothing().when(schedulerService).safeScheduleRepeatingJob(any(RepeatingSchedulableJob.class));
        Swachchagrahi swc = new Swachchagrahi(1000000000L);
        swc.setId(123456L);
        when(swcService.getById(anyLong())).thenReturn(swc);
        when(swcService.getByContactNumber(anyLong())).thenReturn(swc);
        when(swcService.getById(anyLong())).thenReturn(swc);
        courseNotificationService.updateSmsStatus(event);
        assertTrue(ccr.getLastDeliveryStatus().equals("DeliveryImpossible"));
        assertEquals(1, ccr.getNotificationRetryCount());
    }

    @Test
    public void testStatusUpdateNotificationScheduler() {
        MotechEvent event = new MotechEvent();
        event.getParameters().put("address", "tel: 9876543210");
        event.getParameters().put("deliveryStatus", "DeliveryImpossible");
        event.getParameters().put("courseName", "WashAcademyCourse");
        WaCourse waCourse = WaCourseDataService.getCourseByName("WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 34, "score", true, 1);
        ccr.setModificationDate(DateTime.now());
        assertNull(ccr.getLastDeliveryStatus());
        assertEquals(0, ccr.getNotificationRetryCount());

        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findBySwcIdAndCourseId(anyLong(),anyInt())).thenReturn(records);
        when(settingsFacade.getProperty(anyString())).thenReturn("1");
        doNothing().when(schedulerService).safeScheduleRepeatingJob(any(RepeatingSchedulableJob.class));
        Swachchagrahi swc = new Swachchagrahi(1000000000L);
        swc.setId(123456L);
        when(swcService.getById(anyLong())).thenReturn(swc);
        when(swcService.getByContactNumber(anyLong())).thenReturn(swc);
        when(swcService.getById(anyLong())).thenReturn(swc);
        courseNotificationService.updateSmsStatus(event);
        assertTrue(ccr.getLastDeliveryStatus().equals("DeliveryImpossible"));
        assertEquals(0, ccr.getNotificationRetryCount());
    }

    @Test(expected = CourseNotCompletedException.class)
    public void testNotificationTriggerException() {
        when(courseCompletionRecordDataService.findBySwcId(anyLong())).thenReturn(null);
        washAcademyService.triggerCompletionNotification(123456L, "WashAcademyCourse",1);
    }
    @Ignore
    @Test
    public void testNotificationTriggerValidNew() {
        WaCourse waCourse = WaCourseDataService.getCourseByName("WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 22, "score",1);
        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findBySwcIdAndCourseId(123456L,1)).thenReturn(records);
        washAcademyService.triggerCompletionNotification(123456L,"WashAcademyCourse",1);
        washAcademyService.triggerCompletionNotification(123456L, "WashAcademyCourse",1);
        assertFalse(ccr.isSentNotification());
    }

    @Test
    public void testNotificationTriggerValidExisting() {
        WaCourse waCourse = WaCourseDataService.getCourseByName("WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(123456L, 22, "score", true,1);
        List<CourseCompletionRecord> records = new ArrayList<>();
        records.add(ccr);
        when(courseCompletionRecordDataService.findBySwcId(anyLong())).thenReturn(records);

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
                                    .getResource("WaCourseTest.json")
                                    .getPath()));
        } catch (IOException io) {
            return null;
        }
    }
}
