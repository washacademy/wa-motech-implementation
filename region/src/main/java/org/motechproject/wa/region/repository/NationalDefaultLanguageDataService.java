package org.motechproject.wa.region.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.region.domain.NationalDefaultLanguage;

public interface NationalDefaultLanguageDataService extends MotechDataService<NationalDefaultLanguage> {
    @Lookup
    NationalDefaultLanguage findByCode(@LookupField(name = "code") Integer code);
}
