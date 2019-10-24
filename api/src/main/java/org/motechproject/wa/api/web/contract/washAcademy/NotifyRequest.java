package org.motechproject.wa.api.web.contract.washAcademy;

public class NotifyRequest {
    private Long swcId;
    private Integer courseId;

    public NotifyRequest(){

    }

    public Long getSwcId() {
        return swcId;
    }

    public void setSwcId(Long swcId) {
        this.swcId = swcId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }
}
