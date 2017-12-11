package org.motechproject.nms.region.service.impl;

import org.datanucleus.store.rdbms.query.ForwardQueryResult;
import org.motechproject.mds.query.SqlQueryExecution;
import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.domain.Panchayat;
import org.motechproject.nms.region.repository.PanchayatDataService;
import org.motechproject.nms.region.service.PanchayatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jdo.Query;

@Service("villageService")
public class PanchayatServiceImpl implements PanchayatService {

    @Autowired
    private PanchayatDataService dataService;


    @Override
    public Panchayat findByBlockAndVcodeAndSvid(final Block block, final long vcode, final long svid) {
        if (block == null) { return null; }

        SqlQueryExecution<Panchayat> queryExecution = new SqlQueryExecution<Panchayat>() {

            @Override
            public String getSqlQuery() {
                return "select * from nms_villages where taluka_id_oid = ? and vcode = ? and svid = ?";
            }

            @Override
            public Panchayat execute(Query query) {
                query.setClass(Panchayat.class);
                ForwardQueryResult fqr = (ForwardQueryResult) query.execute(block.getId(), vcode, svid);
                if (fqr.isEmpty()) {
                    return null;
                }
                if (fqr.size() == 1) {
                    return (Panchayat) fqr.get(0);
                }
                throw new IllegalStateException("More than one row returned!");
            }
        };

        return dataService.executeSQLQuery(queryExecution);
    }

    @Override
    public Panchayat create(Panchayat panchayat) {
        return dataService.create(panchayat);
    }

    @Override
    public Panchayat update(Panchayat panchayat) {
        return dataService.update(panchayat);
    }
}
