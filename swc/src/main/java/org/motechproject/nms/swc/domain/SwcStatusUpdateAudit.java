package org.motechproject.nms.swc.domain;

import org.joda.time.DateTime;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

/**
 * Audit record for update of anonymous flw to active
 */
@Entity(tableName = "wash_swachgrahi_status_update_audit")
public class SwcStatusUpdateAudit {

    @Field
    private DateTime importDate;

    @Field
    private String  swcId;

    @Field
    private Long contactNumber;

    @Field
    private UpdateStatusType updateStatusType;


    public SwcStatusUpdateAudit(DateTime importDate, String swcId, Long contactNumber, UpdateStatusType updateStatusType) {
        this.importDate = importDate;
        this.swcId = swcId;
        this.contactNumber = contactNumber;
        this.updateStatusType = updateStatusType;
    }

    public UpdateStatusType getUpdateStatusType() {
        return updateStatusType;
    }

    public void setUpdateStatusType(UpdateStatusType updateStatusType) {
        this.updateStatusType = updateStatusType;
    }

    public DateTime getImportDate() {
        return importDate;
    }

    public void setImportDate(DateTime importDate) {
        this.importDate = importDate;
    }

    public String getSwcId() {
        return swcId;
    }

    public void setSwcId(String swcId) {
        this.swcId = swcId;
    }

    public Long getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(Long contactNumber) {
        this.contactNumber = contactNumber;
    }
}
