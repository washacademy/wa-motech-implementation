package org.motechproject.wa.region.service;

import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.Panchayat;

public interface PanchayatService {
    Panchayat findByBlockAndVcodeAndSvid(Block block, long vcode, long svid);
    Panchayat create(Panchayat panchayat);
    Panchayat update(Panchayat panchayat);
}
