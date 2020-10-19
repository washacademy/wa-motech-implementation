package org.motechproject.wa.api.web.contract;

/**
 * Created by beehyvsc on 17/7/17.
 */
public class AddSwcRequest {
    private String name;
    private String swcId;
    private Long msisdn;
    private Long stateId;
    private String stateName;
    private Long districtId;
    private String districtName;
    private Long blockId;
    private Long panchayatId;
    private String blockName;
    private String panchayatName;
    private Long age;
    private  String sex;
    private int courseId;

    @Override
    public String toString() {
        return "AddSwcRequest{" +
                "name='" + name + '\'' +
                ", swcId='" + swcId + '\'' +
                ", msisdn=" + msisdn +
                ", stateId=" + stateId +
                ", stateName='" + stateName + '\'' +
                ", districtId=" + districtId +
                ", districtName='" + districtName + '\'' +
                ", blockId=" + blockId +
                ", panchayatId=" + panchayatId +
                ", blockName='" + blockName + '\'' +
                ", panchayatName='" + panchayatName + '\'' +
                ", age=" + age +
                ", sex='" + sex + '\'' +
                ", type='" + type + '\'' +
                ", jobStatus='" + jobStatus + '\'' +
                ", courseId='" + courseId + '\'' +
                '}';
    }

    private  String type;
    private  String jobStatus;
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

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getPanchayatName() {
        return panchayatName;
    }

    public void setPanchayatName(String panchayatName) {
        this.panchayatName = panchayatName;
    }

    public Long getAge() {
        return age;
    }

    public void setAge(Long age) {
        this.age = age;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public int getCourseId() {    return courseId;    }

    public void setCourseId(int courseId) {    this.courseId = courseId;   }
}
