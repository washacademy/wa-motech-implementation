package org.motechproject.wa.swc.service.impl;

//import org.joda.time.DateTime;
import org.motechproject.mds.query.SqlQueryExecution;
import org.motechproject.wa.swc.domain.CallDetailRecord;
import org.motechproject.wa.swc.domain.ServiceUsage;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.repository.CallDetailRecordDataService;
import org.motechproject.wa.swc.service.ServiceUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jdo.Query;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

//import org.motechproject.mds.query.QueryExecution;
//import org.motechproject.mds.util.InstanceSecurityRestriction;


@Service("serviceUsageService")
public class ServiceUsageServiceImpl implements ServiceUsageService {
    private CallDetailRecordDataService callDetailRecordDataService;

    @Autowired
    public ServiceUsageServiceImpl(CallDetailRecordDataService callDetailRecordDataService) {
        this.callDetailRecordDataService = callDetailRecordDataService;
    }

    @Override
    public ServiceUsage getCurrentMonthlyUsageForSWCAndService(final Swachchagrahi swachchagrahi, final org.motechproject.wa.props.domain.Service service, Integer courseId) {
        ServiceUsage serviceUsage = new ServiceUsage(swachchagrahi, service, 0, 0, false);
        Long phone = swachchagrahi.getContactNumber();

        @SuppressWarnings("unchecked")
        SqlQueryExecution<List<CallDetailRecord>> queryExecution = new SqlQueryExecution<List<CallDetailRecord>>() {
//            @Override
//            public List<CallDetailRecord> execute(Query query, InstanceSecurityRestriction restriction) {
//                DateTime monthStart = DateTime.now().withDayOfMonth(1).withTimeAtStartOfDay();
//
//                query.setFilter("swachchagrahi == swc && service == swc_service && callStartTime >= monthStart");
//                query.declareParameters("Swachchagrahi swc, org.joda.time.DateTime monthStart, org.motechproject.wa.props.domain.Service swc_service");
//
//                return (List<CallDetailRecord>) query.executeSQLQuery(swachchagrahi, monthStart, service);
//            }

            @Override
            public String getSqlQuery() {
                LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String query = "Select * FROM wash_swachgrahi_cdrs where callingNumber=" + phone +" and callStartTime >= '"+dtf.format(monthStart)+"' and courseId=" + courseId +";";
                return query;
            }
            @Override
            public List<CallDetailRecord> execute(Query query) {

                query.setClass(CallDetailRecord.class);

                return (List<CallDetailRecord>) query.execute();
            }
        };

        List<CallDetailRecord> callDetailRecords = callDetailRecordDataService.executeSQLQuery(queryExecution);

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
