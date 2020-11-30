package org.motechproject.wa.washacademy.service.impl;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.config.core.constants.ConfigurationConstants;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.mtraining.domain.ActivityRecord;
import org.motechproject.mtraining.domain.ActivityState;
import org.motechproject.mtraining.domain.Bookmark;
import org.motechproject.mtraining.repository.ActivityDataService;
import org.motechproject.mtraining.repository.BookmarkDataService;
import org.motechproject.mtraining.service.ActivityService;
import org.motechproject.mtraining.service.BookmarkService;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.washacademy.domain.CourseCompletionRecord;
import org.motechproject.wa.washacademy.domain.MtrainingModuleActivityRecordAudit;
import org.motechproject.wa.washacademy.domain.WaCourse;
import org.motechproject.wa.washacademy.dto.WaBookmark;
import org.motechproject.wa.washacademy.exception.CourseNotCompletedException;
import org.motechproject.wa.washacademy.repository.CourseCompletionRecordDataService;
import org.motechproject.wa.washacademy.repository.MtrainingModuleActivityRecordAuditDataService;
import org.motechproject.wa.washacademy.repository.WaCourseDataService;
import org.motechproject.wa.washacademy.service.WashAcademyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of the {@link WashAcademyService} interface.
 */
@Service("washAcademyService")
public class WashAcademyServiceImpl implements WashAcademyService {

    private static final String COURSE_CONTENT_FILE = "WaCourse.json";

    private static final String FINAL_BOOKMARK = "COURSE_COMPLETED";

    private static final String COURSE_COMPLETED = "wa.wa.course.completed";

    private static final String SCORES_KEY = "scoresByChapter";

    private static final String BOOKMARK_KEY = "bookmark";

    private static final String NOT_COMPLETE = "<%s: Course not complete>";

    private static final String COURSE_ENTITY_NAME = "WA.Course";

    private static final int MILLIS_PER_SEC = 1000;

    /**
     * Bookmark service to get and set bookmarks
     */
    private BookmarkService bookmarkService;

    /**
     * Activity service to track user completion data
     */
    private ActivityService activityService;

    /**
     * Completion record data service
     */

    private CourseCompletionRecordDataService courseCompletionRecordDataService;

    private SwcService swcService;

    /**
     * Activity record data service
     */
    private ActivityDataService activityDataService;

    /**
     * wa course data service
     */
    private BookmarkDataService bookmarkDataService;

    /**
     * wa course data service
     */
    private WaCourseDataService waCourseDataService;

    /**
     * Eventing system for course completion processing
     */
    private EventRelay eventRelay;

    /**
     * Used to retrieve course data
     */
    private SettingsFacade settingsFacade;

    /**
     * Used for alerting
     */
    private AlertService alertService;

    private MtrainingModuleActivityRecordAuditDataService mtrainingModuleActivityRecordAuditDataService;

    private static final Logger LOGGER = LoggerFactory.getLogger(WashAcademyServiceImpl.class);

    @Autowired
    public WashAcademyServiceImpl(BookmarkService bookmarkService,
                                  ActivityService activityService,
                                  WaCourseDataService waCourseDataService,
                                  ActivityDataService activityDataService,
                                  CourseCompletionRecordDataService courseCompletionRecordDataService,
                                  SwcService swcService,
                                  EventRelay eventRelay,
                                  MtrainingModuleActivityRecordAuditDataService mtrainingModuleActivityRecordAuditDataService,BookmarkDataService bookmarkDataService,
                                  @Qualifier("maSettings") SettingsFacade settingsFacade,
                                  AlertService alertService) {
        this.bookmarkService = bookmarkService;
        this.activityService = activityService;
        this.waCourseDataService = waCourseDataService;
        this.activityDataService = activityDataService;
        this.eventRelay = eventRelay;
        this.settingsFacade = settingsFacade;
        this.alertService = alertService;
        this.mtrainingModuleActivityRecordAuditDataService = mtrainingModuleActivityRecordAuditDataService;
        this.courseCompletionRecordDataService = courseCompletionRecordDataService;
        this.swcService = swcService;
        this.bookmarkDataService = bookmarkDataService;
        bootstrapCourse();
    }

