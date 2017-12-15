package org.motechproject.nms.rejectionhandler.service.impl;

import org.motechproject.nms.rejectionhandler.domain.SwcImportRejection;
import org.motechproject.nms.rejectionhandler.repository.SwcImportRejectionDataService;
import org.motechproject.nms.rejectionhandler.service.SwcRejectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by vishnu on 15/7/17.
 */
@Service("swcRejectionService")
public class SwcRejectionServiceImpl implements SwcRejectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwcRejectionServiceImpl.class);

    @Autowired
    private SwcImportRejectionDataService swcImportRejectionDataService;

    @Override
    public SwcImportRejection findBySwcIdAndPanchayatId(Long swcId, Long stateId) {
        return swcImportRejectionDataService.findBySwcIdAndPanchayatId(swcId, stateId);
    }

    @Override //NO CHECKSTYLE CyclomaticComplexity
    public void createUpdate(SwcImportRejection swcImportRejection) {
        LOGGER.info("Creating rejection record: ");
        if (swcImportRejection.getSwcID() != null && swcImportRejection.getPanchayatId() != null) {
            SwcImportRejection swcImportRejection1 = findBySwcIdAndPanchayatId(swcImportRejection.getSwcID(), swcImportRejection.getPanchayatId());

            if (swcImportRejection1 == null && !swcImportRejection.getAccepted()) {
                swcImportRejectionDataService.create(swcImportRejection);
            } else if (swcImportRejection1 == null && swcImportRejection.getAccepted()) {
                LOGGER.debug(String.format("There is no rejection data for swcId %s and panchayatId %s", swcImportRejection.getSwcID().toString(), swcImportRejection.getPanchayatId().toString()));
            } else if (swcImportRejection1 != null && !swcImportRejection1.getAccepted()) {
                swcImportRejection1 = setNewData(swcImportRejection, swcImportRejection1);
                swcImportRejectionDataService.update(swcImportRejection1);
            } else if (swcImportRejection1 != null && swcImportRejection1.getAccepted()) {
                swcImportRejection1 = setNewData(swcImportRejection, swcImportRejection1);
                swcImportRejectionDataService.update(swcImportRejection1);
            }
        } else if (swcImportRejection.getSwcID() != null && swcImportRejection.getStateId() == null) {
            swcImportRejectionDataService.create(swcImportRejection);
        }
        LOGGER.info("Created rejection record.");
    }

    private static SwcImportRejection setNewData(SwcImportRejection swcImportRejection, SwcImportRejection swcImportRejection1) {
        swcImportRejection1.setStateId(swcImportRejection.getStateId());
        swcImportRejection1.setStateName(swcImportRejection.getStateName());
        swcImportRejection1.setDistrictId(swcImportRejection.getDistrictId());
        swcImportRejection1.setDistrictName(swcImportRejection.getDistrictName());
        swcImportRejection1.setBlockId(swcImportRejection.getBlockId());
        swcImportRejection1.setBlockName(swcImportRejection.getBlockName());
        swcImportRejection1.setPanchayatId(swcImportRejection.getPanchayatId());
        swcImportRejection1.setPanchayatName(swcImportRejection.getPanchayatName());
        swcImportRejection1.setSwcID(swcImportRejection.getSwcID());
        swcImportRejection1.setSwcName(swcImportRejection.getSwcName());
        swcImportRejection1.setSwcStatus(swcImportRejection.getSwcStatus());
        swcImportRejection1.setMsisdn(swcImportRejection.getMsisdn());
        swcImportRejection1.setSex(swcImportRejection.getSex());
        swcImportRejection1.setAccepted(swcImportRejection.getAccepted());
        swcImportRejection1.setRejectionReason(swcImportRejection.getRejectionReason());
        swcImportRejection1.setSource(swcImportRejection.getSource());
        swcImportRejection1.setAction(swcImportRejection.getAction());
        swcImportRejection1.setUpdateDateNic(swcImportRejection.getUpdateDateNic());
        return swcImportRejection1;
    }
}
