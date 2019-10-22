package org.motechproject.wa.swc.service;

import org.motechproject.wa.props.domain.Service;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.swc.domain.ServiceUsageCap;

public interface ServiceUsageCapService {
    ServiceUsageCap getServiceUsageCap(final State state, final Service service, Integer courseId);
}
