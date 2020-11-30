package org.motechproject.wa.region.service.impl;

import org.datanucleus.store.rdbms.query.ForwardQueryResult;
import org.motechproject.mds.query.QueryExecution;
import org.motechproject.mds.query.SqlQueryExecution;
import org.motechproject.mds.util.InstanceSecurityRestriction;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.region.service.DistrictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jdo.Query;
import java.util.HashSet;
import java.util.Set;

@Service("districtService")
public class DistrictServiceImpl implements DistrictService {

    private DistrictDataService districtDataService;

    @Autowired
    public DistrictServiceImpl(DistrictDataService districtDataService) {
        this.districtDataService = districtDataService;
    }

    @Override
    public Set<District> getAllForLanguage(final Language language) {

        QueryExecution<Set<District>> stateQueryExecution = new QueryExecution<Set<District>>() {
            @Override
            public Set<District> execute(Query query, InstanceSecurityRestriction restriction) {

                query.setFilter("language == _language");
                query.declareParameters("Language _language");

                Set<District> districts = new HashSet<>();
                ForwardQueryResult fqr = (ForwardQueryResult) query.execute(language);
                for (Object o : fqr) {
                    districts.add((District) o);
                }
                return districts;
            }
        };

        return districtDataService.executeQuery(stateQueryExecution);
    }

    @Override
    public District findById(Long id){
        SqlQueryExecution<District> queryExecution = new SqlQueryExecution<District>() {

            @Override
            public String getSqlQuery() {
                return "select * from wash_districts where id = ?";
            }

            @Override
            public District execute(Query query) {
                query.setClass(District.class);
                ForwardQueryResult fqr = (ForwardQueryResult) query.execute(id);
                if (fqr.isEmpty()) {
                    return null;
                }
                if (fqr.size() == 1) {
                    return (District) fqr.get(0);
                }
                throw new IllegalStateException("More than one row returned!");
            }
        };
        return districtDataService.executeSQLQuery(queryExecution);
    }


    @Override
    public District findByStateAndCode(final State state, final Long code) {

        if (state == null) { return null; }

        SqlQueryExecution<District> queryExecution = new SqlQueryExecution<District>() {

            @Override
            public String getSqlQuery() {
                return "select * from wash_districts where state_id_oid = ? and code = ?";
            }

            @Override
            public District execute(Query query) {
                query.setClass(District.class);
                ForwardQueryResult fqr = (ForwardQueryResult) query.execute(state.getId(), code);
                if (fqr.isEmpty()) {
                    return null;
                }
                if (fqr.size() == 1) {
                    return (District) fqr.get(0);
                }
                throw new IllegalStateException("More than one row returned!");
            }
        };

        return districtDataService.executeSQLQuery(queryExecution);

    }

    @Override
    public District findByStateAndName(final State state, final String name) {

        if (state == null) { return null; }

        SqlQueryExecution<District> queryExecution = new SqlQueryExecution<District>() {

            @Override
            public String getSqlQuery() {
                return "select * from wash_districts where state_id_oid = ? and name = ?";
            }

            @Override
            public District execute(Query query) {
                query.setClass(District.class);
                ForwardQueryResult fqr = (ForwardQueryResult) query.execute(state.getId(), name);
                if (fqr.isEmpty()) {
                    return null;
                }
                if (fqr.size() == 1) {
                    return (District) fqr.get(0);
                }
                throw new IllegalStateException("More than one row returned!");
            }
        };

        return districtDataService.executeSQLQuery(queryExecution);

    }

    @Override
    public District create(District district) {
        return districtDataService.create(district);
    }

    @Override
    public District update(District district) {
        return districtDataService.update(district);
    }

    @Override
    public Object getDetachedField(District district, String fieldName) {
        return districtDataService.getDetachedField(district, fieldName);
    }
}
