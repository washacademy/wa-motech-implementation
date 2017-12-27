package org.motechproject.nms.testing.it.api.utils;

import org.motechproject.nms.swc.domain.SwachchagrahiStatus;
import org.motechproject.nms.swc.domain.SwcJobStatus;
import org.motechproject.nms.swc.domain.Swachchagrahi;

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
