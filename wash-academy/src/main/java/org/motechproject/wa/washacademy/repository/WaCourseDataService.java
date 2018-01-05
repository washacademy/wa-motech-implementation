package org.motechproject.wa.washacademy.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.washacademy.domain.WaCourse;

/**
 * data service to perform CRUD operations on WaCourse
 */
public interface WaCourseDataService extends MotechDataService<WaCourse> {

    @Lookup
    WaCourse getCourseByName(@LookupField(name = "name") String name);
}
