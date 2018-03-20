package org.motechproject.wa.testing.it.api.utils;

import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwachchagrahiStatus;
import org.motechproject.wa.swc.domain.SwcJobStatus;

/**
 * Api test helper with static methods
 */
public final class ApiTestHelper {

    public static Swachchagrahi createSwc(String name, Long phoneNumber, String mctsSwcId, SwachchagrahiStatus status) {
        Swachchagrahi swc = new Swachchagrahi(name, phoneNumber);
        swc.setCourseStatus(status);
        swc.setJobStatus(SwcJobStatus.ACTIVE);
        return swc;
    }
}
