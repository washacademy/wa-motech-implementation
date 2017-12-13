package org.motechproject.nms.swc.service;

import org.motechproject.nms.region.domain.State;

public interface WhitelistService {
    boolean numberWhitelistedForState(State state, Long contactNumber);
}
