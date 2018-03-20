package org.motechproject.wa.testing.service.impl;

import org.motechproject.mds.query.SqlQueryExecution;
import org.motechproject.metrics.service.Timer;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.LanguageService;
import org.motechproject.wa.testing.service.TestingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.jdo.Query;
import java.io.IOException;

@SuppressWarnings("PMD")
@Service("testingService")
public class TestingServiceImpl implements TestingService {

    private static final String TESTING_ENVIRONMENT = "testing.environment";
    private static final int PREGNANCY_PACK_WEEKS = 72;
    private static final int CHILD_PACK_WEEKS = 48;
    private static final int TWO_MINUTES = 120;
    private static final int TEN_SECS = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(TestingServiceImpl.class);
    public static final String CHILD_PACK = "childPack";
    public static final String PREGNANCY_PACK = "pregnancyPack";
    public static final String TESTING_SERVICE_FORBIDDEN = "calling TestingService in a production environment is forbidden!";


    private static final String[] QUERIES = {
    };

    private static final String[] TABLES = {
        "ALERTS_MODULE_ALERT",
        "ALERTS_MODULE_ALERT_DATA",
        "ALERTS_MODULE_ALERT__TRASH",
        "ALERTS_MODULE_ALERT__TRASH_DATA",
        "MTRAINING_MODULE_ACTIVITYRECORD",
        "MTRAINING_MODULE_ACTIVITYRECORD__TRASH",
        "MTRAINING_MODULE_ACTIVITYRECORD_Audit",
        "MTRAINING_MODULE_ACTIVITYRECORD_Audit__TRASH",
        "MTRAINING_MODULE_BOOKMARK",
        "MTRAINING_MODULE_BOOKMARK__TRASH",
        "MTRAINING_MODULE_CHAPTER",
        "MTRAINING_MODULE_CHAPTER__TRASH",
        "MTRAINING_MODULE_COURSE",
        "MTRAINING_MODULE_COURSEUNITMETADATA",
        "MTRAINING_MODULE_COURSEUNITMETADATA__TRASH",
        "MTRAINING_MODULE_COURSE__TRASH",
        "MTRAINING_MODULE_LESSON",
        "MTRAINING_MODULE_LESSON__TRASH",
        "MTRAINING_MODULE_QUESTION",
        "MTRAINING_MODULE_QUESTION__TRASH",
        "MTRAINING_MODULE_QUIZ",
        "MTRAINING_MODULE_QUIZ__TRASH",
        "wa_anonymous_call_details_audit",
        "wa_anonymous_call_details_audit__TRASH",
        "wa_inactive_job_call_audit",
        "wa_inactive_job_call_audit__TRASH",
        "wash_circles",
        "wash_circles__TRASH",
        "wa_contactNumber_audit",
        "wa_contactNumber_audit__TRASH",
        "wa_csv_audit_records",
        "wa_csv_audit_records__TRASH",
        "wa_deployed_services",
        "wa_deployed_services__TRASH",
        "wash_swachgrahi_cdrs",
        "wash_swachgrahi_cdrs__TRASH",
        "wash_swachchagrahi",
        "wash_swachchagrahi__TRASH",
        "wash_districts",
        "wash_districts__TRASH",
        "wa_swc_errors",
            "wa_swc_errors__TRASH",
            "wash_swachgrahi_status_update_audit",
            "wash_swachgrahi_status_update_audit__TRASH",
        "wash_blocks",
        "wash_blocks__TRASH",
        "wa_imi_cdrs",
        "wa_imi_cdrs__TRASH",
        "wa_imi_csrs",
        "wa_imi_csrs__TRASH",
        "wa_imi_file_audit_records",
        "wa_imi_file_audit_records__TRASH",
            "wa_imi_chunk_audit_records",
            "wa_imi_chunk_audit_records__TRASH",
            "wash_call_content",
            "wash_call_content__TRASH",
        "wash_course_completion_records",
        "wash_course_completion_records__TRASH",
        "wash_course",
        "wash_course__TRASH",
        "wa_rch_audit",
        "wa_rch_audit__TRASH",
        "wa_rch_failures",
        "wa_rch_failures__TRASH",
        "wash_national_default_language",
        "wash_national_default_language__TRASH",
        "wa_service_usage_caps",
        "wa_service_usage_caps__TRASH",
        "wash_states",
        "wash_states__TRASH",
        "wash_languages",
        "wash_languages__TRASH",
        "wash_panchayats",
        "wash_panchayats__TRASH",
        "wa_whitelist_entries",
        "wa_whitelist_entries__TRASH",
        "wa_whitelisted_states",
        "wa_whitelisted_states__TRASH",
        "TRACKING_MODULE_CHANGELOG",
        "TRACKING_MODULE_CHANGELOG__TRASH",
        "wash_swachgrahi_rejects",
        "wash_swachgrahi_rejects__TRASH",
        "wa_rch_import_facilitator",
        "wa_rch_import_facilitator__TRASH"
    };

