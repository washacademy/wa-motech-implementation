package org.motechproject.nms.testing.it.api.utils;

import org.motechproject.nms.swc.domain.SwachchagrahiStatus;
import org.motechproject.nms.swc.domain.SwcJobStatus;
import org.motechproject.nms.swc.domain.Swachchagrahi;

/**
 * Api test helper with static methods
 */
public final class ApiTestHelper {

    public static Swachchagrahi createFlw(String name, Long phoneNumber, String mctsFlwId, SwachchagrahiStatus status) {
        Swachchagrahi flw = new Swachchagrahi(name, phoneNumber);
        flw.setCourseStatus(status);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        return flw;
    }
}
