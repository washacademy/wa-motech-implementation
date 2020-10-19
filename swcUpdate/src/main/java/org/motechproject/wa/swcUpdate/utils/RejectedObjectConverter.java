package org.motechproject.wa.swcUpdate.utils;

import org.motechproject.wa.rejectionhandler.domain.SwcImportRejection;
import org.motechproject.wa.swc.domain.SwcJobStatus;
import org.motechproject.wa.swcUpdate.contract.SwcRecord;


public final class RejectedObjectConverter {

    private RejectedObjectConverter() {
    }

    public static SwcImportRejection swcRejection(SwcRecord record, Boolean accepted, String rejectionReason, String action) {
        SwcImportRejection swcImportRejection = new SwcImportRejection();
        swcImportRejection.setStateId(record.getStateId());
        swcImportRejection.setStateName(record.getStateName());
        swcImportRejection.setDistrictId(record.getDistrictId());
        swcImportRejection.setDistrictName(record.getDistrictName());
        swcImportRejection.setMsisdn(record.getMobileNo());
        swcImportRejection.setSource("RCH-Import");
        swcImportRejection.setAccepted(accepted);
        swcImportRejection.setBlockId(record.getBlockId());
        swcImportRejection.setBlockName(record.getBlockName());
        swcImportRejection.setPanchayatId(record.getPanchayatId());
        swcImportRejection.setPanchayatName(record.getPanchayatName());
        swcImportRejection.setSwcID(record.getGfId());
        swcImportRejection.setRejectionReason(rejectionReason);
        swcImportRejection.setAction(action);
        swcImportRejection.setSwcName(record.getGfName());
        swcImportRejection.setSwcStatus(SwcJobStatus.ACTIVE.toString());
        swcImportRejection.setSex(record.getGfSex());
        swcImportRejection.setUpdateDateNic(record.getExecDate());
        swcImportRejection.setCourseId(record.getCourseId());

        return swcImportRejection;
    }
}