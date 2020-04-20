package org.motechproject.wa.washacademy.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.washacademy.domain.CourseCompletionRecord;

import java.util.List;

/**
 * data interface to create and update completion record for course
 */
public interface CourseCompletionRecordDataService extends MotechDataService<CourseCompletionRecord> {

    @Lookup
    List<CourseCompletionRecord> findBySwcId(@LookupField(name = "swcId") Long swcId);

    @Lookup
    List<CourseCompletionRecord> findBySwcIdAndCourseId(@LookupField(name = "swcId" ) Long swcId, @LookupField(name = "courseId") Integer courseId);

    @Lookup
    CourseCompletionRecord findBySwcIdAndCourseIdAndClientCorrelator(@LookupField(name = "swcId" ) Long swcId, @LookupField(name = "courseId") Integer courseId, @LookupField(name = "clientCorrelator") String clientCorrelator);
}
