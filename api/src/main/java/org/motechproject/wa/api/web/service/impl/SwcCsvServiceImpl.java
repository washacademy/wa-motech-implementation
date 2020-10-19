package org.motechproject.wa.api.web.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.motechproject.wa.api.web.contract.AddSwcRequest;
import org.motechproject.wa.api.web.service.SwcCsvService;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.repository.BlockDataService;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.region.repository.PanchayatDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.rejectionhandler.domain.SwcImportRejection;
import org.motechproject.wa.rejectionhandler.service.SwcRejectionService;
import org.motechproject.wa.swc.domain.RejectionReasons;
import org.motechproject.wa.swc.domain.SubscriptionOrigin;
import org.motechproject.wa.swc.domain.SwcJobStatus;
import org.motechproject.wa.swc.utils.SwcConstants;
import org.motechproject.wa.swcUpdate.service.SwcImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vishnu on 25/9/17.
 */
@Service("swcCsvService")
public class SwcCsvServiceImpl implements SwcCsvService {

    private static final String NOT_PRESENT = "<%s: Not Present>";
    private static final String INVALID = "<%s: Invalid>";
    private static final String IVR_INTERACTION_LOG = "IVR INTERACTION: %s";

    private static final long SMALLEST_10_DIGIT_NUMBER = 1000000000L;
    private static final long LARGEST_10_DIGIT_NUMBER  = 9999999999L;
    private final String contactNumber = "contactNumber";
    private final String gfStatus = "gfStatus";

    private DistrictDataService districtDataService;
    private BlockDataService blockDataService;
    private StateDataService stateDataService;
    private PanchayatDataService panchayatDataService;

    private HashMap<String,State> stateHashMap = new HashMap<>();
    private HashMap<String,District> districtHashMap = new HashMap();
    private HashMap<String,Block> blockHashMap = new HashMap();
    private HashMap<String,Panchayat> panchayatHashMap = new HashMap();


    private static final Logger LOGGER = LoggerFactory.getLogger(SwcCsvServiceImpl.class);

    @Autowired
    private SwcRejectionService swcRejectionService;

    @Autowired
    private SwcImportService swcImportService;

    @Override
    @Transactional
    public StringBuilder csvUploadRch(AddSwcRequest addSwcRequest) {
        log("REQUEST: /ops/createUpdateRchSwc", String.format(
                "callingNumber=%s, rchId=%s, name=%s, state=%d, district=%d",
                LogHelper.obscure(addSwcRequest.getMsisdn()),
                addSwcRequest.getSwcId(),
                addSwcRequest.getName(),
                addSwcRequest.getStateId(),
                addSwcRequest.getDistrictId()));

        StringBuilder failureReasons = new StringBuilder();
        validateField10Digits(failureReasons, contactNumber, addSwcRequest.getMsisdn());
        validateFieldPositiveLong(failureReasons, contactNumber, addSwcRequest.getMsisdn());
        validateFieldPresent(failureReasons, "swcId", addSwcRequest.getSwcId());
        validateFieldPresent(failureReasons, "stateId", addSwcRequest.getStateId());
        validateFieldPresent(failureReasons, "districtId", addSwcRequest.getDistrictId());
        validateFieldString(failureReasons, "name", addSwcRequest.getName());
        validateFieldPresent(failureReasons, "blockId", addSwcRequest.getBlockId());
        validateFieldPresent(failureReasons, "panchayatId", addSwcRequest.getPanchayatId());
        validateNameFields(failureReasons, "blockName", addSwcRequest.getBlockName(), addSwcRequest.getBlockId());
        validateNameFields(failureReasons, "panchayatName", addSwcRequest.getPanchayatName(), addSwcRequest.getPanchayatId());
        LOGGER.info(failureReasons.toString());
        if (failureReasons.length() > 0) {
            String fieldName = failureReasons.toString().split("[\\W]")[1];
            csvRejectionsRch(fieldName, addSwcRequest);
            LOGGER.info(failureReasons.toString());
            return failureReasons;
        }
        return null;
    }