    @Override
    public org.motechproject.wa.washacademy.dto.WaCourse getCourse(Integer courseId ) {
        WaCourse course = waCourseDataService.getCourseById(courseId);
        if (course == null) {
            alertService.create(COURSE_ENTITY_NAME, "Course For Given CourseId", "Could not find course", AlertType.CRITICAL, AlertStatus.NEW, 0, null);
            throw new IllegalStateException("No course bootstrapped. Check deployment");
        }
        return mapCourseDomainToDto(course);
    }

    @Override
    public void setCourse(org.motechproject.wa.washacademy.dto.WaCourse courseDto) {

        if (courseDto == null) {
            LOGGER.error("Attempted to set null course, exiting operation");
            alertService.create(COURSE_ENTITY_NAME, "WaCourse", "Trying to set null WaCourse", AlertType.CRITICAL, AlertStatus.NEW, 0, null);
            return;
        }

        setOrUpdateCourse(courseDto);
    }

    @Override
    public long getCourseVersion(Integer courseId) {
        WaCourse course = waCourseDataService.getCourseById(courseId);
        if (course == null) {
            alertService.create(COURSE_ENTITY_NAME, "Course For Given CourseId", "Could not find course", AlertType.CRITICAL, AlertStatus.NEW, 0, null);
            throw new IllegalStateException("No course bootstrapped. Check deployment");
        }
        return course.getModificationDate().getMillis() / MILLIS_PER_SEC;  //Unix epoch is represented in seconds
    }

    public Bookmark getBookmarkByUserIdAndCourseName (String swcId, String courseName ){
        List<Bookmark> bookmarks = this.bookmarkDataService.findBookmarksForUser(swcId);
        if (bookmarks != null){
            LOGGER.info(String.valueOf(bookmarks));
            Bookmark bookmark = new Bookmark();
            for (int i = 0; i < bookmarks.size(); i++) {
                if (bookmarks.get(i).getCourseIdentifier().equals(courseName)){
                    bookmark = bookmarks.get(i);
                    break;
                }
            }
            return bookmark;
        }
        return new Bookmark();

    }


    @Override
    public WaBookmark getBookmark(Long callingNumber, String callId, Integer courseId) {

        Swachchagrahi swc = swcService.getByContactNumberAndCourseId(callingNumber,courseId);
        if (swc == null) {
            return null;
        }
        WaCourse currentCourse = waCourseDataService.getCourseById(courseId);
        if(currentCourse == null){
            alertService.create(COURSE_ENTITY_NAME, "Course For Given CourseId", "Could not find course", AlertType.CRITICAL, AlertStatus.NEW, 0, null);
            throw new IllegalStateException("No course bootstrapped. Check deployment");
        }
        String courseName = currentCourse.getName();
         String swcId = swc.getId().toString();
//        Bookmark existingBookmark = bookmarkService.getLatestBookmarkByUserId(swcId.toString());
        Bookmark existingBookmark = getBookmarkByUserIdAndCourseName(swcId, courseName);
//        if (existingBookmark != null) {
            WaBookmark toReturn = setMaBookmarkProperties(existingBookmark);
            toReturn.setCallId(callId);
            return toReturn;
//        }



    }


    @Override
    public List<Bookmark> getAllBookmarks(){

    List<Swachchagrahi> allSwc = swcService.getRecords();
    List<Bookmark> allBookmarks = new ArrayList<>();

    for(int i=0;i<allSwc.size();i++){
        List<Bookmark> bookmarks = this.bookmarkDataService.findBookmarksForUser(allSwc.get(i).getId().toString());
       if(bookmarks.size()!=0){
           if(bookmarks.get(0).getCourseIdentifier().equalsIgnoreCase("RA1")){
               allBookmarks.add(bookmarks.get(0));
           }
       }
    }
        return allBookmarks;
    }
//    // Method to change swc.domain bookmark to Bookmark object.
//    private Bookmark mapSwcBookmarkToBookmark(org.motechproject.wa.swc.domain.Bookmark existingBookmark) {
//
//        Bookmark swcBookmarkToBookmark = new Bookmark();
//        swcBookmarkToBookmark.setExternalId(existingBookmark.getExternalId());
//        swcBookmarkToBookmark.setCourseIdentifier(existingBookmark.getCourseIdentifier());
//        swcBookmarkToBookmark.setChapterIdentifier(existingBookmark.getChapterIdentifier());
//        swcBookmarkToBookmark.setLessonIdentifier(existingBookmark.getLessonIdentifier());
//        swcBookmarkToBookmark.setProgress(existingBookmark.getProgress());
//        return swcBookmarkToBookmark;
//    }


