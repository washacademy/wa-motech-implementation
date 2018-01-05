package org.motechproject.wa.swc.service;

import org.motechproject.wa.region.domain.State;

public interface WhitelistService {
    boolean numberWhitelistedForState(State state, Long contactNumber);
}
