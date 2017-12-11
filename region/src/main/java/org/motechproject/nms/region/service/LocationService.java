package org.motechproject.nms.region.service;

import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.domain.Panchayat;
import org.motechproject.nms.region.exception.InvalidLocationException;

import java.util.Map;

/**
 * Location service to get and validate location data
 */
public interface LocationService {

    /**
     * Get locations method that fetches the associated location code from the mapping
     * @param locationMapping mapping codes for location hierarchy
     * @return mapping of code to location object
     * @throws InvalidLocationException when the map of code set violates the location hierarchy
     */
    Map<String, Object> getLocations(Map<String, Object> locationMapping) throws InvalidLocationException;

    /**
     * Get locations method that fetches the associated location code from the mapping
     * @param locationMapping mapping codes for location hierarchy
     * @param createIfNotExist creates the location hierarchy if it doesnt exist already
     * @return mapping of code to location object
     * @throws InvalidLocationException when the map of code set violates the location hierarchy
     */
    Map<String, Object> getLocations(Map<String, Object> locationMapping, boolean createIfNotExist) throws InvalidLocationException;

    State getState(Long stateId);

    District getDistrict(Long stateId, Long districtId);

    Block getBlock(Long stateId, Long districtId, String blockId);

    Panchayat getPanchayat(Long stateId, Long districtId, String blockId, Long vCode, Long svid);

    Panchayat getCensusPanchayat(Long stateId, Long districtId, String blockId, Long vCode);

    Panchayat getNonCensusPanchayat(Long stateId, Long districtId, String blockId, Long svid);

}
