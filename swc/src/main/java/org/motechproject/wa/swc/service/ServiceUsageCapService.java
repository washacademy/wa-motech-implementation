package org.motechproject.wa.swc.service;

import org.motechproject.wa.swc.domain.ServiceUsageCap;
import org.motechproject.wa.props.domain.Service;
import org.motechproject.wa.region.domain.State;

public interface ServiceUsageCapService {
    ServiceUsageCap getServiceUsageCap(final State state, final Service service);
}
