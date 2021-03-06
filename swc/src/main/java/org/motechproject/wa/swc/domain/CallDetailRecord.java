package org.motechproject.wa.swc.domain;

import org.codehaus.jackson.annotate.JsonManagedReference;
import org.joda.time.DateTime;
import org.motechproject.mds.annotations.Cascade;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;
import org.motechproject.wa.props.domain.CallDisconnectReason;
import org.motechproject.wa.props.domain.FinalCallStatus;
import org.motechproject.wa.props.domain.Service;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.Persistent;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Entity(tableName = "wash_swachgrahi_cdrs")
public class CallDetailRecord {

    public CallDetailRecord() {
    }

    @Field
    private Swachchagrahi swachchagrahi;

    @Field
    @NotNull
    @Column(allowsNull = "false")
    private Service service;

    @Field
    @Min(value = 1000000000L, message = "callingNumber must be 10 digits")
    @Max(value = 9999999999L, message = "callingNumber must be 10 digits")
    @Column(length = 10)
    private long callingNumber;

    @Field
    @Column(length = 25)
    private String callId;

    @Field
    @Column(length = 255)
    private String operator;

    @Field
    @Column(length = 255)
    private String circle;

    @Field
    private DateTime callStartTime;

    @Field
    private DateTime callEndTime;

    @Field
    private int callDurationInPulses;

    @Field
    private int endOfUsagePromptCounter;

    @Field
    private Boolean welcomePrompt;

    @Field
    private FinalCallStatus finalCallStatus;

    @Field
    private CallDisconnectReason callDisconnectReason;

    @Field
    @Persistent(mappedBy = "callDetailRecord", defaultFetchGroup = "false")
    @Cascade(delete = true)
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "id ASC"))
    @JsonManagedReference
    private List<CallContent> content;

    public Swachchagrahi getSwachchagrahi() {
        return swachchagrahi;
    }

    public void setSwachchagrahi(Swachchagrahi swachchagrahi) {
        this.swachchagrahi = swachchagrahi;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public long getCallingNumber() {
        return callingNumber;
    }

    public void setCallingNumber(long callingNumber) {
        this.callingNumber = callingNumber;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getCircle() {
        return circle;
    }

    public void setCircle(String circle) {
        this.circle = circle;
    }

    public DateTime getCallStartTime() {
        return callStartTime;
    }

    public void setCallStartTime(DateTime callStartTime) {
        this.callStartTime = callStartTime;
    }

    public DateTime getCallEndTime() {
        return callEndTime;
    }

    public void setCallEndTime(DateTime callEndTime) {
        this.callEndTime = callEndTime;
    }

    public int getCallDurationInPulses() {
        return callDurationInPulses;
    }

    public void setCallDurationInPulses(int callDurationInPulses) {
        this.callDurationInPulses = callDurationInPulses;
    }

    public int getEndOfUsagePromptCounter() {
        return endOfUsagePromptCounter;
    }

    public void setEndOfUsagePromptCounter(int endOfUsagePromptCounter) {
        this.endOfUsagePromptCounter = endOfUsagePromptCounter;
    }

    public Boolean getWelcomePrompt() {
        return welcomePrompt;
    }

    public void setWelcomePrompt(Boolean welcomePrompt) {
        this.welcomePrompt = welcomePrompt;
    }

    public FinalCallStatus getFinalCallStatus() {
        return finalCallStatus;
    }

    public void setFinalCallStatus(FinalCallStatus finalCallStatus) {
        this.finalCallStatus = finalCallStatus;
    }

    public CallDisconnectReason getCallDisconnectReason() {
        return callDisconnectReason;
    }

    public void setCallDisconnectReason(CallDisconnectReason callDisconnectReason) {
        this.callDisconnectReason = callDisconnectReason;
    }

    public void setCallDisconnectReason(int i) {
        this.callDisconnectReason = CallDisconnectReason.fromInt(i);
    }

    public List<CallContent> getContent() {
        if (content == null) {
            return Collections.emptyList();
        }

        return content;
    }

    public void setContent(List<CallContent> content) {
        this.content = content;
    }
}
