package org.motechproject.nms.swcUpdate.contract;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

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
    private String talukaId;
    private String talukaName;
    private Long healthBlockId;
    private String healthBlockName;
    private Long phcId;
    private String phcName;
    private Long subCentreId;
    private String subCentreName;
    private Long villageId;
    private String villageName;
    private Long gfId;
    private String mobileNo;
    private String gfName;
    private String gfType;
    private String execDate;
    private String gfStatus;

    @XmlElement(name = "Taluka_Name")
    public void setTalukaName(String talukaName) {
        this.talukaName = talukaName;
    }

    public Long getHealthBlockId() {
        return healthBlockId;
    }

    @XmlElement(name = "State_Name")
    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    @XmlElement(name = "HealthBlock_ID")
    public void setHealthBlockId(Long healthBlockId) {
        this.healthBlockId = healthBlockId;
    }

    public String getHealthBlockName() {
        return healthBlockName;
    }

    public String getDistrictName() {
        return districtName;
    }

    @XmlElement(name = "District_Name")
    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getTalukaId() {
        return talukaId;
    }

    @XmlElement(name = "Taluka_ID")
    public void setTalukaId(String talukaId) {
        this.talukaId = talukaId;
    }

    public String getTalukaName() {
        return talukaName;
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

    public String getVillageName() {
        return villageName;
    }

    @XmlElement(name = "Village_Name")
    public void setVillageName(String villageName) {
        this.villageName = villageName;
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

    @XmlElement(name = "HealthBlock_Name")
    public void setHealthBlockName(String healthBlockName) {
        this.healthBlockName = healthBlockName;
    }

    public Long getPhcId() {
        return phcId;
    }
    @XmlElement(name = "Mobile_no")
    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getGfName() {
        return gfName;
    }

    @XmlElement(name = "SubCentre_ID")
    public void setSubCentreId(Long subCentreId) {
        this.subCentreId = subCentreId;
    }

    public String getSubCentreName() {
        return subCentreName;
    }

    @XmlElement(name = "SubCentre_Name")
    public void setSubCentreName(String subCentreName) {
        this.subCentreName = subCentreName;
    }

    public Long getVillageId() {
        return villageId;
    }

    @XmlElement(name = "PHC_ID")
    public void setPhcId(Long phcId) {
        this.phcId = phcId;
    }

    public String getPhcName() {
        return phcName;
    }

    @XmlElement(name = "PHC_Name")
    public void setPhcName(String phcName) {
        this.phcName = phcName;
    }

    public Long getSubCentreId() {
        return subCentreId;
    }

    @XmlElement(name = "Village_ID")
    public void setVillageId(Long villageId) {
        this.villageId = villageId;
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
}