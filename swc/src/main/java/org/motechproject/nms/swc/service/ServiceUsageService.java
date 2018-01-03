package org.motechproject.nms.swc.service;

import org.motechproject.nms.swc.domain.Swachchagrahi;
import org.motechproject.nms.swc.domain.ServiceUsage;
import org.motechproject.nms.props.domain.Service;


public interface ServiceUsageService {
    ServiceUsage getCurrentMonthlyUsageForSWCAndService(final Swachchagrahi swachchagrahi, final Service service);
}
