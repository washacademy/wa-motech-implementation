package org.motechproject.wa.region.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.region.domain.Panchayat;

public interface PanchayatDataService extends MotechDataService<Panchayat> {
    @Lookup
    Panchayat findByName(@LookupField(name = "name") String name);

    @Lookup
    Panchayat findByCode(@LookupField(name = "vcode") Long vcode);
}
