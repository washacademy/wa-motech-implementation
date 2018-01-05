package org.motechproject.wa.imi.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.imi.domain.CallDetailRecord;

import java.util.List;

public interface CallDetailRecordDataService  extends MotechDataService<CallDetailRecord> {

    @Lookup
    List<CallDetailRecord> findByRequestIdAndCallId(@LookupField(name = "requestId") String requestId,
                                                    @LookupField(name = "callId") String callId);
}
