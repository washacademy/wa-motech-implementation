package org.motechproject.nms.region.csv.impl;

import org.motechproject.nms.csv.utils.GetLong;
import org.motechproject.nms.csv.utils.GetString;
import org.motechproject.nms.csv.utils.Store;
import org.motechproject.nms.region.csv.PanchayatImportService;
import org.motechproject.nms.region.domain.Panchayat;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.service.BlockService;
import org.motechproject.nms.region.service.DistrictService;
import org.motechproject.nms.region.service.PanchayatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.cellprocessor.ift.CellProcessor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service("censusVillageImportService")
public class PanchayatImportServiceImpl extends BaseLocationImportService<Panchayat>
        implements PanchayatImportService {

    public static final String PANCHAYAT_CODE = "VCode";
    public static final String REGIONAL_NAME = "Name_G";
    public static final String NAME = "Name_E";
    public static final String BLOCK_CODE = "TCode";
    public static final String DISTRICT_CODE = "DCode";
    public static final String STATE_ID = "StateID";

    public static final String PANCHAYAT_CODE_FIELD = "vcode";
    public static final String REGIONAL_NAME_FIELD = "regionalName";
    public static final String NAME_FIELD = "name";
    public static final String BLOCK_FIELD = "block";

    private PanchayatService panchayatService;
    private DistrictService districtService;
    private StateDataService stateDataService;
    private BlockService blockService;

    @Autowired
    public PanchayatImportServiceImpl(PanchayatService panchayatService,
                                      BlockService blockService,
                                      DistrictService districtService,
                                      StateDataService stateDataService) {
        super(Panchayat.class);
        this.panchayatService = panchayatService;
        this.blockService = blockService;
        this.districtService = districtService;
        this.stateDataService = stateDataService;
    }

    @Override
    protected void createOrUpdateInstance(Panchayat instance) {
        Panchayat panchayat = panchayatService.findByBlockAndVcodeAndSvid(instance.getBlock(), instance.getVcode(),
                                                                      instance.getSvid());

        if (panchayat != null) {
            panchayat.setName(instance.getName());
            panchayat.setRegionalName(instance.getRegionalName());

            panchayatService.update(panchayat);
        } else {
            panchayatService.create(instance);
        }
    }

    @Override
    protected Map<String, CellProcessor> getProcessorMapping() {
        Map<String, CellProcessor> mapping = new LinkedHashMap<>();
        final Store store = new Store();

        mapping.put(PANCHAYAT_CODE, new GetLong());
        mapping.put(REGIONAL_NAME, new GetString());
        mapping.put(NAME, new GetString());
        mapping.put(STATE_ID, store.store(STATE, mapState(stateDataService)));
        mapping.put(DISTRICT_CODE, store.store(DISTRICT, mapDistrict(store, districtService)));
        mapping.put(BLOCK_CODE, mapBlock(store, blockService));
        return mapping;
    }

    @Override
    protected Map<String, String> getFieldNameMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(PANCHAYAT_CODE, PANCHAYAT_CODE_FIELD);
        mapping.put(REGIONAL_NAME, REGIONAL_NAME_FIELD);
        mapping.put(NAME, NAME_FIELD);
        mapping.put(BLOCK_CODE, BLOCK_FIELD);
        mapping.put(STATE_ID, null);
        mapping.put(DISTRICT_CODE, null);
        return mapping;
    }
}
