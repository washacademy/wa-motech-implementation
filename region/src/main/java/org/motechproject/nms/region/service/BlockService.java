package org.motechproject.nms.region.service;

import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.domain.District;

public interface BlockService {
    Block findByDistrictAndCode(District district, String code);
    Block create(Block block);
    Block update(Block block);
}
