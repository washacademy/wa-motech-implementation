package org.motechproject.wa.swcUpdate.service;

import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.exception.InvalidLocationException;
import org.motechproject.wa.swc.domain.SubscriptionOrigin;
import org.motechproject.wa.swc.exception.SwcExistingRecordException;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public interface SwcImportService {

    void importData(Reader reader, SubscriptionOrigin importOrigin) throws IOException;

    void importMctsFrontLineWorker(Map<String, Object> record, State state) throws InvalidLocationException, SwcExistingRecordException;

    void importRchFrontLineWorker(Map<String, Object> record, Panchayat panchayat) throws InvalidLocationException, SwcExistingRecordException;
    /**
     * Used to create or update an SWC from mcts or other sync services
     * @param swcRecord key-value pair of properties for swc
     */
    boolean createUpdate(Map<String, Object> swcRecord, SubscriptionOrigin importOrigin);
}
