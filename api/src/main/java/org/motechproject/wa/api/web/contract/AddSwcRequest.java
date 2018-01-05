package org.motechproject.wa.api.web.contract;

/**
 * Created by beehyvsc on 17/7/17.
 */
public class AddSwcRequest {
    private String name;
    private String swcId;
    private Long msisdn;
    private Long stateId;
    private Long districtId;
    private Long blockId;
    private Long panchayatId;
    private String gfStatus;

    public AddSwcRequest() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSwcId() {
        return swcId;
    }

    public void setSwcId(String swcId) {
        this.swcId = swcId;
    }

    public Long getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(Long msisdn) {
        this.msisdn = msisdn;
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

    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
        this.blockId = blockId;
    }

    public Long getPanchayatId() {
        return panchayatId;
    }

    public void setPanchayatId(Long panchayatId) {
        this.panchayatId = panchayatId;
    }

    public String getGfStatus() {
        return gfStatus;
    }

    public void setGfStatus(String gfStatus) {
        this.gfStatus = gfStatus;
    }
}
