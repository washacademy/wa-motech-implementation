package org.motechproject.nms.api.web.service;

import org.motechproject.nms.api.web.contract.AddSwcRequest;

/**
 * Created by vishnu on 22/9/17.
 */
public interface SwcCsvService {

    StringBuilder csvUploadRch(AddSwcRequest addSwcRequest);

    void persistFlwRch(AddSwcRequest addSwcRequest);

    void csvRejectionsRch(String fieldName, AddSwcRequest addSwcRequest);
}
