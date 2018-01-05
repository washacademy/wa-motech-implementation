package org.motechproject.wa.region.service.impl;

import org.datanucleus.store.rdbms.query.ForwardQueryResult;
import org.motechproject.mds.query.SqlQueryExecution;
import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.repository.BlockDataService;
import org.motechproject.wa.region.service.BlockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jdo.Query;

@Service("blockService")
public class BlockServiceImpl implements BlockService {

    @Autowired
    private BlockDataService dataService;

    @Override
    public Block findByDistrictAndCode(final District district, final String code) {
        if (district == null) { return null; }

        SqlQueryExecution<Block> queryExecution = new SqlQueryExecution<Block>() {

            @Override
            public String getSqlQuery() {
                return "select * from wash_blocks where district_id_oid = ? and code = ?";
            }

            @Override
            public Block execute(Query query) {
                query.setClass(Block.class);
                ForwardQueryResult fqr = (ForwardQueryResult) query.execute(district.getId(), code);
                if (fqr.isEmpty()) {
                    return null;
                }
                if (fqr.size() == 1) {
                    return (Block) fqr.get(0);
                }
                throw new IllegalStateException("More than one row returned!");
            }
        };

        return dataService.executeSQLQuery(queryExecution);
    }

    @Override
    public Block create(Block block) {
        return dataService.create(block);
    }

    @Override
    public Block update(Block block) {
        return dataService.update(block);
    }
}
