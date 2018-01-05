package org.motechproject.wa.api.web.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.api.web.domain.InactiveJobCallAudit;

import java.util.List;

/**
 * Data service to CRUD on Inactive Job Swc Call audit
 */

public interface InactiveJobCallAuditDataService extends MotechDataService<InactiveJobCallAudit> {

    @Lookup
    List<InactiveJobCallAudit> findByNumber(@LookupField(name = "callingNumber") Long callingNumber);

    @Lookup
    List<InactiveJobCallAudit> findBySwcId(@LookupField(name = "swcId") Long swcId);
}
