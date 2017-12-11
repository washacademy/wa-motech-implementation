package org.motechproject.nms.region.service;

import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.domain.Panchayat;

public interface PanchayatService {
    Panchayat findByBlockAndVcodeAndSvid(Block block, long vcode, long svid);
    Panchayat create(Panchayat panchayat);
    Panchayat update(Panchayat panchayat);
}
