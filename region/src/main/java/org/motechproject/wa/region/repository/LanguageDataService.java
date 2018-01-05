package org.motechproject.wa.region.repository;

import org.motechproject.mds.annotations.Lookup;
import org.motechproject.mds.annotations.LookupField;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.wa.region.domain.Language;


public interface LanguageDataService extends MotechDataService<Language> {
    @Lookup
    Language findByName(@LookupField(name = "name") String name);

    @Lookup
    Language findByCode(@LookupField(name = "code") String code);
}
