package org.motechproject.nms.swc.domain;

import org.joda.time.LocalDate;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

@Entity(tableName = "nms_contactNumber_audit")
public class ContactNumberAudit {

    @Field
    private LocalDate importDate;

    @Field
    private Long swcId;

    @Field
    private Long oldCallingNumber;

    @Field
    private Long newCallingNumber;

    public ContactNumberAudit(Long swcId) {
        this(null, swcId, null, null);
    }

    public ContactNumberAudit(LocalDate importDate, Long swcId, Long oldCallingNumber, Long newCallingNumber) {
        this.importDate = importDate;
        this.swcId = swcId;
        this.oldCallingNumber = oldCallingNumber;
        this.newCallingNumber = newCallingNumber;
    }

    public LocalDate getImportDate() {
        return importDate;
    }

    public void setImportDate(LocalDate importDate) {
        this.importDate = importDate;
    }


    public Long getOldCallingNumber() {
        return oldCallingNumber;
    }

    public void setOldCallingNumber(Long oldCallingNumber) {
        this.oldCallingNumber = oldCallingNumber;
    }

    public Long getSwcId() {
        return swcId;
    }

    public void setSwcId(Long swcId) {
        this.swcId = swcId;
    }

    public Long getNewCallingNumber() {
        return newCallingNumber;
    }

    public void setNewCallingNumber(Long newCallingNumber) {
        this.newCallingNumber = newCallingNumber;
    }

}
