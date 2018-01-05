package org.motechproject.wa.api.ut;

import org.junit.Test;
import org.motechproject.wa.api.utils.CourseBuilder;
import org.motechproject.wa.api.web.contract.washAcademy.CourseResponse;
import org.motechproject.wa.api.web.converter.WashAcademyConverter;
import org.motechproject.wa.washacademy.dto.WaCourse;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for converting between MA dto and MA api response objects
 */
public class WashAcademyConverterUnitTest {

    @Test
    public void TestCourseConversion() {

        // ideally this course would be generated like the call above, but not enough time now
        WaCourse course = WashAcademyConverter.convertCourseResponse(CourseBuilder.generateValidCourseResponse());
        CourseResponse response = WashAcademyConverter.convertCourseDto(course);
        assertNotNull(course);
    }

    // TODO: more tests when the json course ingestion is completed.

}
