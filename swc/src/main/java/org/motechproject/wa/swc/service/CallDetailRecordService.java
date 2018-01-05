package org.motechproject.wa.swc.service;

import org.motechproject.wa.swc.domain.CallDetailRecord;

public interface CallDetailRecordService {
    void add(CallDetailRecord callDetailRecord);

    CallDetailRecord getByCallingNumber(long callingNumber);

    void update(CallDetailRecord record);

    void delete(CallDetailRecord record);
}
