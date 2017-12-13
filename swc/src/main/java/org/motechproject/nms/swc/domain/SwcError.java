package org.motechproject.nms.swc.domain;

import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

/**
 * Audit log object for tracking failed FLW updates
 */
@Entity(tableName = "wa_swc_errors")
public class SwcError {

    @Field
    private String swcId;

    @Field
    private Long stateId;

    @Field
    private Long districtId;

    @Field
    private SwcErrorReason reason;

    public SwcError(String swcId, Long stateId, Long districtId, SwcErrorReason reason) {
        this.swcId = swcId;
        this.stateId = stateId;
        this.districtId = districtId;
        this.reason = reason;
    }

    public String getSwcId() {
        return swcId;
    }

    public void setSwcId(String swcId) {
        this.swcId = swcId;
    }

    public Long getStateId() {
        return stateId;
    }

    public void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public SwcErrorReason getReason() {
        return reason;
    }

    public void setReason(SwcErrorReason reason) {
        this.reason = reason;
    }
}
