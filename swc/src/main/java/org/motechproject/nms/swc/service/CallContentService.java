package org.motechproject.nms.swc.service;

import org.motechproject.nms.swc.domain.CallContent;

public interface CallContentService {
    void add(CallContent callContent);

    void update(CallContent record);

    void delete(CallContent record);
}
