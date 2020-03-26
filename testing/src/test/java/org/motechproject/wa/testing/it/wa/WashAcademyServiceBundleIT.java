package org.motechproject.wa.testing.it.wa;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.event.MotechEvent;
import org.motechproject.mtraining.domain.ActivityState;
import org.motechproject.mtraining.domain.Bookmark;
import org.motechproject.mtraining.repository.ActivityDataService;
import org.motechproject.mtraining.repository.BookmarkDataService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.repository.CircleDataService;
import org.motechproject.wa.region.repository.LanguageDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.DistrictService;
import org.motechproject.wa.region.service.LanguageService;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwachchagrahiStatus;
import org.motechproject.wa.swc.domain.SwcJobStatus;
import org.motechproject.wa.swc.repository.SwcDataService;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.testing.service.TestingService;
import org.motechproject.wa.washacademy.domain.CourseCompletionRecord;
import org.motechproject.wa.washacademy.domain.WaCourse;
import org.motechproject.wa.washacademy.dto.WaBookmark;
import org.motechproject.wa.washacademy.exception.CourseNotCompletedException;
import org.motechproject.wa.washacademy.repository.CourseCompletionRecordDataService;
import org.motechproject.wa.washacademy.repository.WaCourseDataService;
import org.motechproject.wa.washacademy.service.CourseNotificationService;
import org.motechproject.wa.washacademy.service.WashAcademyService;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Verify that WashAcademyService present, functional.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class WashAcademyServiceBundleIT extends BasePaxIT {

    @Inject
    WashAcademyService maService;

    @Inject
    BookmarkDataService bookmarkDataService;

    @Inject
    ActivityDataService activityDataService;

    @Inject
    CourseCompletionRecordDataService courseCompletionRecordDataService;

    @Inject
    SwcDataService swcDataService;

    @Inject
    WaCourseDataService waCourseDataService;

    @Inject
    CourseNotificationService courseNotificationService;

    @Inject
    LanguageDataService languageDataService;

    @Inject
    LanguageService languageService;

    @Inject
    StateDataService stateDataService;

    @Inject
    CircleDataService circleDataService;

    @Inject
    DistrictService districtService;

    @Inject
    SwcService swcService;

    @Inject
    TestingService testingService;

    @Inject
    PlatformTransactionManager transactionManager;

    private static final String VALID_COURSE_NAME = "WashAcademyCourse";

    private static final String FINAL_BOOKMARK = "COURSE_COMPLETED";

    private static final String VALID_CALL_ID = "1234567890123456789012345";

    @Before
    public void setupWashAcademy() {

        courseCompletionRecordDataService.deleteAll();
        activityDataService.deleteAll();
        bookmarkDataService.deleteAll();
        testingService.clearDatabase();
    }

    @Test
    public void testSetCourseNoUpdate() throws IOException {
        setupWaCourse();
        WaCourse originalCourse = waCourseDataService.getCourseByName(VALID_COURSE_NAME);
        org.motechproject.wa.washacademy.dto.WaCourse copyCourse = new org.motechproject.wa.washacademy.dto.WaCourse(originalCourse.getName(), originalCourse.getModificationDate().getMillis(), originalCourse.getContent(),originalCourse.getCourseId());
        maService.setCourse(copyCourse);

        // verify that modified time (version) didn't change
        assertEquals(waCourseDataService.getCourseByName(VALID_COURSE_NAME).getModificationDate(),
                originalCourse.getModificationDate());
    }
    @Ignore
    @Test
    public void testSetCourseUpdate() throws IOException {
        setupWaCourse();
        WaCourse originalCourse = waCourseDataService.getCourseByName(VALID_COURSE_NAME);
        String courseContent = originalCourse.getContent();
        org.motechproject.wa.washacademy.dto.WaCourse copyCourse = new org.motechproject.wa.washacademy.dto.WaCourse(originalCourse.getName(), originalCourse.getModificationDate().getMillis(), originalCourse.getContent() + "foo", 1);
        maService.setCourse(copyCourse);

        // verify that modified time (version) did change
        assertNotEquals(waCourseDataService.getCourseByName(VALID_COURSE_NAME).getModificationDate(),
                originalCourse.getModificationDate());
        originalCourse.setContent(courseContent);
        waCourseDataService.update(originalCourse);
    }
    @Ignore
    @Test
    public void testNoCoursePresent() throws IOException {
        setupWaCourse();
        WaCourse originalCourse = waCourseDataService.getCourseByName(VALID_COURSE_NAME);
        waCourseDataService.delete(originalCourse);
        assertNull(waCourseDataService.getCourseByName(VALID_COURSE_NAME));

        try {
            maService.getCourse(1);
        } catch (IllegalStateException is) {
            assertTrue(is.toString().contains("No course bootstrapped. Check deployment"));
        }

        waCourseDataService.create(new WaCourse(originalCourse.getName(), originalCourse.getContent(), originalCourse.getCourseId()));
    }

    @Test
    public void testWashAcademyServicePresent() throws Exception {
        assertNotNull(maService);
    }

    @Test
    public void testGetCourse() throws IOException {
        setupWaCourse();
        assertNotNull(maService.getCourse(1));
    }

    @Test
    public void testGetCourseVersion() throws IOException {
        setupWaCourse();
        assertNotNull(maService.getCourseVersion(1));
        assertTrue(maService.getCourseVersion(1) > 0);
    }

    @Ignore
    @Test
    public void testGetBookmark() throws IOException {
        setupWaCourse();
        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        String swcId = swcService.getByContactNumber(1234567890L).getId().toString();
        bookmarkDataService.create(new Bookmark(swcId, "1", "1", "1", new HashMap<String, Object>()));
        assertNotNull(maService.getBookmark(1234567890L, VALID_CALL_ID,1));
    }

    @Test
    public void testGetEmptyBookmark() {

        assertNull(maService.getBookmark(123L, VALID_CALL_ID,1));
    }

    @Test
    public void testSetNullBookmark() {
        try {
            maService.setBookmark(null,1);
            throw new IllegalStateException("This test expected an IllegalArgumentException");
        } catch (IllegalArgumentException ia) {
            assertTrue(ia.toString().contains("cannot be null"));
        }
    }

    @Test
    public void testSetNewBookmark() throws IOException {
        setupWaCourse();
        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(1234567890L);
        List<Bookmark> existing = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == (existing.size() + 1));
    }

    @Test
    public void testStartedActivity() throws IOException {
        setupWaCourse();
        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(1234567890L);
        List<Bookmark> existing = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == (existing.size() + 1));
        assertEquals(1, activityDataService.findRecordsForUserByState(swc.getContactNumber().toString(), ActivityState.STARTED).size());
    }

    @Test
    public void testSetExistingBookmark() throws IOException {
        setupWaCourse();
        Swachchagrahi swc = new Swachchagrahi(1234567890L);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(1234567890L);
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == 1);

        bookmark.setBookmark("Chapter3_Lesson2");
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Quiz1", 4);
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);

        WaBookmark retrieved = maService.getBookmark(1234567890L, VALID_CALL_ID,1);
        assertNotNull(retrieved.getBookmark());
        assertTrue(retrieved.getBookmark().equals("Chapter3_Lesson2"));
        assertNotNull(retrieved.getScoresByChapter());
        assertTrue(retrieved.getScoresByChapter().get("Quiz1") == 4);
    }

    @Test
    public void testSetLastBookmark() throws IOException {
        setupWaCourse();
        long callingNumber = 9876543210L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == 1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), ((int) (Math.random() * 100)) % 5);
        }
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);
    }

    @Test
    public void testCompletionCount() throws IOException {
        setupWaCourse();
        long callingNumber = 9876543210L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        int completionCountBefore = activityDataService.findRecordsForUser(String.valueOf(callingNumber)).size();
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == 1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), ((int) (Math.random() * 100)) % 5);
        }
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);
        int completionCountAfter = activityDataService.findRecordsForUserByState(String.valueOf(callingNumber), ActivityState.COMPLETED).size();

        assertEquals(completionCountBefore + 1, completionCountAfter);
    }

    @Test
    public void testSetGetLastBookmark() throws IOException {
        setupWaCourse();
        long callingNumber = 9987654321L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == 1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 1);
        }
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);

        WaBookmark retrieved = maService.getBookmark(callingNumber, VALID_CALL_ID,1);
        assertNotNull(retrieved.getSwcId());
        assertNotNull(retrieved.getCallId());
        assertNull(retrieved.getBookmark());
        assertNull(retrieved.getScoresByChapter());
    }

    @Test
    public void testSetGetResetBookmark() throws IOException {
        setupWaCourse();
        long callingNumber = 9987654321L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == 1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 4);
        }
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);

        WaBookmark retrieved = maService.getBookmark(callingNumber, VALID_CALL_ID,1);
        assertNotNull(retrieved.getSwcId());
        assertNotNull(retrieved.getCallId());
        assertNull(retrieved.getBookmark());
        assertNull(retrieved.getScoresByChapter());
    }

    @Test
    public void testResetBookmarkNewStartActivity() throws IOException {
        setupWaCourse();
        long callingNumber = 9987654321L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == 1);

        // set final bookmark and trigger completed activity record
        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 4);
        }
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);

        // this beforeCount includes completed activity now
        int beforeCount = activityDataService.findRecordsForUser(String.valueOf(callingNumber)).size();

        // verify that the bookmark is reset on the following get call
        WaBookmark retrieved = maService.getBookmark(callingNumber, VALID_CALL_ID,1);
        assertNotNull(retrieved.getSwcId());
        assertNotNull(retrieved.getCallId());
        assertNull(retrieved.getBookmark());
        assertNull(retrieved.getScoresByChapter());

        // set new bookmark to trigger started activity
        bookmark.setBookmark("Chapter01_Lesson01");
        scores.clear();
        scores.put("1", 3);
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark, 1);

        // this will now include the new start activity
        int afterCount = activityDataService.findRecordsForUser(String.valueOf(callingNumber)).size();

        // verify that we added a new activity since the last completion
        assertEquals(beforeCount + 1, afterCount);
    }

    @Test
    public void testTriggerNotificationSent() throws IOException {
        setupWaCourse();
        long callingNumber = 9876543210L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcDataService.create(swc);
        swc = swcService.getByContactNumber(callingNumber);

        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(String.valueOf(swc.getId()));
        assertTrue(added.size() == 1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 3);
        }
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);
        Long swcId = swcService.getByContactNumber(callingNumber).getId();
        CourseCompletionRecord ccr = courseCompletionRecordDataService.findBySwcId(swcId).get(0);
        assertNotNull(ccr);
        assertEquals(ccr.getSwcId(), swcId);
        assertEquals(ccr.getScore(), 33);
    }

    @Test
    public void testTriggerNotificationNotSent() throws IOException {
        setupWaCourse();
        long callingNumber = 9876543211L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcDataService.create(swc);
        swc = swcService.getByContactNumber(callingNumber);
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(String.valueOf(swc.getId()));
        assertTrue(added.size() == 1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), 1);
        }
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);
        assertEquals(1, courseCompletionRecordDataService.findBySwcId(swc.getId()).size());
        assertFalse(courseCompletionRecordDataService.findBySwcId(swc.getId()).get(0).isPassed());
    }

    @Test
    public void testRetriggerNotification() {

        long callingNumber = 9876543211L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        Long swcId = swcService.getByContactNumber(callingNumber).getId();
        WaCourse waCourse = waCourseDataService.getCourseByName("WashAcademyCourse");

        CourseCompletionRecord ccr = new CourseCompletionRecord(swcId, 44, "score", true,1);
        courseCompletionRecordDataService.create(ccr);

        maService.triggerCompletionNotification(swcId,"WashAcademyCourse",1);
        List<CourseCompletionRecord> ccrs = courseCompletionRecordDataService.findBySwcId(swcId);
        ccr = ccrs.get(ccrs.size()-1);
        assertTrue(ccr.isSentNotification());
    }

    @Test(expected = CourseNotCompletedException.class)
    public void testRetriggerNotificationException() {

        long callingNumber = 9876543222L;
        maService.triggerCompletionNotification(callingNumber,"WashAcademyCourse",1);
    }

    @Test
    public void testNotification() {
        long callingNumber = 2111113333L;

        // Setup language/location and swc for notification
        Swachchagrahi swc = swcService.getByContactNumber(callingNumber);
        if (swc != null) {
            swc.setCourseStatus(SwachchagrahiStatus.INVALID);
            swcService.update(swc);
            swcService.delete(swc);
        }

        createLanguageLocationData();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        State sampleState = stateDataService.findByCode(1L);
        Language language = languageService.getForCode("50");
        swc = new Swachchagrahi("Test Worker", callingNumber);
        swc.setLanguage(language);
        swc.setState(sampleState);
        swc.setDistrict(sampleState.getDistricts().iterator().next());
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        assertNotNull(swc);
        transactionManager.commit(status);

        MotechEvent event = new MotechEvent();
        event.getParameters().put("callingNumber", callingNumber);
        event.getParameters().put("smsContent", "FooBar");
        WaCourse waCourse = waCourseDataService.getCourseByName("WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(callingNumber, 35, "score", false,1);
        courseCompletionRecordDataService.create(ccr);
        courseNotificationService.sendSmsNotification(event);
        // TODO: cannot check the notification status yet since we don't have a real IMI url to hit
    }

    @Test
    public void testNotificationNoLocation() {
        long callingNumber = 2111113333L;

        // Setup swc for notification (without language/location)
        Swachchagrahi swc = swcService.getByContactNumber(callingNumber);
        if (swc != null) {
            swc.setCourseStatus(SwachchagrahiStatus.INVALID);
            swcService.update(swc);
            swcService.delete(swc);
        }

        swc = new Swachchagrahi("Test Worker", callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        assertNotNull(swc);

        MotechEvent event = new MotechEvent();
        event.getParameters().put("callingNumber", callingNumber);
        event.getParameters().put("smsContent", "FooBar");
        WaCourse waCourse = waCourseDataService.getCourseByName("WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(callingNumber, 35, "score", false,1);
        courseCompletionRecordDataService.create(ccr);
        courseNotificationService.sendSmsNotification(event);
        // TODO: cannot check the notification status yet since we don't have a real IMI url to hit
    }

    @Test
    @Ignore
    public void testSmsReference() {
        long callingNumber = 2111113333L;

        // Setup language/location and swc for notification
        Swachchagrahi swc = swcService.getByContactNumber(callingNumber);
        if (swc != null) {
            swc.setCourseStatus(SwachchagrahiStatus.INVALID);
            swcService.update(swc);
            swcService.delete(swc);
        }

        createLanguageLocationData();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        State sampleState = stateDataService.findByCode(1L);
        swc = new Swachchagrahi("Test Worker", callingNumber);
        swc.setState(sampleState);
        swc.setDistrict(sampleState.getDistricts().iterator().next());
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        assertNotNull(swc);
        Long swcId = swc.getId();
        transactionManager.commit(status);

        MotechEvent event = new MotechEvent();
        event.getParameters().put("swcId", swcId);
        event.getParameters().put("smsContent", "FooBar");
        WaCourse waCourse = waCourseDataService.getCourseByName("WashAcademyCourse");
        CourseCompletionRecord ccr = new CourseCompletionRecord(swcId, 35, "score", false,1);
        courseCompletionRecordDataService.create(ccr);
        assertNull(ccr.getSmsReferenceNumber());

        courseNotificationService.sendSmsNotification(event);
        CourseCompletionRecord smsCcr = courseCompletionRecordDataService.findBySwcId(swcId).get(0);
        assertNotNull(smsCcr.getSmsReferenceNumber());
        String expectedCode = "" + swc.getState().getCode() + swc.getDistrict().getCode() + callingNumber + 0; // location code + callingNumber + tries
        assertEquals(expectedCode, smsCcr.getSmsReferenceNumber());
    }

    @Test
    public void testMultipleCompletions() throws IOException {
        setupWaCourse();
        long callingNumber = 9876543210L;
        Swachchagrahi swc = new Swachchagrahi(callingNumber);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        swcService.add(swc);
        swc = swcService.getByContactNumber(callingNumber);
        WaBookmark bookmark = new WaBookmark(swc.getId(), VALID_CALL_ID, null, null);
        maService.setBookmark(bookmark,1);
        List<Bookmark> added = bookmarkDataService.findBookmarksForUser(swc.getId().toString());
        assertTrue(added.size() == 1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores.put(String.valueOf(i), ((int) (Math.random() * 100)) % 5);
        }
        String chapterwiseScore = scores.toString();
        bookmark.setScoresByChapter(scores);
        maService.setBookmark(bookmark,1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores1 = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores1.put(String.valueOf(i), 3);
        }
        String chapterwiseScore1 = scores1.toString();
        bookmark.setScoresByChapter(scores1);
        maService.setBookmark(bookmark,1);

        bookmark.setBookmark(FINAL_BOOKMARK);
        Map<String, Integer> scores2 = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            scores2.put(String.valueOf(i), 1);
        }
        String chapterwiseScore2 = scores2.toString();
        bookmark.setScoresByChapter(scores2);
        maService.setBookmark(bookmark,1);

        Long swcId = swcService.getByContactNumber(callingNumber).getId();
        List<CourseCompletionRecord> ccrs = courseCompletionRecordDataService.findBySwcId(swcId);
        assertEquals(3, ccrs.size());
        assertEquals(4, activityDataService.findRecordsForUser((String.valueOf(callingNumber))).size());
        assertEquals(ActivityState.STARTED, activityDataService.findRecordsForUser((String.valueOf(callingNumber))).get(0).getState());
        assertEquals(ActivityState.COMPLETED, activityDataService.findRecordsForUser((String.valueOf(callingNumber))).get(1).getState());
        assertEquals(ActivityState.COMPLETED, activityDataService.findRecordsForUser((String.valueOf(callingNumber))).get(2).getState());
        assertEquals(ActivityState.COMPLETED, activityDataService.findRecordsForUser((String.valueOf(callingNumber))).get(3).getState());
        assertEquals(33, ccrs.get(1).getScore());
        assertEquals(11, ccrs.get(2).getScore());
        assertTrue(ccrs.get(1).isPassed());
        assertFalse(ccrs.get(2).isPassed());
        assertEquals(chapterwiseScore, ccrs.get(0).getChapterWiseScores());
        assertEquals(chapterwiseScore1, ccrs.get(1).getChapterWiseScores());
        assertEquals(chapterwiseScore2, ccrs.get(2).getChapterWiseScores());
    }

    private void createLanguageLocationData() {
        Language ta = languageService.getForCode("50");
        if (ta == null) {
            ta = languageDataService.create(new Language("50", "hin"));
        }

        State state = stateDataService.findByCode(1L);

        if (state == null) {
            District district = new District();
            district.setName("District 1");
            district.setRegionalName("District 1");
            district.setLanguage(ta);
            district.setCode(1L);

            state = new State();
            state.setName("State 1");
            state.setCode(1L);
            state.getDistricts().add(district);
            stateDataService.create(state);
        }
    }

    /**
     * setup MA course structure from WaCourse.json file.
     */
    private JSONObject setupWaCourse() throws IOException {
        org.motechproject.wa.washacademy.dto.WaCourse course = new org.motechproject.wa.washacademy.dto.WaCourse();
        InputStream fileStream = getFileInputStream("WaCourse.json");
        String jsonText = IOUtils.toString(fileStream);
        JSONObject jo = new JSONObject(jsonText);
        course.setName(jo.get("name").toString());
        course.setContent(jo.get("chapters").toString());
        waCourseDataService.create(new WaCourse(course.getName(), course.getContent(), 1));
        fileStream.close();
        return jo;
    }

    private InputStream getFileInputStream(String fileName) {
        return getClass().getClassLoader().getResourceAsStream(fileName);
    }

}
