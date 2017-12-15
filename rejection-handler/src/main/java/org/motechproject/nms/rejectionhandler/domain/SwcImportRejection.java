package org.motechproject.nms.rejectionhandler.domain;

import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

@Entity(tableName = "wash_swachgrahi_rejects")
public class SwcImportRejection {

    @Field
    private Boolean accepted;

    @Field
    private String rejectionReason;

    @Field
    private String source;

    @Field
    private String action;

    @Field
    private String updateDateNic;

    @Field
    private Long swcID;

    @Field
    private String swcName;

    @Field
    private String swcStatus;

    @Field
    private String msisdn;

    @Field
    private String sex;

    @Field
    private Long stateId;

    @Field
    private String stateName;

    @Field
    private Long districtId;

    @Field
    private String districtName;

    @Field
    private Long blockId;

    @Field
    private String blockName;

    @Field
    private Long panchayatId;

    @Field
    private String panchayatName;

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUpdateDateNic() {
        return updateDateNic;
    }

    public void setUpdateDateNic(String updateDateNic) {
        this.updateDateNic = updateDateNic;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Long getStateId() {
        return stateId;
    }

    public void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public Long getSwcID() {
        return swcID;
    }

    public void setSwcID(Long swcID) {
        this.swcID = swcID;
    }

    public String getSwcName() {
        return swcName;
    }

    public void setSwcName(String swcName) {
        this.swcName = swcName;
    }

    public String getSwcStatus() {
        return swcStatus;
    }

    public void setSwcStatus(String swcStatus) {
        this.swcStatus = swcStatus;
    }

    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
        this.blockId = blockId;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public Long getPanchayatId() {
        return panchayatId;
    }

    public void setPanchayatId(Long panchayatId) {
        this.panchayatId = panchayatId;
    }

    public String getPanchayatName() {
        return panchayatName;
    }

    public void setPanchayatName(String panchayatName) {
        this.panchayatName = panchayatName;
    }
}

