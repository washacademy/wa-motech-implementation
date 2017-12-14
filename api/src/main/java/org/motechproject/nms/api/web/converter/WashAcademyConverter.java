package org.motechproject.nms.api.web.converter;

import org.motechproject.nms.api.web.contract.mobileAcademy.CourseResponse;
import org.motechproject.nms.api.web.contract.mobileAcademy.GetBookmarkResponse;
import org.motechproject.nms.api.web.contract.mobileAcademy.SaveBookmarkRequest;
import org.motechproject.nms.washacademy.dto.WaBookmark;
import org.motechproject.nms.washacademy.dto.WaCourse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translator helper module that converts from the service dto object to
 * API response object
 */
public final class WashAcademyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WashAcademyConverter.class);

    /**
     * Private constructor for static MA course converters
     */
    private WashAcademyConverter() {

    }

    /**
     * Converts the course service dto to api response object
     * @param course course dto
     * @return CourseResponse API object
     */
    public static CourseResponse convertCourseDto(WaCourse course) {

        LOGGER.debug("Converting course dto to response contract");
        CourseResponse response = new CourseResponse();
        response.setName(course.getName());
        response.setCourseVersion(course.getVersion());
        response.setChapters(course.getContent());
        return response;
    }

    /**
     * Convert course response api object back to course service dto (used by tests)
     * @param courseResponse course response object
     * @return course dto
     */
    public static WaCourse convertCourseResponse(CourseResponse courseResponse) {
        WaCourse course = new WaCourse();
        course.setName(courseResponse.getName());
        course.setVersion(courseResponse.getCourseVersion());
        course.setContent(courseResponse.getChapters().toString());
        return course;
    }

    /**
     * Convert bookmark dto to api response object
     * @param bookmark bookmark dto
     * @return api response object
     */
    public static GetBookmarkResponse convertBookmarkDto(WaBookmark bookmark) {
        GetBookmarkResponse response = new GetBookmarkResponse();
        if (bookmark != null) {
            response.setBookmark(bookmark.getBookmark());
            response.setScoresByChapter(bookmark.getScoresByChapter());
        }
        return response;
    }

    /**
     * Convert api request object to bookmark dto
     * @param saveBookmarkRequest api request object
     * @return bookmark dto
     */
    public static WaBookmark convertSaveBookmarkRequest(SaveBookmarkRequest saveBookmarkRequest, Long swcId) {
        WaBookmark bookmark = new WaBookmark();
        bookmark.setSwcId(swcId);
        bookmark.setCallId(saveBookmarkRequest.getCallId());
        bookmark.setBookmark(saveBookmarkRequest.getBookmark());
        bookmark.setScoresByChapter(saveBookmarkRequest.getScoresByChapter());
        return bookmark;
    }
}
