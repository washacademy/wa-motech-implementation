package org.motechproject.wa.api.utils;


import org.motechproject.wa.api.web.contract.washAcademy.CourseResponse;
import org.motechproject.wa.washacademy.dto.WaCourse;

/**
 * Helper to generate a course response
 */
public final class CourseBuilder {

    public static CourseResponse generateValidCourseResponse() {
        CourseResponse response = new CourseResponse();
        response.setName("WashAcademyCourse");
        response.setCourseVersion(20150526L);
        response.setChapters("[]");
        return response;
    }

    public static WaCourse generateValidCourseDto() {
        WaCourse course = new WaCourse();
        course.setName("WashAcademyCourse");
        course.setVersion(20150526L); // random, supposed to be millis eventually
        course.setContent("[{}]");
        return course;
    }
}
