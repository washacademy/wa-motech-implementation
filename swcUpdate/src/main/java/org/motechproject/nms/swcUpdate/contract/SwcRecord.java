package org.motechproject.nms.swcUpdate.contract;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.motechproject.nms.swc.utils.SwcConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.Map;

@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class SwcRecord {

    private Long stateId;
    private String stateName;
    private Long districtId;
    private String districtName;
    private Long blockId;
    private String blockName;
    private Long panchayatId;
    private String panchayatName;
    private Long gfId;
    private String mobileNo;
    private String gfName;
    private String gfType;
    private String execDate;
    private String gfStatus;

    @XmlElement(name = "Block_Name")
    public void setBlockName(String talukaName) {
        this.blockName = talukaName;
    }

    @XmlElement(name = "State_Name")
    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getDistrictName() {
        return districtName;
    }

    @XmlElement(name = "District_Name")
    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public Long getBlockId() {
        return blockId;
    }

    @XmlElement(name = "Block_ID")
    public void setBlockId(Long talukaId) {
        this.blockId = talukaId;
    }

    public String getBlockName() {
        return blockName;
    }


    public Long getStateId() {
        return stateId;
    }

    @XmlElement(name = "StateID")
    public void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    public Long getDistrictId() {
        return districtId;
    }

    @XmlElement(name = "District_ID")
    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public Long getGfId() {
        return gfId;
    }

    @XmlElement(name = "GF_ID")
    public void setGfId(Long gfId) {
        this.gfId = gfId;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    @XmlElement(name = "Mobile_no")
    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getGfName() {
        return gfName;
    }

    @XmlElement(name = "Panchayat_ID")
    public void setPanchayatId(Long subCentreId) {
        this.panchayatId = subCentreId;
    }

    public String getPanchayatName() {
        return panchayatName;
    }

    @XmlElement(name = "Panchayat_Name")
    public void setPanchayatName(String subCentreName) {
        this.panchayatName = subCentreName;
    }

    public Long getPanchayatId() {
        return panchayatId;
    }

    @XmlElement(name = "GF_Name")
    public void setGfName(String gfName) {
        this.gfName = gfName;
    }

    public String getGfType() {
        return gfType;
    }

    @XmlElement(name = "GF_type")
    public void setGfType(String gfType) {
        this.gfType = gfType;
    }

    public String getExecDate() {
        return execDate;
    }

    @XmlElement(name = "Exec_Date")
    public void setExecDate(String execDate) {
        this.execDate = execDate;
    }

    public String getGfStatus() {
        return gfStatus;
    }

    @XmlElement(name = "GF_Status")
    public void setGfStatus(String gfStatus) {
        this.gfStatus = gfStatus;
    }

    public Map<String, Object> toSwcRecordMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(SwcConstants.STATE_ID, getStateId());
        map.put(SwcConstants.DISTRICT_ID, getDistrictId());
        map.put(SwcConstants.DISTRICT_NAME, getDistrictName());
        map.put(SwcConstants.BLOCK_ID, getBlockId());
        map.put(SwcConstants.BLOCK_NAME, getBlockName());
        map.put(SwcConstants.PANCHAYAT_ID, getPanchayatId());
        map.put(SwcConstants.PANCHAYAT_NAME, getPanchayatName());
        map.put(SwcConstants.GF_ID, getGfId() == null ? null : getGfId().toString());
        map.put(SwcConstants.MOBILE_NO, getMobileNo() == null ? null : Long.parseLong(getMobileNo()));
        map.put(SwcConstants.GF_NAME, getGfName());
        map.put(SwcConstants.GF_TYPE, getGfType());
        map.put(SwcConstants.EXEC_DATE, "".equals(getExecDate()) ? null : LocalDate.parse(getExecDate(), DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")));
        map.put(SwcConstants.GF_STATUS, getGfStatus());
        return map;
    }
}