    @Override
    @Transactional
    public void persistSwcRch(AddSwcRequest addSwcRequest) {
        Map<String, Object> swcProperties = new HashMap<>();
        swcProperties.put(SwcConstants.NAME, addSwcRequest.getName());
        swcProperties.put(SwcConstants.ID, addSwcRequest.getSwcId());
        swcProperties.put(SwcConstants.MOBILE_NO, addSwcRequest.getMsisdn());
        swcProperties.put(SwcConstants.STATE_ID, addSwcRequest.getStateId());
        swcProperties.put(SwcConstants.DISTRICT_ID, addSwcRequest.getDistrictId());
        swcProperties.put(SwcConstants.JOB_STATUS, SwcJobStatus.ACTIVE.toString());
        swcProperties.put(SwcConstants.BLOCK_ID, addSwcRequest.getBlockId());
        swcProperties.put(SwcConstants.PANCHAYAT_ID, addSwcRequest.getPanchayatId());
        swcProperties.put(SwcConstants.BLOCK_NAME, addSwcRequest.getBlockName());
        swcProperties.put(SwcConstants.PANCHAYAT_NAME, addSwcRequest.getPanchayatName());
        swcProperties.put(SwcConstants.SWC_SEX, addSwcRequest.getSex());
        swcProperties.put(SwcConstants.SWC_AGE, addSwcRequest.getAge());
        swcProperties.put(SwcConstants.TYPE, addSwcRequest.getType());
        swcProperties.put(SwcConstants.COURSE_ID,addSwcRequest.getCourseId());
        swcImportService.createUpdate(swcProperties, SubscriptionOrigin.RCH_IMPORT);
    }

    @Override
    @Transactional
    public void persistCsvSwcRch(AddSwcRequest addSwcRequest) {
        Map<String, Object> swcProperties = new HashMap<>();
        swcProperties.put(SwcConstants.NAME, addSwcRequest.getName());
        swcProperties.put(SwcConstants.ID, addSwcRequest.getSwcId());
        swcProperties.put(SwcConstants.MOBILE_NO, addSwcRequest.getMsisdn());
        swcProperties.put(SwcConstants.DISTRICT_NAME, addSwcRequest.getDistrictName());
        swcProperties.put(SwcConstants.STATE_NAME, addSwcRequest.getStateName());
        swcProperties.put(SwcConstants.STATE_ID, addSwcRequest.getStateId());
        swcProperties.put(SwcConstants.DISTRICT_ID, addSwcRequest.getDistrictId());
        swcProperties.put(SwcConstants.JOB_STATUS, SwcJobStatus.ACTIVE.toString());
        swcProperties.put(SwcConstants.BLOCK_ID, addSwcRequest.getBlockId());
        swcProperties.put(SwcConstants.PANCHAYAT_ID, addSwcRequest.getPanchayatId());
        swcProperties.put(SwcConstants.BLOCK_NAME, addSwcRequest.getBlockName());
        swcProperties.put(SwcConstants.PANCHAYAT_NAME, addSwcRequest.getPanchayatName());
        swcProperties.put(SwcConstants.SWC_SEX, addSwcRequest.getSex());
        swcProperties.put(SwcConstants.SWC_AGE, addSwcRequest.getAge());
        swcProperties.put(SwcConstants.TYPE, addSwcRequest.getType());
        swcProperties.put(SwcConstants.COURSE_ID,addSwcRequest.getCourseId());

        swcImportService.createCsvUpdate(swcProperties, SubscriptionOrigin.RCH_IMPORT, this.stateHashMap, this.districtHashMap,
                this.blockHashMap, this.panchayatHashMap);
    }



    @Override
    @Transactional
    public void csvRejectionsRch(String fieldName, AddSwcRequest addSwcRequest) {
        String action = this.rchSwcActionFinder(addSwcRequest);
        if ("contactNumber".equals(fieldName)) {
            swcRejectionService.createUpdate(swcRejectionRch(addSwcRequest, false, RejectionReasons.MOBILE_NUMBER_EMPTY_OR_WRONG_FORMAT.toString(), action));
        } else if (gfStatus.equals(fieldName)) {
            swcRejectionService.createUpdate(swcRejectionRch(addSwcRequest, false, RejectionReasons.GF_STATUS_EMPTY_OR_WRONG_FORMAT.toString(), action));
        } else {
            swcRejectionService.createUpdate(swcRejectionRch(addSwcRequest, false, RejectionReasons.FIELD_NOT_PRESENT.toString(), action));
        }
    }


    private String rchSwcActionFinder(AddSwcRequest record) {
        Long swcId = record.getSwcId() == null ? null : Long.parseLong(record.getSwcId());
        if (swcRejectionService.findBySwcIdAndPanchayatIdAndCourseId(swcId, record.getPanchayatId(),record.getCourseId()) == null) {
            return "CREATE";
        } else {
            return "UPDATE";
        }
    }

