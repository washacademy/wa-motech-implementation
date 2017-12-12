package org.motechproject.nms.rejectionhandler.service;

import org.motechproject.nms.rejectionhandler.domain.SwcImportRejection;

/**
 * Created by vishnu on 15/7/17.
 */
public interface SwcRejectionService {

    SwcImportRejection findBySwcIdAndPanchayatId(Long swcId, Long panchayatId);

    void createUpdate(SwcImportRejection swcImportRejection);

}
