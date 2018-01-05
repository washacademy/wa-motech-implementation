package org.motechproject.wa.region.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.region.domain.Circle;

public interface CircleDataService  extends MotechDataService<Circle> {
    @Lookup
    Circle findByName(@LookupField(name = "name") String name);
}
