package org.motechproject.nms.api.web.domain;

import org.joda.time.DateTime;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

/**
 * Audit record for Inactive Job Flw call details
 */

@Entity(tableName = "nms_inactive_job_call_audit")
public class InactiveJobCallAudit {

    @Field
    private DateTime dateTimeNow;

    @Field
    private Long swcId;

    @Field
    private Long callingNumber;

    public InactiveJobCallAudit(DateTime dateTimeNow, Long swcId, Long callingNumber) {
        this.dateTimeNow = dateTimeNow;
        this.swcId = swcId;
        this.callingNumber = callingNumber;
    }

    public DateTime getDateTimeNow() {
        return dateTimeNow;
    }

    public void setDateTimeNow(DateTime dateTimeNow) {
        this.dateTimeNow = dateTimeNow;
    }

    public Long getSwcId() {
        return swcId;
    }

    public void setSwcId(Long swcId) {
        this.swcId = swcId;
    }

    public Long getCallingNumber() {
        return callingNumber;
    }

    public void setCallingNumber(Long callingNumber) {
        this.callingNumber = callingNumber;
    }
}
