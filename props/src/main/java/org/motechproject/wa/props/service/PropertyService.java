package org.motechproject.wa.props.service;

import org.motechproject.wa.props.domain.Service;
import org.motechproject.wa.region.domain.State;

public interface PropertyService {
    boolean isServiceDeployedInState(Service service, State state);
}
