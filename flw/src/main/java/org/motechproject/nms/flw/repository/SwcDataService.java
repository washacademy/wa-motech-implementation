package org.motechproject.nms.flw.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.nms.flw.domain.SwcJobStatus;
import org.motechproject.nms.flw.domain.Swachchagrahi;

import java.util.List;

public interface SwcDataService extends MotechDataService<Swachchagrahi> {
    @Lookup
    Swachchagrahi findBySwcId(@LookupField(name = "swcId") String swcId);

    @Lookup
    List<Swachchagrahi> findByContactNumberAndJobStatus(@LookupField(name = "contactNumber") Long contactNumber,
                                                        @LookupField(name = "jobStatus") SwcJobStatus jobStatus);
}
