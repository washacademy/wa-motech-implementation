package org.motechproject.nms.flw.service;

import org.motechproject.nms.flw.domain.Swachchagrahi;
import org.motechproject.nms.flw.domain.ServiceUsage;
import org.motechproject.nms.props.domain.Service;


public interface ServiceUsageService {
    ServiceUsage getCurrentMonthlyUsageForFLWAndService(final Swachchagrahi swachchagrahi, final Service service);
}