    /**
     * Region
     */
    @Autowired
    private DistrictDataService districtDataService;
    @Autowired
    private LanguageService languageService;
    @Autowired
    private StateDataService stateDataService;


    /**
     * SettingsFacade
     */
    @Autowired
    @Qualifier("testingSettings")
    private SettingsFacade settingsFacade;


    public TestingServiceImpl() {
        //
        // Should only happen on dev / CI machines, so no need to save/restore settings
        //
        System.setProperty("org.motechproject.testing.osgi.http.numTries", "1");
    }


    private MctsHelper createMctsHelper() {
        return new MctsHelper(settingsFacade, stateDataService, districtDataService);
    }


    private void changeConstraints(final boolean disable) {
        SqlQueryExecution sqe = new SqlQueryExecution() {

            @Override
            public String getSqlQuery() {
                return String.format("SET FOREIGN_KEY_CHECKS = %d", disable ? 0 : 1);
            }

            @Override
            public Object execute(Query query) {
                query.execute();
                return null;
            }
        };
        stateDataService.executeSQLQuery(sqe);
    }


    private void disableConstraints() {
        changeConstraints(true);
    }


    private void enableConstraints() {
        changeConstraints(false);
    }


    private void execQuery(final String sqlQuery) {
        SqlQueryExecution sqe = new SqlQueryExecution() {

            @Override
            public String getSqlQuery() {
                return sqlQuery;
            }

            @Override
            public Object execute(Query query) {
                query.execute();
                return null;
            }
        };
        try {
            stateDataService.executeSQLQuery(sqe);
        } catch (Exception e) {
            String s = String.format("Exception While executing \"%s\"", sqlQuery);
            LOGGER.error(s);
            throw e;
        }
    }


    private void truncateTable(final String table) {
        SqlQueryExecution sqe = new SqlQueryExecution() {

            @Override
            public String getSqlQuery() {
                return String.format("DELETE FROM %s WHERE 1=1", table);
            }

            @Override
            public Object execute(Query query) {
                query.execute();
                return null;
            }
        };
        try {
            stateDataService.executeSQLQuery(sqe);
        } catch (Exception e) {
            String s = String.format("Exception while deleting %s : %s", table, e.getMessage());
            LOGGER.error(s);
            throw e;
        }
    }


    @Override
    public void clearDatabase() {
        Timer timer = new Timer();

        if (!Boolean.parseBoolean(settingsFacade.getProperty(TESTING_ENVIRONMENT))) {
            throw new IllegalStateException(TESTING_SERVICE_FORBIDDEN);
        }

        for (String query : QUERIES) {
            execQuery(query);
        }

        disableConstraints();

        for (String table : TABLES) {
            truncateTable(table);
        }

        enableConstraints();

        languageService.cacheEvict(null);

        LOGGER.debug("clearDatabase: {}", timer.time());
    }


    public String createMctsMoms(int count, boolean staticLMP) throws IOException {

        LOGGER.debug("createMctsMoms(count={}, lmp={})", count, staticLMP);

        if (!Boolean.parseBoolean(settingsFacade.getProperty(TESTING_ENVIRONMENT))) {
            throw new IllegalStateException(TESTING_SERVICE_FORBIDDEN);
        }

        MctsHelper mctsHelper = createMctsHelper();
        return mctsHelper.createMoms(count, staticLMP);
    }


    public String createMctsKids(int count, boolean staticDOB) throws IOException {

        LOGGER.debug("createMctsKids(count={}, dob={})", count, staticDOB);

        if (!Boolean.parseBoolean(settingsFacade.getProperty(TESTING_ENVIRONMENT))) {
            throw new IllegalStateException(TESTING_SERVICE_FORBIDDEN);
        }

        MctsHelper mctsHelper = createMctsHelper();
        return mctsHelper.createKids(count, staticDOB);
    }
}
