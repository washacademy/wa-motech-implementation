package org.motechproject.wa.rch.service.impl;


import org.joda.time.LocalDate;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.mds.query.QueryParams;
import org.motechproject.mds.util.Order;
import org.motechproject.wa.rch.domain.RchImportAudit;
import org.motechproject.wa.rch.domain.RchImportFailRecord;
import org.motechproject.wa.rch.domain.RchUserType;
import org.motechproject.wa.rch.exception.RchWebServiceException;
import org.motechproject.wa.rch.repository.RchImportAuditDataService;
import org.motechproject.wa.rch.repository.RchImportFailRecordDataService;
import org.motechproject.wa.rch.service.RchWebServiceFacade;
import org.motechproject.wa.rch.service.RchWsImportService;
import org.motechproject.wa.rch.utils.Constants;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.repository.StateDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jdo.annotations.Transactional;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("rchWsImportService")
public class RchWsImportServiceImpl implements RchWsImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RchWsImportServiceImpl.class);
    private static final String RCH_WEB_SERVICE = "RCH Web Service";

    @Autowired
    private StateDataService stateDataService;

    @Autowired
    private RchWebServiceFacade rchWebServiceFacade;

    @Autowired
    private AlertService alertService;

    @Autowired
    private RchImportAuditDataService rchImportAuditDataService;

    @Autowired
    private RchImportFailRecordDataService rchImportFailRecordDataService;

    /**
     * Event relay service to handle async notifications
     */
    @Autowired
    private EventRelay eventRelay;

    @Override
    public void startRchImport() {
        eventRelay.sendEventMessage(new MotechEvent(Constants.RCH_IMPORT_EVENT));
    }

    @Override
    public void importFromRch(List<Long> stateIds, LocalDate referenceDate, URL endpoint) {
        LOGGER.info("Starting import from RCH web service");
        LOGGER.info("Pulling data for {}, for states {}", referenceDate, stateIds);

        if (endpoint == null) {
            LOGGER.debug("Using default service endpoint from WSDL");
        } else {
            LOGGER.debug("Using custom endpoint {}", endpoint);
        }

        for (Long stateId : stateIds) {
            sendImportEventForAUserType(stateId, RchUserType.ASHA, referenceDate, endpoint, Constants.RCH_ASHA_IMPORT_SUBJECT);
        }

        LOGGER.info("Initiated import workflow from RCH for mothers and children");
    }


    @MotechListener(subjects = { Constants.RCH_ASHA_IMPORT_SUBJECT })
    @Transactional
    @Override
    public void importRchAshaData(MotechEvent motechEvent) {
        LOGGER.info("Asha import entry point");
        Long stateId = (Long) motechEvent.getParameters().get(Constants.STATE_ID_PARAM);
        LocalDate startReferenceDate = (LocalDate) motechEvent.getParameters().get(Constants.START_DATE_PARAM);
        LocalDate endReferenceDate = (LocalDate) motechEvent.getParameters().get(Constants.END_DATE_PARAM);
        URL endpoint = (URL) motechEvent.getParameters().get(Constants.ENDPOINT_PARAM);

        State state = stateDataService.findByCode(stateId);
        if (state == null) {
            String error = String.format("State with code %s does not exist in database. Skipping RCH SWC import for this state.", stateId);
            LOGGER.error(error);
            rchImportAuditDataService.create(new RchImportAudit(startReferenceDate, endReferenceDate, RchUserType.ASHA, stateId, null, 0, 0, error));
            return;
        }

        String stateName = state.getName();
        Long stateCode = state.getCode();

        try {
            if (rchWebServiceFacade.getAnmAshaData(startReferenceDate, endReferenceDate, endpoint, stateId)) {
                LOGGER.info("RCH SWC responses for state id {} recorded to file successfully.");
            }
        } catch (RchWebServiceException e) {
            String error = String.format("Cannot read SWC data from %s state with state id: %d", stateName, stateCode);
            LOGGER.error(error);
            alertService.create(RCH_WEB_SERVICE, "RCH Web Service Asha Import", e.getMessage() + " " + error, AlertType.CRITICAL, AlertStatus.NEW, 0, null);
            rchImportAuditDataService.create(new RchImportAudit(startReferenceDate, endReferenceDate, RchUserType.ASHA, stateCode, stateName, 0, 0, error));
            rchImportFailRecordDataService.create(new RchImportFailRecord(endReferenceDate, RchUserType.ASHA, stateId));
        }
    }

    private void sendImportEventForAUserType(Long stateId, RchUserType userType, LocalDate referenceDate, URL endpoint, String importSubject) {

        LOGGER.debug("Fetching all the failed imports in the last 7 days for stateId {} and UserType {}", stateId, userType);
        QueryParams queryParams = new QueryParams(new Order("importDate", Order.Direction.ASC));
        List<RchImportFailRecord> failedImports = rchImportFailRecordDataService.getByStateAndImportdateAndUsertype(stateId, referenceDate.minusDays(6), userType, queryParams);
        LocalDate startDate = failedImports.isEmpty() ? referenceDate : failedImports.get(0).getImportDate();

        Map<String, Object> eventParams = new HashMap<>();
        eventParams.put(Constants.START_DATE_PARAM, startDate);
        eventParams.put(Constants.END_DATE_PARAM, referenceDate);
        eventParams.put(Constants.STATE_ID_PARAM, stateId);
        eventParams.put(Constants.ENDPOINT_PARAM, endpoint);
        LOGGER.debug("Sending import message for stateId {} and UserType {}", stateId, userType);
        eventRelay.sendEventMessage(new MotechEvent(importSubject, eventParams));
    }
}
