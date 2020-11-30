package org.motechproject.wa.region.service;

import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.domain.State;

import java.util.Set;

public interface DistrictService {
    /**
     * Get all districts for a given language
     * @param language language to lookup
     * @return set of districts
     */
    Set<District> getAllForLanguage(Language language);

    /**
     * Find district in a state with given code
     * @param state State to find district in
     * @param code district code to lookup
     * @return District object
     */
    District findByStateAndCode(State state, Long code);

    District findById(Long id);

    /**
     * Find district in a state with given district name
     * @param state State to find district in
     * @param name name of the district
     * @return District object
     */
    District findByStateAndName(State state, String name);

    /**
     * Create a given district
     * @param district district to create
     * @return Created district object
     */
    District create(District district);

    /**
     * Update the given district properties
     * @param district district object to persist
     * @return updated district object
     */
    District update(District district);

    /**
     * Helper to get detached field for a district
     * @param district District to use
     * @param fieldName name of the field object
     * @return field object in district
     */
    Object getDetachedField(District district, String fieldName);
}
