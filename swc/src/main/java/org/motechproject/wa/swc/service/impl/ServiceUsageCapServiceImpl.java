package org.motechproject.wa.swc.service.impl;

import org.motechproject.mds.query.QueryExecution;
import org.motechproject.mds.util.InstanceSecurityRestriction;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.swc.domain.ServiceUsageCap;
import org.motechproject.wa.swc.repository.ServiceUsageCapDataService;
import org.motechproject.wa.swc.service.ServiceUsageCapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jdo.Query;

@Service("serviceUsageCapService")
public class ServiceUsageCapServiceImpl implements ServiceUsageCapService {
    private ServiceUsageCapDataService serviceUsageCapDataService;

    @Autowired
    public ServiceUsageCapServiceImpl(ServiceUsageCapDataService serviceUsageCapDataService) {
        this.serviceUsageCapDataService = serviceUsageCapDataService;
    }

    /*
    The spec was a little unclear on which cap took precedence.
    (Sara in mail 4/15/15: it should be possible to set both a national cap or a state cap. If you set a
                           state cap for a service, then the national cap does not apply. If no state cap is set, then
                           the national cap applies.
     */
    @Override
    public ServiceUsageCap getServiceUsageCap(final State state, final org.motechproject.wa.props.domain.Service service, final Integer courseId) {

        // Todo: #59 Since the only difference between the state and national query is the value of state they should
        //       be combined
        if (null != state) {
            // Find a state cap by providing a state
            QueryExecution<ServiceUsageCap> stateQueryExecution = new QueryExecution<ServiceUsageCap>() {
                @Override
                public ServiceUsageCap execute(Query query, InstanceSecurityRestriction restriction) {

                    query.setFilter("state == swc_state && service == swc_service && courseId == swc_courseId ");
                    query.declareParameters("org.motechproject.wa.region.domain.State swc_state, org.motechproject.wa.props.domain.Service swc_service, Integer swc_courseId");
                    query.setUnique(true);

                    return (ServiceUsageCap) query.execute(state, service, courseId);
                }
            };

            ServiceUsageCap stateServiceUsageCap = serviceUsageCapDataService.executeQuery(stateQueryExecution);

            if (null != stateServiceUsageCap) {
                return stateServiceUsageCap;
            }
        }

        // Find the national cap by looking for a record with null state
        QueryExecution<ServiceUsageCap> nationalQueryExecution = new QueryExecution<ServiceUsageCap>() {
            @Override
            public ServiceUsageCap execute(Query query, InstanceSecurityRestriction restriction) {

                query.setFilter("state == swc_state && service == swc_service && courseId == swc_courseId ");
                query.declareParameters("org.motechproject.wa.region.domain.State swc_state, org.motechproject.wa.props.domain.Service swc_service, Integer swc_courseId");
                query.setUnique(true);

                return (ServiceUsageCap) query.execute(null, service, courseId);
            }
        };

        ServiceUsageCap nationalServiceUsageCap = serviceUsageCapDataService.executeQuery(nationalQueryExecution);

        if (null != nationalServiceUsageCap) {
            return nationalServiceUsageCap;
        }

        // Usage is uncapped
        return new ServiceUsageCap(null, null, -1, courseId );
    }
}
