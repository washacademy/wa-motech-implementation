package org.motechproject.wa.swc.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.swc.domain.SwcError;

import java.util.List;

/**
 * Data service to add swc sync error audit logs
 */
public interface SwcErrorDataService extends MotechDataService<SwcError> {

    @Lookup
    List<SwcError> findBySwcId(@LookupField(name = "swcId") String mctsId,
                                @LookupField(name = "stateId") Long stateId,
                                @LookupField(name = "districtId") Long districtId);
}
