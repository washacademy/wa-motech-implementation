package org.motechproject.nms.swc.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.nms.swc.domain.SwcStatusUpdateAudit;

import java.util.List;

/**
 * Data Service to CRUD on SwcStatusUpdateAudit
 */
public interface SwcStatusUpdateAuditDataService extends MotechDataService<SwcStatusUpdateAudit> {

    @Lookup
    List<SwcStatusUpdateAudit> findBySwcId(@LookupField(name = "swcId") String swcId);

    @Lookup
    List<SwcStatusUpdateAudit> findByContactNumber(@LookupField(name = "contactNumber") Long contactNumber);
}