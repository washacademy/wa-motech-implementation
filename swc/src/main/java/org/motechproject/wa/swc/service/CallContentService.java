package org.motechproject.wa.swc.service;

import org.motechproject.wa.swc.domain.CallContent;

public interface CallContentService {
    void add(CallContent callContent);

    void update(CallContent record);

    void delete(CallContent record);
}
