package org.motechproject.nms.swc.service.impl;

import org.joda.time.DateTime;
import org.motechproject.mds.query.QueryExecution;
import org.motechproject.mds.util.InstanceSecurityRestriction;
import org.motechproject.nms.swc.domain.CallDetailRecord;
import org.motechproject.nms.swc.domain.Swachchagrahi;
import org.motechproject.nms.swc.domain.ServiceUsage;
import org.motechproject.nms.swc.repository.CallDetailRecordDataService;
import org.motechproject.nms.swc.service.ServiceUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jdo.Query;
import java.util.List;


@Service("serviceUsageService")
public class ServiceUsageServiceImpl implements ServiceUsageService {
    private CallDetailRecordDataService callDetailRecordDataService;

    @Autowired
    public ServiceUsageServiceImpl(CallDetailRecordDataService callDetailRecordDataService) {
        this.callDetailRecordDataService = callDetailRecordDataService;
    }

    @Override
    public ServiceUsage getCurrentMonthlyUsageForSWCAndService(final Swachchagrahi swachchagrahi, final org.motechproject.nms.props.domain.Service service) {
        ServiceUsage serviceUsage = new ServiceUsage(swachchagrahi, service, 0, 0, false);

        @SuppressWarnings("unchecked")
        QueryExecution<List<CallDetailRecord>> queryExecution = new QueryExecution<List<CallDetailRecord>>() {
            @Override
            public List<CallDetailRecord> execute(Query query, InstanceSecurityRestriction restriction) {
                DateTime monthStart = DateTime.now().withDayOfMonth(1).withTimeAtStartOfDay();

                query.setFilter("swachchagrahi == swc && service == swc_service && callStartTime >= monthStart");
                query.declareParameters("Swachchagrahi swc, org.joda.time.DateTime monthStart, org.motechproject.nms.props.domain.Service swc_service");

                return (List<CallDetailRecord>) query.execute(swachchagrahi, monthStart, service);
            }
        };

        List<CallDetailRecord> callDetailRecords = callDetailRecordDataService.executeQuery(queryExecution);

        // TODO: I'm not sure I like combining the individual service usage records into an aggregate record and using the same domain object for it.
        for (CallDetailRecord callDetailRecord : callDetailRecords) {
            // Add up pulse usage, endOfUsagePromptCounter and or together welcomePrompt
            serviceUsage.setEndOfUsage(serviceUsage.getEndOfUsage() + callDetailRecord
                    .getEndOfUsagePromptCounter());
            serviceUsage.setUsageInPulses(serviceUsage.getUsageInPulses() + callDetailRecord
                    .getCallDurationInPulses());

            boolean welcomePrompt = callDetailRecord.getWelcomePrompt() != null ? callDetailRecord.getWelcomePrompt() : false;
            serviceUsage.setWelcomePrompt(serviceUsage.getWelcomePrompt() || welcomePrompt);
        }

        return serviceUsage;
    }
}