    @Override
    public WaBookmark getBookmarkOps(Long callingNumber) {
        LOGGER.debug("Retrieve bookmark by Ops");
        Bookmark existingBookmark = bookmarkService.getLatestBookmarkByUserId(callingNumber.toString());
        if (existingBookmark != null) {
            WaBookmark toReturn = new WaBookmark();
            toReturn.setSwcId(Long.parseLong(existingBookmark.getExternalId()));

            // default behavior to map the data
            if (existingBookmark.getProgress() != null) {
                Object bookmark = existingBookmark.getProgress().get(BOOKMARK_KEY);
                toReturn.setBookmark(bookmark == null ? null : bookmark.toString());
                toReturn.setScoresByChapter((Map<String, Integer>) existingBookmark.getProgress().get(SCORES_KEY));
            }
        }

        return null;
    }


    @Override
    public void setBookmark(WaBookmark saveBookmark, Integer courseId) {

        if (saveBookmark == null) {
            LOGGER.error("Bookmark cannot be null, check request");
            throw new IllegalArgumentException("Invalid bookmark, cannot be null");
        }

        WaCourse currentCourse = waCourseDataService.getCourseById(courseId);

        if(currentCourse == null){
            alertService.create(COURSE_ENTITY_NAME, "Course with given courseId", "Could not find course", AlertType.CRITICAL, AlertStatus.NEW, 0, null);
            throw new IllegalStateException("No course bootstrapped. Check deployment");
        }

        String courseName  = currentCourse.getName();
        int passingMarks = currentCourse.getPassingScore();
        int noOfChapters = currentCourse.getNoOfChapters();

        String swcId = saveBookmark.getSwcId().toString();
        List<Bookmark> bookmarks = bookmarkService.getAllBookmarksForUser(swcId);
        Bookmark existingBookmark = null;
        for (int i = 0; i< bookmarks.size(); i++){
            if (bookmarks.get(i).getCourseIdentifier().equals(courseName)){
                existingBookmark = bookmarks.get(i);
                break;
            }
        }

        Swachchagrahi swc = swcService.getById(saveBookmark.getSwcId());
        String callingNumber = swc.getContactNumber().toString();

        // write a new activity record if existing bookmark is null or
        // existing bookmark has no progress from earlier reset


        if (existingBookmark == null ||
                (existingBookmark.getProgress() != null && existingBookmark.getProgress().isEmpty()))  {
            activityService.createActivity(
                    new ActivityRecord(callingNumber, courseName, null, null, DateTime.now(), null, ActivityState.STARTED));
        }


        if (existingBookmark == null) {
            // if no bookmarks exist for user
            LOGGER.info("No bookmarks found for user " + LogHelper.obscure(saveBookmark.getSwcId()));
            Bookmark bookmark = setBookmarkProperties(saveBookmark, new Bookmark());
            bookmark.setCourseIdentifier(courseName);
            bookmarkService.createBookmark(bookmark);
        } else {

            // update the first bookmark
            LOGGER.info("Updating bookmark for user");
            bookmarkService.updateBookmark(setBookmarkProperties(saveBookmark, existingBookmark));
        }

        if (saveBookmark.getBookmark() != null
                && saveBookmark.getBookmark().equals(FINAL_BOOKMARK)
                && saveBookmark.getScoresByChapter() != null
                && saveBookmark.getScoresByChapter().size() == noOfChapters) {

            LOGGER.debug("Found last bookmark and all scores. Starting evaluation & notification");
            // Create an activity record here since pass/fail counts as 1 try
            activityService.createActivity(
                    new ActivityRecord(callingNumber, courseName, null, null, null, DateTime.now(), ActivityState.COMPLETED));
            evaluateCourseCompletion(saveBookmark.getSwcId(), saveBookmark.getScoresByChapter(), courseId, courseName, passingMarks, noOfChapters);
        }
    }
//    @Override
//    public void setBookmark(WaBookmark saveBookmark, Integer courseId) {
//
//        if (saveBookmark == null) {
//            LOGGER.error("Bookmark cannot be null, check request");
//            throw new IllegalArgumentException("Invalid bookmark, cannot be null");
//        }
//        WaCourse currentCourse = waCourseDataService.getCourseById(courseId);
//        if(currentCourse == null){
//            alertService.create(COURSE_ENTITY_NAME, "Course For Given CourseId", "Could not find course", AlertType.CRITICAL, AlertStatus.NEW, 0, null);
//            throw new IllegalStateException("No course bootstrapped. Check deployment");
//        }
//        String courseName = currentCourse.getName();
//
//
//        String swcId = saveBookmark.getSwcId().toString();
//        List<Bookmark> bookmarks = bookmarkService.getAllBookmarksForUser(swcId);
//        Bookmark existingBookmark = null;
//        for (int i = 0; i< bookmarks.size(); i++){
//            if (bookmarks.get(i).getCourseIdentifier().equals(courseName)){
//                existingBookmark = bookmarks.get(i);
//                break;
//            }
//        }
//
//        Swachchagrahi swc = swcService.getById(saveBookmark.getSwcId());
//        String callingNumber = swc.getContactNumber().toString();
//
//        // write a new activity record if existing bookmark is null or
//        // existing bookmark has no progress from earlier reset
//
//
//        if (existingBookmark == null ||
//                (existingBookmark.getProgress() != null && existingBookmark.getProgress().isEmpty()))  {
//            activityService.createActivity(
//                    new ActivityRecord(callingNumber, courseName, null, null, DateTime.now(), null, ActivityState.STARTED));
//        }
//
//
//        if (existingBookmark == null) {
//            // if no bookmarks exist for user
//            LOGGER.info("No bookmarks found for user " + LogHelper.obscure(saveBookmark.getSwcId()));
//            Bookmark bookmark = setBookmarkProperties(saveBookmark, new Bookmark());
//            bookmark.setCourseIdentifier(courseName);
//            bookmarkService.createBookmark(bookmark);
//        } else {
//
//            // update the first bookmark
//            LOGGER.info("Updating bookmark for user");
//            bookmarkService.updateBookmark(setBookmarkProperties(saveBookmark, existingBookmark));
//        }
//
//        if (saveBookmark.getBookmark() != null
//                && saveBookmark.getBookmark().equals(FINAL_BOOKMARK)
//                && saveBookmark.getScoresByChapter() != null
//                && saveBookmark.getScoresByChapter().size() == CHAPTER_COUNT) {
//
//            LOGGER.debug("Found last bookmark and 11 scores. Starting evaluation & notification");
//            // Create an activity record here since pass/fail counts as 1 try
//            activityService.createActivity(
//                    new ActivityRecord(callingNumber, courseName, null, null, null, DateTime.now(), ActivityState.COMPLETED));
//            evaluateCourseCompletion(saveBookmark.getSwcId(), saveBookmark.getScoresByChapter(), courseName, courseId);
//        }
//    }

