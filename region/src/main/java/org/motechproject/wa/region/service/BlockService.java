package org.motechproject.wa.region.service;

import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.District;

public interface BlockService {
    Block findByDistrictAndCode(District district, String code);
    Block create(Block block);
    Block update(Block block);
}
