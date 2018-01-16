package org.motechproject.wa.region.service.impl;

import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.domain.Circle;
import org.motechproject.wa.region.exception.InvalidLocationException;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Location service impl to get location objects
 */
@Service("locationService")
public class LocationServiceImpl implements LocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationServiceImpl.class);

    private static final String INVALID = "<%s - %s : Invalid location>";
    private static final String STATE_ID = "StateID";
    private static final String DISTRICT_ID = "District_ID";
    private static final String BLOCK_ID = "Block_ID";
    private static final String BLOCK_NAME = "Block_Name";
    private static final String PANCHAYAT_ID = "Panchayat_ID";
    private static final String PANCHAYAT_NAME = "Panchayat_Name";

    private StateDataService stateDataService;

    private DistrictService districtService;

    private BlockService blockService;

    private PanchayatService panchayatService;

    private CircleService circleService;

    @Autowired
    public LocationServiceImpl(StateDataService stateDataService, DistrictService districtService,
                               BlockService blockService, PanchayatService panchayatService) {
        this.stateDataService = stateDataService;
        this.districtService = districtService;
        this.blockService = blockService;
        this.panchayatService = panchayatService;
    }


    private boolean isValidID(final Map<String, Object> map, final String key) {
        Object obj = map.get(key);
        if (obj == null) {
            return false;
        }

        if (obj.getClass().equals(Long.class)) {
            return (Long) obj > 0L;
        }

        return !"0".equals(obj);
    }

    public Map<String, Object> getLocations(Map<String, Object> map) throws InvalidLocationException {
       return getLocations(map, true);
    }

    @Override // NO CHECKSTYLE Cyclomatic Complexity
    @SuppressWarnings("PMD")
    public Map<String, Object> getLocations(Map<String, Object> map, boolean createIfNotExists) throws InvalidLocationException {

        Map<String, Object> locations = new HashMap<>();

        // set state
        if (!isValidID(map, STATE_ID)) {
            return locations;
        }
        State state = stateDataService.findByCode((Long) map.get(STATE_ID));
        if (state == null) { // we are here because stateId wasn't null but fetch returned no data
            throw new InvalidLocationException(String.format(INVALID, STATE_ID, map.get(STATE_ID)));
        }
        locations.put(STATE_ID, state);


        // set district
        if (!isValidID(map, DISTRICT_ID)) {
            return locations;
        }
        District district = districtService.findByStateAndCode(state, (Long) map.get(DISTRICT_ID));
        if (district == null) {
            throw new InvalidLocationException(String.format(INVALID, DISTRICT_ID, map.get(DISTRICT_ID)));
        }
        locations.put(DISTRICT_ID, district);


        // set and/or create block
        if (!isValidID(map, BLOCK_ID)) {
            return locations;
        }
        Block block = blockService.findByDistrictAndCode(district, (long) map.get(BLOCK_ID));
        if (block == null && createIfNotExists) {
            block = new Block();
            block.setCode((Long) map.get(BLOCK_ID));
            block.setName((String) map.get(BLOCK_NAME));
            block.setDistrict(district);
            district.getBlocks().add(block);
            LOGGER.debug(String.format("Created %s in %s with id %d", block, district, block.getId()));
        }
        locations.put(BLOCK_ID, block);


        // set and/or create panchayat
        Long vcode = map.get(PANCHAYAT_ID) == null ? 0 : (Long) map.get(PANCHAYAT_ID);
        if (vcode != 0) {
            Panchayat panchayat = panchayatService.findByBlockAndVcodeAndSvid(block, vcode, vcode);
            if (panchayat == null && createIfNotExists) {
                panchayat = new Panchayat();
                panchayat.setVcode(vcode);
                panchayat.setBlock(block);
                panchayat.setName((String) map.get(PANCHAYAT_NAME));
                block.getPanchayats().add(panchayat);
                LOGGER.debug(String.format("Created %s in %s with id %d", panchayat, block, panchayat.getId()));
            }
            locations.put(PANCHAYAT_ID, panchayat);
        }


        // set and/or create health block
        // set and/or create health facility

        // set and/or create health sub-facility
        
        return locations;
    }

    @Override
    public State getState(Long stateId) {

        return stateDataService.findByCode(stateId);
    }

    @Override
    public Circle getCircle(String circleName) {

        return circleService.getByName(circleName);
    }

    @Override
    public District getDistrict(Long stateId, Long districtId) {

        State state = getState(stateId);

        if (state != null) {
            return districtService.findByStateAndCode(state, districtId);
        }

        return null;
    }

    @Override
    public Block getBlock(Long stateId, Long districtId, Long blockId) {

        District district = getDistrict(stateId, districtId);

        if (district != null) {
            return blockService.findByDistrictAndCode(district, blockId);
        }

        return null;
    }

    @Override
    public Panchayat getPanchayat(Long stateId, Long districtId, Long blockId, Long vCode, Long svid) {

        Block block = getBlock(stateId, districtId, blockId);

        if (block != null) {
            return panchayatService.findByBlockAndVcodeAndSvid(block, vCode, svid);
        }

        return null;
    }

    @Override
    public Panchayat getCensusPanchayat(Long stateId, Long districtId, Long blockId, Long vCode) {

        return getPanchayat(stateId, districtId, blockId, vCode, 0L);
    }

    @Override
    public Panchayat getNonCensusPanchayat(Long stateId, Long districtId, Long blockId, Long svid) {

        return getPanchayat(stateId, districtId, blockId, 0L, svid);
    }
}