    @Override
    public void triggerCompletionNotification(final Long swcId, String courseName, Integer courseId) {

        List<CourseCompletionRecord> ccrs = courseCompletionRecordDataService.findBySwcIdAndCourseId(swcId, courseId);

        if (ccrs == null || ccrs.isEmpty()) {
            throw new CourseNotCompletedException(String.format(NOT_COMPLETE, swcId));
        }

        final CourseCompletionRecord ccr = ccrs.get(ccrs.size() - 1);

        if (ccr.isSentNotification()) {
            LOGGER.error("Notification has already been sent.");
            return;
        }

        // If this is running inside a transaction (which it probably always will), then send the event after
        // the db commit. Else, most likely in a test, send it right away
        // https://github.com/motech-implementations/mim/issues/518
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    sendEvent(ccr.getSwcId(), courseName, courseId);
                }
            });
        } else {
            sendEvent(ccr.getSwcId(), courseName, courseId);
        }
    }

    /**
     * Send event to notify
     * @param swcId swc ID to notify
     */
    private void sendEvent(Long swcId, String courseName, Integer courseId) {

        Map<String, Object> eventParams = new HashMap<>();
        eventParams.put("swcId", swcId);
        eventParams.put("courseName", courseName);
        eventParams.put("courseId", courseId);
        MotechEvent motechEvent = new MotechEvent(COURSE_COMPLETED, eventParams);
        eventRelay.sendEventMessage(motechEvent);
        LOGGER.debug("Sent event message to process completion notification");
    }

    // Map the dto to the domain object
    private Bookmark setBookmarkProperties(WaBookmark fromBookmark, Bookmark toBookmark) {

        toBookmark.setExternalId(fromBookmark.getSwcId().toString());

        if (toBookmark.getProgress() == null) {
            toBookmark.setProgress(new HashMap<String, Object>());
        }
        toBookmark.getProgress().put("callId", fromBookmark.getCallId());

        // This guarantees that we always update to the latest scores
        if (fromBookmark.getScoresByChapter() != null) {
            toBookmark.getProgress().put(SCORES_KEY, fromBookmark.getScoresByChapter());
        }

        String bookmark = fromBookmark.getBookmark();
        if (bookmark != null) {
            toBookmark.getProgress().put(BOOKMARK_KEY, bookmark);
        }

        return toBookmark;
    }

    // Map domain object to dto
    private WaBookmark setMaBookmarkProperties(Bookmark fromBookmark) {

        WaBookmark toReturn = new WaBookmark();
        if (!(fromBookmark.getExternalId() == null || fromBookmark.getExternalId().isEmpty())) {
            toReturn.setSwcId(Long.parseLong(fromBookmark.getExternalId()));

            // default behavior to map the data
            if (fromBookmark.getProgress() != null) {
                Object bookmark = fromBookmark.getProgress().get(BOOKMARK_KEY);
                toReturn.setBookmark(bookmark == null ? null : bookmark.toString());
                toReturn.setScoresByChapter((Map<String, Integer>) fromBookmark.getProgress().get(SCORES_KEY));
            }

            // if the bookmark is final, reset it
            if (toReturn.getBookmark() != null && toReturn.getBookmark().equals(FINAL_BOOKMARK)) {
                LOGGER.debug("We need to reset bookmark to new state.");
                fromBookmark.setProgress(new HashMap<String, Object>());
                bookmarkService.updateBookmark(fromBookmark);

                toReturn.setScoresByChapter(null);
                toReturn.setBookmark(null);
            }

            return toReturn;

        }
        else {
            return new WaBookmark();
        }
    }


    /**
     * Helper method to check whether a course meets completion criteria
     * @param swcId swc Id of swc
     * @param scores scores in quiz
     */
    private void evaluateCourseCompletion(Long swcId, Map<String, Integer> scores,Integer courseId, String courseName, Integer passingMarks, Integer noOfChapters) {

        int totalScore = getTotalScore(scores,noOfChapters);

        CourseCompletionRecord ccr = new CourseCompletionRecord(swcId, totalScore, scores.toString(), courseId );
        courseCompletionRecordDataService.create(ccr);

        if (totalScore < passingMarks) {
            LOGGER.debug("User with swcId: " + LogHelper.obscure(swcId) + " failed with score: " + totalScore);
            ccr.setPassed(false);
            courseCompletionRecordDataService.update(ccr);
            return;
        } else {
            // we updated the completion record. Start event message to trigger notification workflow
            ccr.setPassed(true);
            courseCompletionRecordDataService.update(ccr);
            triggerCompletionNotification(swcId, courseName, courseId);
        }
    }

    /**
     * Get total scores from all chapters
     * @param scoresByChapter scores by chapter
     * @return total score
     */
    private static int getTotalScore(Map<String, Integer> scoresByChapter, Integer noOfChapters) {

        if (scoresByChapter == null) {
            return 0;
        }

        int totalScore = 0;
        for (int chapterCount = 1; chapterCount <= noOfChapters; chapterCount++) {

            totalScore += scoresByChapter.get(String.valueOf(chapterCount));
        }

        return totalScore;
    }

    @MotechListener(subjects = {
            ConfigurationConstants.FILE_CHANGED_EVENT_SUBJECT,
            ConfigurationConstants.FILE_CREATED_EVENT_SUBJECT })
    @Transactional
    public void handleCourseChanges(MotechEvent event) {

        String filePath = (String) event.getParameters().get(ConfigurationConstants.FILE_PATH);

        if (filePath.contains(COURSE_CONTENT_FILE)) {
            LOGGER.debug("Got notification for course data change, reloading course");
            bootstrapCourse();
        } else {
            LOGGER.debug("Course file not in path, back to sleep: " + filePath);
        }

    }

    private void bootstrapCourse() {
        org.motechproject.wa.washacademy.dto.WaCourse course = new org.motechproject.wa.washacademy.dto.WaCourse();
        try (InputStream is = settingsFacade.getRawConfig(COURSE_CONTENT_FILE)) {
            String jsonText = IOUtils.toString(is);
            JSONObject jo = new JSONObject(jsonText);
            // TODO: validate the json format here
            course.setName(jo.get("name").toString());
            course.setContent(jo.get("chapters").toString());
            course.setCourseId(1);
            setOrUpdateCourse(course);
        }
        catch (Exception e) {
            LOGGER.error("Error while reading course json. Check file. Exception: " + e.toString());
            alertService.create(COURSE_ENTITY_NAME, "WaCourse", "Error reading course json", AlertType.CRITICAL, AlertStatus.NEW, 0, null);
        }
    }

    private void setOrUpdateCourse(org.motechproject.wa.washacademy.dto.WaCourse courseDto) {
        WaCourse existing = waCourseDataService.getCourseById(courseDto.getCourseId());

        if (existing == null) {
            waCourseDataService.create(new WaCourse(courseDto.getName(), courseDto.getContent(), courseDto.getCourseId()));
            LOGGER.debug("Successfully created new course");
            return;
        }

        if (existing.getContent().equals(courseDto.getContent())) {
            LOGGER.debug("Found no changes in course data, dropping update");
        } else {
            existing.setContent(courseDto.getContent());
            waCourseDataService.update(existing);
            LOGGER.debug("Found updated to course data and did the needful");
        }
    }

    private org.motechproject.wa.washacademy.dto.WaCourse mapCourseDomainToDto(WaCourse course) {

        org.motechproject.wa.washacademy.dto.WaCourse courseDto = new org.motechproject.wa.washacademy.dto.WaCourse();
        courseDto.setName(course.getName());
        courseDto.setVersion(course.getModificationDate().getMillis() / MILLIS_PER_SEC);
        courseDto.setContent(course.getContent());
        courseDto.setCourseId(course.getCourseId());
        return courseDto;
    }

    public String getScoresForUser(Long callingNumber) {
        LOGGER.debug("Fetching scores in service");
        String scores = "{000000}";
        Bookmark existingBookmark = bookmarkService.getLatestBookmarkByUserId(callingNumber.toString());
        if (existingBookmark != null && existingBookmark.getProgress() != null) {
            Map<String, Integer> scoreMap = (Map<String, Integer>) existingBookmark.getProgress().get(SCORES_KEY);
            scores = scoreMap.toString();
            LOGGER.debug("Returning real scores for user");
        } else {
            LOGGER.debug("No scores found for user");
        }

        return scores;
    }

    @Override
    public void updateMsisdn(Long id, Long oldCallingNumber, Long newCallingNumber) {

        if ((newCallingNumber == null) || newCallingNumber.equals(oldCallingNumber)) {
            return;
        }
        // Update Msisdn  In MTRAINING_MODULE_BOOKMARK
        LOGGER.debug("Fetching Bookmarks for Msisdn {}.", oldCallingNumber);
        List<Bookmark> existingBookmarks = bookmarkService.getAllBookmarksForUser(oldCallingNumber.toString());
        if (existingBookmarks.size() > 0) {
            int i;
            Bookmark bookmark;
            for (i = 0; i < existingBookmarks.size(); i++) {
                bookmark = existingBookmarks.get(i);
                bookmark.setExternalId(newCallingNumber.toString());
                bookmarkService.updateBookmark(bookmark);
            }
            LOGGER.debug("Updated MSISDN {} to {} in {} Bookmarks", oldCallingNumber, newCallingNumber, i);
        } else {
            LOGGER.debug("No Bookmarks exists with given Msisdn");
        }

    // Update Msisdn  In MTRAINING_MODULE_ACTIVITYRECORD
        LOGGER.debug("Fetching Activity records for Msisdn {}", oldCallingNumber);
        List<ActivityRecord> existingRecords = activityDataService.findRecordsForUser(oldCallingNumber.toString());
        if (existingRecords.size() > 0) {
            int i;
            ActivityRecord activityRecord;
            for (i = 0; i < existingRecords.size(); i++) {
                activityRecord = existingRecords.get(i);
                activityRecord.setExternalId(newCallingNumber.toString());
                activityDataService.update(activityRecord);
            }
            mtrainingModuleActivityRecordAuditDataService.create(new MtrainingModuleActivityRecordAudit(id, oldCallingNumber, newCallingNumber));
            LOGGER.debug("Updated MSISDN {} to {} in {} Activity records", oldCallingNumber, newCallingNumber, i);
        } else {
            LOGGER.debug("No Activity records exists with given Msisdn");
        }
    }

    @Autowired
    public void setBookmarkService(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }
}
