package org.motechproject.wa.api.web.service;

import org.motechproject.wa.api.web.contract.AddSwcRequest;

/**
 * Created by vishnu on 22/9/17.
 */
public interface SwcCsvService {

    StringBuilder csvUploadRch(AddSwcRequest addSwcRequest);

    void persistSwcRch(AddSwcRequest addSwcRequest);

    void csvRejectionsRch(String fieldName, AddSwcRequest addSwcRequest);
}
