package org.motechproject.nms.swc.service;

import org.motechproject.nms.swc.domain.CallDetailRecord;

public interface CallDetailRecordService {
    void add(CallDetailRecord callDetailRecord);

    CallDetailRecord getByCallingNumber(long callingNumber);

    void update(CallDetailRecord record);

    void delete(CallDetailRecord record);
}
