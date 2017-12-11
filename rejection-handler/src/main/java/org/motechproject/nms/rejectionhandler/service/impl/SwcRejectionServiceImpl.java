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
@Service("flwRejectionService")
public class SwcRejectionServiceImpl implements SwcRejectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwcRejectionServiceImpl.class);

    @Autowired
    private SwcImportRejectionDataService flwImportRejectionDataService;

    @Override
    public SwcImportRejection findBySwcIdAndPanchayatId(Long flwId, Long stateId) {
        return flwImportRejectionDataService.findBySwcIdAndPanchayatId(flwId, stateId);
    }

    @Override //NO CHECKSTYLE CyclomaticComplexity
    public void createUpdate(SwcImportRejection flwImportRejection) {
        LOGGER.info("Creating rejection record: ");
        if (flwImportRejection.getSwcID() != null && flwImportRejection.getStateId() != null) {
            SwcImportRejection flwImportRejection1 = findBySwcIdAndPanchayatId(flwImportRejection.getSwcID(), flwImportRejection.getStateId());

            if (flwImportRejection1 == null && !flwImportRejection.getAccepted()) {
                flwImportRejectionDataService.create(flwImportRejection);
            } else if (flwImportRejection1 == null && flwImportRejection.getAccepted()) {
                LOGGER.debug(String.format("There is no rejection data for flwId %s and stateId %s", flwImportRejection.getSwcID().toString(), flwImportRejection.getStateId().toString()));
            } else if (flwImportRejection1 != null && !flwImportRejection1.getAccepted()) {
                flwImportRejection1 = setNewData(flwImportRejection, flwImportRejection1);
                flwImportRejectionDataService.update(flwImportRejection1);
            } else if (flwImportRejection1 != null && flwImportRejection1.getAccepted()) {
                flwImportRejection1 = setNewData(flwImportRejection, flwImportRejection1);
                flwImportRejectionDataService.update(flwImportRejection1);
            }
        } else if (flwImportRejection.getSwcID() != null && flwImportRejection.getStateId() == null) {
            flwImportRejectionDataService.create(flwImportRejection);
        }
        LOGGER.info("Created rejection record.");
    }

    private static SwcImportRejection setNewData(SwcImportRejection flwImportRejection, SwcImportRejection flwImportRejection1) {
        flwImportRejection1.setStateId(flwImportRejection.getStateId());
        flwImportRejection1.setDistrictId(flwImportRejection.getDistrictId());
        flwImportRejection1.setDistrictName(flwImportRejection.getDistrictName());
        flwImportRejection1.setBlockId(flwImportRejection.getBlockId());
        flwImportRejection1.setBlockName(flwImportRejection.getBlockName());
        flwImportRejection1.setSwcID(flwImportRejection.getSwcID());
        flwImportRejection1.setSwcName(flwImportRejection.getSwcName());
        flwImportRejection1.setSwcStatus(flwImportRejection.getSwcStatus());
        flwImportRejection1.setMsisdn(flwImportRejection.getMsisdn());
        flwImportRejection1.setSex(flwImportRejection.getSex());
        flwImportRejection1.setAccepted(flwImportRejection.getAccepted());
        flwImportRejection1.setRejectionReason(flwImportRejection.getRejectionReason());
        flwImportRejection1.setSource(flwImportRejection.getSource());
        flwImportRejection1.setAction(flwImportRejection.getAction());
        return flwImportRejection1;
    }
}
