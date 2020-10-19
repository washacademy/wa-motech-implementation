package org.motechproject.wa.rejectionhandler.service;

import org.motechproject.wa.rejectionhandler.domain.SwcImportRejection;

/**
 * Created by vishnu on 15/7/17.
 */
public interface SwcRejectionService {

    SwcImportRejection findBySwcIdAndPanchayatIdAndCourseId(Long swcId, Long panchayatId, Integer courseId);

    void createUpdate(SwcImportRejection swcImportRejection);

}
