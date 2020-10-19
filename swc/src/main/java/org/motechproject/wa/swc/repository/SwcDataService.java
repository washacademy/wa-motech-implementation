package org.motechproject.wa.swc.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwcJobStatus;

import java.util.List;

public interface SwcDataService extends MotechDataService<Swachchagrahi> {
    @Lookup
    Swachchagrahi findBySwcIdAndCourseId(@LookupField(name = "swcId") String swcId,
                                         @LookupField(name = "courseId") Integer courseId);

    @Lookup
    List<Swachchagrahi> findByContactNumberAndJobStatus(@LookupField(name = "contactNumber") Long contactNumber,
                                                        @LookupField(name = "jobStatus") SwcJobStatus jobStatus);
}
