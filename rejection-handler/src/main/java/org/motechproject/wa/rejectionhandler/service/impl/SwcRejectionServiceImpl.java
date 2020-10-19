package org.motechproject.wa.rejectionhandler.service.impl;

import org.motechproject.wa.rejectionhandler.domain.SwcImportRejection;
import org.motechproject.wa.rejectionhandler.repository.SwcImportRejectionDataService;
import org.motechproject.wa.rejectionhandler.service.SwcRejectionService;
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
    public SwcImportRejection findBySwcIdAndPanchayatIdAndCourseId(Long swcId, Long stateId,Integer courseId) {
        return swcImportRejectionDataService.findBySwcIdAndPanchayatIdAndCourseId(swcId, stateId,courseId);
    }

    private SwcImportRejection findBySwcIdAndCourseId(Long swcId,Integer courseId) {
        return swcImportRejectionDataService.findBySwcIdAndCourseId(swcId,courseId);
    }

    @Override //NO CHECKSTYLE CyclomaticComplexity
    public void createUpdate(SwcImportRejection swcImportRejection) {
        LOGGER.info("Creating rejection record: {} ", swcImportRejection);
        if (swcImportRejection.getSwcID() != null && swcImportRejection.getPanchayatId() != null) {
            SwcImportRejection swcImportRejection1 = findBySwcIdAndPanchayatIdAndCourseId(swcImportRejection.getSwcID(), swcImportRejection.getPanchayatId(),swcImportRejection.getCourseId());

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
        } else if (swcImportRejection.getSwcID() != null && swcImportRejection.getPanchayatId() == null) {
            SwcImportRejection swcImportRejection1 = findBySwcIdAndCourseId(swcImportRejection.getSwcID(),swcImportRejection.getCourseId());

            if (swcImportRejection1 == null && !swcImportRejection.getAccepted()) {
                swcImportRejectionDataService.create(swcImportRejection);
            } else {
                swcImportRejection1 = setNewData(swcImportRejection, swcImportRejection1);
                swcImportRejectionDataService.update(swcImportRejection1);
            }
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
        swcImportRejection1.setCourseId(swcImportRejection.getCourseId());
        return swcImportRejection1;
    }
}
