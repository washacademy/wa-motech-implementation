package org.motechproject.wa.swc.domain;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.annotations.InstanceLifecycleListeners;
import org.motechproject.mds.domain.MdsEntity;
import org.motechproject.wa.region.domain.*;
import org.motechproject.wa.region.domain.validation.ValidFullLocation;
import org.motechproject.wa.swc.domain.validation.ValidSwachchagrahi;
import org.motechproject.wa.tracking.annotation.TrackClass;
import org.motechproject.wa.tracking.annotation.TrackFields;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Persistent;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@ValidFullLocation
@ValidSwachchagrahi
@Entity(tableName = "wash_swachchagrahi")
@TrackClass
@TrackFields
@InstanceLifecycleListeners
public class Swachchagrahi extends MdsEntity implements FullLocation {

    @Field
    private String swcId;

    @Field
    @Min(value = 1000000000L, message = "contactNumber must be 10 digits")
    @Max(value = 9999999999L, message = "contactNumber must be 10 digits")
    @Column(length = 10)
    private Long contactNumber;

    @Field
    private String name;

    @Field
    private SwachchagrahiStatus courseStatus;

    @Field
    private String sex;

    @Field
    private Integer age;

    @Field
    private String qualification;

    @Field
    private String designation;

    @Field
    private Boolean trainedInCommunityService;

    @Field
    private Boolean workingSBMdedicatedresource;

    @Field
    private Boolean amountPaid;

    @Field
    private Boolean paymentLinkedToTarget;

    @Field
    private String activities;

    @Field
    private Integer experience;

    @Field
    private DateTime invalidationDate;

    @Field
    private LocalDate updatedDateNic;

    @Field
    private Language language;

    @Field
    @Persistent(defaultFetchGroup = "true")
    private State state;

    @Field
    @Persistent(defaultFetchGroup = "true")
    private District district;

    @Field
    @Persistent(defaultFetchGroup = "true")
    private Block block;

    @Field
    @Persistent(defaultFetchGroup = "true")
    private Panchayat panchayat;

    @Field
    @Persistent(defaultFetchGroup = "true")
    private Circle circle;

    @Field
    @Persistent(defaultFetchGroup = "true")
    private SwcJobStatus jobStatus;

    @Field
    private int courseId;

    public int getCourseId() { return courseId;  }

    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public Boolean getTrainedInCommunityService() {
        return trainedInCommunityService;
    }

    public void setTrainedInCommunityService(Boolean trainedInCommunityService) {
        this.trainedInCommunityService = trainedInCommunityService;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Boolean getWorkingSBMdedicatedresource() {
        return workingSBMdedicatedresource;
    }

    public void setWorkingSBMdedicatedresource(Boolean workingSBMdedicatedresource) {
        this.workingSBMdedicatedresource = workingSBMdedicatedresource;
    }

    public Boolean getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Boolean amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Boolean getPaymentLinkedToTarget() {
        return paymentLinkedToTarget;
    }

    public void setPaymentLinkedToTarget(Boolean paymentLinkedToTarget) {
        this.paymentLinkedToTarget = paymentLinkedToTarget;
    }

    public String getActivities() {
        return activities;
    }

    public void setActivities(String activities) {
        this.activities = activities;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public Block getBlock() {
        return block;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    @Override
    public void setBlock(Block block) {
        this.block = block;
    }

    @Override
    public Panchayat getPanchayat() {
        return panchayat;
    }

    @Override
    public void setPanchayat(Panchayat panchayat) {
        this.panchayat = panchayat;
    }

    public Swachchagrahi(Long contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Swachchagrahi(Long contactNumber, Circle circle){
        this.contactNumber = contactNumber;
        this.circle = circle;
    }

    public Swachchagrahi(Long contactNumber, Circle circle,int courseId){
        this.contactNumber = contactNumber;
        this.circle = circle;
        this.courseId = courseId;
    }

    public Swachchagrahi(String name, Long contactNumber) {
        this.name = name;
        this.contactNumber = contactNumber;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SwachchagrahiStatus getCourseStatus() {
        return courseStatus;
    }

    public void setCourseStatus(SwachchagrahiStatus courseStatus) {
        this.courseStatus = courseStatus;

        if (this.courseStatus == SwachchagrahiStatus.INVALID) {
            setInvalidationDate(new DateTime());
            setContactNumber(null);
        } else {
            setInvalidationDate(null);
        }
    }

    public DateTime getInvalidationDate() {
        return invalidationDate;
    }

    public void setInvalidationDate(DateTime invalidationDate) {
        this.invalidationDate = invalidationDate;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public District getDistrict() {
        return district;
    }

    @Override
    public void setDistrict(District district) {
        this.district = district;
    }

    public LocalDate getUpdatedDateNic() {
        return updatedDateNic;
    }

    public void setUpdatedDateNic(LocalDate updatedDateNic) {
        this.updatedDateNic = updatedDateNic;
    }

    public SwcJobStatus getJobStatus() { return jobStatus; }

    public void setJobStatus(SwcJobStatus jobStatus) { this.jobStatus = jobStatus; }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    @Override //NO CHECKSTYLE CyclomaticComplexity
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Swachchagrahi that = (Swachchagrahi) o;

        if (!this.getId().equals(that.getId())) {
            return false;
        }
        if (contactNumber != null ? !contactNumber.equals(that.contactNumber) : that.contactNumber != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return !(district != null ? !district.equals(that.district) : that.district != null);

    }

    @Override
    public int hashCode() {
        int result = (getId() != null ? getId().hashCode() : 0);
        result = 31 * result + (contactNumber != null ? contactNumber.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Swachchagrahi{" +
                "id=" + getId() +
                ", contactNumber=" + contactNumber +
                ", name=" + name +
                '}';
    }
}
