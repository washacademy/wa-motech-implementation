package org.motechproject.nms.rejectionhandler.domain;

import org.joda.time.DateTime;
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
    private DateTime creationDate;

    @Field
    private String createdBy;

    @Field
    private String updateDateNic;

    @Field
    private Long SwcID;

    @Field
    private String SwcName;

    @Field
    private String SwcStatus;

    @Field
    private DateTime modificationDate;

    @Field
    private String modifiedBy;

    @Field
    private String msisdn;

    @Field
    private String owner;

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
    private Long BlockId;

    @Field
    private String BlockName;

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

    public DateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdateDateNic() {
        return updateDateNic;
    }

    public void setUpdateDateNic(String updateDateNic) {
        this.updateDateNic = updateDateNic;
    }

    public Long getSwcID() {
        return SwcID;
    }

    public void setSwcID(Long swcID) {
        SwcID = swcID;
    }

    public String getSwcName() {
        return SwcName;
    }

    public void setSwcName(String swcName) {
        SwcName = swcName;
    }

    public String getSwcStatus() {
        return SwcStatus;
    }

    public void setSwcStatus(String swcStatus) {
        SwcStatus = swcStatus;
    }

    public DateTime getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(DateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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

    public Long getBlockId() {
        return BlockId;
    }

    public void setBlockId(Long blockId) {
        BlockId = blockId;
    }

    public String getBlockName() {
        return BlockName;
    }

    public void setBlockName(String blockName) {
        BlockName = blockName;
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