    public static SwcImportRejection swcRejectionRch(AddSwcRequest record, Boolean accepted, String rejectionReason, String action) {
        SwcImportRejection swcImportRejection = new SwcImportRejection();
        swcImportRejection.setSwcName(record.getName());
        swcImportRejection.setSwcID(Long.parseLong(record.getSwcId()));
        swcImportRejection.setMsisdn(record.getMsisdn().toString());
        swcImportRejection.setStateId(record.getStateId());
        swcImportRejection.setDistrictId(record.getDistrictId());
        swcImportRejection.setBlockId(record.getBlockId());
        swcImportRejection.setPanchayatId(record.getPanchayatId());
        swcImportRejection.setBlockName(record.getBlockName());
        swcImportRejection.setPanchayatName(record.getPanchayatName());
        swcImportRejection.setSwcStatus(SwcJobStatus.ACTIVE.toString());
        swcImportRejection.setSource("RCH-Import");
        swcImportRejection.setAccepted(accepted);
        swcImportRejection.setRejectionReason(rejectionReason);
        swcImportRejection.setAction(action);
        swcImportRejection.setSex(record.getSex());
        swcImportRejection.setCourseId(record.getCourseId());
        return swcImportRejection;
    }

    private static boolean validateField10Digits(StringBuilder errors, String fieldName, Long value) {
        if (!validateFieldPresent(errors, fieldName, value)) {
            return false;
        }
        if (value >= SMALLEST_10_DIGIT_NUMBER && value <= LARGEST_10_DIGIT_NUMBER) {
            return true;
        }
        errors.append(String.format(INVALID, fieldName));
        return false;
    }

    private static boolean validateFieldPositiveLong(StringBuilder errors, String fieldName, Long value) {
        if (!validateFieldPresent(errors, fieldName, value)) {
            return false;
        }
        if (value >= 0) {
            return true;
        }
        errors.append(String.format(INVALID, fieldName));
        return false;
    }

    private static boolean validateFieldPresent(StringBuilder errors, String fieldName, Object value) {
        if (value != null) {
            return true;
        }
        errors.append(String.format(NOT_PRESENT, fieldName));
        return false;
    }

    private static boolean validateFieldString(StringBuilder errors, String fieldName, String value) {
        if (!validateFieldPresent(errors, fieldName, value)) {
            return false;
        }
        if (value.length() > 0) {
            return true;
        }
        errors.append(String.format(INVALID, fieldName));
        return false;
    }

    private static boolean validateNameFields(StringBuilder errors, String fieldName, Object value1, Object value2) {
        if (value2 != null && !"NULL".equalsIgnoreCase(value2.toString()) && !value2.toString().isEmpty() && !"0".equals(value2.toString())) {
            if (value1 != null && !"NULL".equalsIgnoreCase(value1.toString()) && !value1.toString().isEmpty()) {
                return true;
            }
        } else {
            return true;
        }
        errors.append(String.format(NOT_PRESENT, fieldName));
        return false;
    }

    protected static void log(final String endpoint, final String s) {
        LOGGER.info(IVR_INTERACTION_LOG.format(endpoint) + (StringUtils.isBlank(s) ? "" : " : " + s));
    }

    @Transactional
    public void createLocation() {
        createLocations(this.stateHashMap,this.districtHashMap,this.blockHashMap,this.panchayatHashMap);
//        LOGGER.info("stateMap:"+this.stateHashMap.toString());
//        LOGGER.info("districtMap:"+ this.districtHashMap.toString());
//        LOGGER.info("blockMap:"+ this.blockHashMap.toString());
//        LOGGER.info("panchayatMap:"+ this.panchayatHashMap.toString());
    }

    public void createLocations(HashMap<String, State> sMap, HashMap<String, District> dMap,
                                HashMap<String, Block> bMap, HashMap<String, Panchayat> pMap) {
        List<State> stateList = stateDataService.retrieveAll();
        for(State s: stateList) {
            sMap.put(s.getCode().toString(),s);
        }
        List<District> districtList = districtDataService.retrieveAll();
        for(District d: districtList) {
            dMap.put(d.getState().getCode().toString()+d.getCode().toString(),d);
        }
        List<Block> blockList = blockDataService.retrieveAll();
        for(Block b: blockList) {
            bMap.put(b.getDistrict().getState().getCode().toString()+b.getDistrict().getCode().toString()+
                    b.getCode().toString(),b);
        }
        List<Panchayat> panchayatList = panchayatDataService.retrieveAll();
        for(Panchayat p: panchayatList) {
            pMap.put(p.getBlock().getDistrict().getState().getCode().toString()+
                    p.getBlock().getDistrict().getCode().toString()+p.getBlock().getCode().toString()+
                    Long.toString(p.getVcode()),p);
        }
    }

    @Autowired
    public void setDistrictDataService(DistrictDataService districtDataService) {
        this.districtDataService = districtDataService;
    }

    @Autowired
    public void setBlockDataService(BlockDataService blockDataService) {
        this.blockDataService = blockDataService;
    }

    @Autowired
    public void setStateDataService(StateDataService stateDataService) {
        this.stateDataService = stateDataService;
    }

    @Autowired
    public void setPanchayatDataService(PanchayatDataService panchayatDataService) {
        this.panchayatDataService = panchayatDataService;
    }
}
