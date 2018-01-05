package org.motechproject.wa.rch.service;

import org.joda.time.LocalDate;

import java.net.URL;

/**
 * Created by beehyvsc on 1/6/17.
 */
public interface RchWebServiceFacade {

    boolean getAnmAshaData(LocalDate from, LocalDate to, URL endpoint, Long stateId);
}
