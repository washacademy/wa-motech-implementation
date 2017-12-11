package org.motechproject.nms.testing.it.api.utils;

import org.motechproject.nms.flw.domain.SwachchagrahiStatus;
import org.motechproject.nms.flw.domain.SwcJobStatus;
import org.motechproject.nms.flw.domain.Swachchagrahi;

/**
 * Api test helper with static methods
 */
public final class ApiTestHelper {

    public static Swachchagrahi createFlw(String name, Long phoneNumber, String mctsFlwId, SwachchagrahiStatus status) {
        Swachchagrahi flw = new Swachchagrahi(name, phoneNumber);
        flw.setMctsFlwId(mctsFlwId);
        flw.setStatus(status);
        flw.setJobStatus(SwcJobStatus.ACTIVE);
        return flw;
    }
}
