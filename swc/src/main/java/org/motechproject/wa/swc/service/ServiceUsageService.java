package org.motechproject.wa.swc.service;

import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.ServiceUsage;
import org.motechproject.wa.props.domain.Service;


public interface ServiceUsageService {
    ServiceUsage getCurrentMonthlyUsageForSWCAndService(final Swachchagrahi swachchagrahi, final Service service);
}
