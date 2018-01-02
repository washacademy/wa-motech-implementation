package org.motechproject.nms.api.web.contract.washAcademy;

import org.motechproject.nms.api.web.contract.washAcademy.sms.RequestData;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Sms status object sent to NMS from IMI
 */
public class SmsStatusRequest {

    @Valid
    @NotNull
    private RequestData requestData;

    public SmsStatusRequest() {
    }

    public RequestData getRequestData() {
        return requestData;
    }

    public void setRequestData(RequestData requestData) {
        this.requestData = requestData;
    }

    @Override
    public String toString() {
        return "SmsStatusRequest{" +
                "requestData=" + requestData +
                '}';
    }
}
