package org.motechproject.wa.rejectionhandler.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.rejectionhandler.domain.SwcImportRejection;

/**
 * Created by vishnu on 14/7/17.
 */
public interface SwcImportRejectionDataService extends MotechDataService<SwcImportRejection>  {

    @Lookup
    SwcImportRejection findBySwcIdAndPanchayatIdAndCourseId(@LookupField(name = "swcID") Long swcID,
                                                 @LookupField(name = "panchayatId") Long panchayatId,
                                                 @LookupField(name = "courseId") Integer courseId);

    @Lookup
    SwcImportRejection findBySwcIdAndCourseId(@LookupField(name = "swcID") Long swcID,
                                   @LookupField(name = "courseId") Integer courseId);
}
