package org.motechproject.wa.swc.service.impl;
import org.joda.time.DateTime;
import org.joda.time.Weeks;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.mds.query.QueryExecution;
import org.motechproject.mds.query.SqlQueryExecution;
import org.motechproject.mds.util.InstanceSecurityRestriction;
import org.motechproject.scheduler.contract.RepeatingSchedulableJob;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.service.LanguageService;
import org.motechproject.wa.swc.domain.*;
import org.motechproject.wa.swc.repository.SwcDataService;
import org.motechproject.wa.swc.repository.SwcStatusUpdateAuditDataService;
import org.motechproject.wa.swc.service.SwcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jdo.Query;
import java.util.*;

/**
 * Simple implementation of the {@link SwcService} interface.
 */
@Service("swcService")
public class SwcServiceImpl implements SwcService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwcServiceImpl.class);

    private static final String SWC_PURGE_TIME = "swc.purge_invalid_swc_start_time";
    private static final String SWC_PURGE_SEC_INTERVAL = "swc.purge_invalid_swc_sec_interval";
    private static final String WEEKS_TO_KEEP_INVALID_SWCS = "swc.weeks_to_keep_invalid_swcs";
    private static final String ALLOW_ANONYMOUS_CALLS = "allow_anonymous_calls_for_courseId";

    private static final String SWC_PURGE_EVENT_SUBJECT = "wa.swc.purge_invalid_swc";

    private SwcDataService swcDataService;

    private SwcStatusUpdateAuditDataService swcStatusUpdateAuditDataService;

    private SettingsFacade settingsFacade;
    private MotechSchedulerService schedulerService;
    private LanguageService languageService;

    @Autowired
    public SwcServiceImpl(@Qualifier("swcSettings") SettingsFacade settingsFacade,
                          MotechSchedulerService schedulerService,
                          SwcDataService swcDataService,LanguageService languageService,
                          SwcStatusUpdateAuditDataService swcStatusUpdateAuditDataService) {
        this.swcDataService = swcDataService;
        this.schedulerService = schedulerService;
        this.settingsFacade = settingsFacade;
        this.languageService = languageService;
        this.swcStatusUpdateAuditDataService = swcStatusUpdateAuditDataService;
        schedulePurgeOfOldSwc();
    }

    /**
     * Use the MOTECH scheduler to setup a repeating job
     * The job will start today at the time stored in swc.purge_invalid_swc_start_time in swc.properties
     * It will repeat every swc.purge_invalid_swc_sec_interval seconds (default value is a day)
     */
    private void schedulePurgeOfOldSwc() {
        //Calculate today's fire time
        DateTimeFormatter fmt = DateTimeFormat.forPattern("H:m");
        String timeProp = settingsFacade.getProperty(SWC_PURGE_TIME);
        DateTime time = fmt.parseDateTime(timeProp);
        DateTime today = DateTime.now()                     // This means today's date...
                .withHourOfDay(time.getHourOfDay())         // ...at the hour...
                .withMinuteOfHour(time.getMinuteOfHour())   // ...and minute specified in imi.properties
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        //Second interval between events
        String intervalProp = settingsFacade.getProperty(SWC_PURGE_SEC_INTERVAL);
        Integer secInterval = Integer.parseInt(intervalProp);

        LOGGER.debug(String.format("The %s message will be sent every %ss starting at %s",
                SWC_PURGE_EVENT_SUBJECT, secInterval.toString(), today.toString()));

        //Schedule repeating job
        MotechEvent event = new MotechEvent(SWC_PURGE_EVENT_SUBJECT);
        RepeatingSchedulableJob job = new RepeatingSchedulableJob(
                event,          //MOTECH event
                null,           //repeatCount, null means infinity
                secInterval,    //repeatIntervalInSeconds
                today.toDate(), //startTime
                null,           //endTime, null means no end time
                true);          //ignorePastFiresAtStart

        schedulerService.safeScheduleRepeatingJob(job);
    }

    @MotechListener(subjects = { SWC_PURGE_EVENT_SUBJECT })
    @Transactional
    public void purgeOldInvalidSWCs(MotechEvent event) {
        int weeksToKeepInvalidSWCs = Integer.parseInt(settingsFacade.getProperty(WEEKS_TO_KEEP_INVALID_SWCS));
        final SwachchagrahiStatus status = SwachchagrahiStatus.INVALID;
        final DateTime cutoff = DateTime.now().minusWeeks(weeksToKeepInvalidSWCs).withTimeAtStartOfDay();

        @SuppressWarnings("unchecked")
        QueryExecution<Long> queryExecution = new QueryExecution<Long>() {
            @Override
            public Long execute(Query query, InstanceSecurityRestriction restriction) {

                query.setFilter("courseStatus == invalid && invalidationDate < cutoff");
                query.declareParameters("SwachchagrahiStatus invalid, org.joda.time.DateTime cutoff");

                return query.deletePersistentAll(status, cutoff);
            }
        };

        Long purgedRecordCount = swcDataService.executeQuery(queryExecution);
        LOGGER.info(String.format("Purged %s SWCs with status %s and invalidation date before %s",
                purgedRecordCount, status, cutoff.toString()));
    }

    @Override
    public State getState(Swachchagrahi swachchagrahi) {
        if (swachchagrahi == null) {
            return null;
        }

        State state = null;
        District district = swachchagrahi.getDistrict();

        if (district != null) {
            state = district.getState();
        }
        if (state == null) {
            Language language = swachchagrahi.getLanguage();
            if (language != null) {
                Set<State> states = languageService.getAllStatesForLanguage(language);
                if (states.size() == 1) {
                    state = states.iterator().next();
                }
            }
        }

        return state;
    }

    @Override
    public void add(Swachchagrahi record) {

        // TODO: also check for SWCDesignation, once we add that field
        // TODO: find out which language/location fields are mandatory
        if ((record.getName() != null) && (record.getContactNumber() != null) &&
                (record.getDistrict() != null)) {

//            (record.getLanguage() != null)
            // the record was added via CSV upload and the SWC hasn't called the service yet
            record.setCourseStatus(SwachchagrahiStatus.INACTIVE);

        } else if (record.getContactNumber() != null) {

            // the record was added when the SWC called the IVR service for the first time
            record.setCourseStatus(SwachchagrahiStatus.ANONYMOUS);
        }

        swcDataService.create(record);
        LOGGER.debug("created user with courseId:"+record.getCourseId()+" and contactNumber:"+record.getContactNumber().toString());
    }

    @Override
    public void createSwc(Swachchagrahi swachchagrahi){
        swcDataService.create(swachchagrahi);
    }

    @Override
    public Swachchagrahi getBySwcIdAndCourseId(String swcId, Integer courseId) {
        return swcDataService.findBySwcIdAndCourseId(swcId,courseId);
    }

    @Override
    public Swachchagrahi getBySwcIdAndPanchayatAndCourseId(final String swcId, final Panchayat panchayat, final Integer courseId) {
        if (swcId == null || panchayat == null ) {
            LOGGER.error(String.format("Attempt to look up SWC by a null swcId (%s) or panchayat (%s)",
                    swcId, panchayat == null ? "null" : panchayat.getName()));
            return null;
        }

        @SuppressWarnings("unchecked")
        QueryExecution<Swachchagrahi> queryExecution = new QueryExecution<Swachchagrahi>() {
            @Override
            public Swachchagrahi execute(Query query, InstanceSecurityRestriction restriction) {
                query.setFilter("swcId == _swcId && panchayat == _panchayat && courseId == _courseId");
                query.declareParameters("String _swcId, org.motechproject.wa.region.domain.Panchayat _panchayat, int _courseId");
                query.setClass(Swachchagrahi.class);
                query.setUnique(true);

                return (Swachchagrahi) query.execute(swcId, panchayat, courseId);
            }
        };

        return swcDataService.executeQuery(queryExecution);
    }

    @Override
    public Swachchagrahi getById(Long id) {
        return swcDataService.findById(id);
    }


    @Override
    public Swachchagrahi getByContactNumber(Long contactNumber) {
        SqlQueryExecution<List<Swachchagrahi>> queryExecution = new SqlQueryExecution<List<Swachchagrahi>>() {
            @Override
            public String getSqlQuery() {
                String query = "Select * FROM wash_swachchagrahi where contactNumber=" + contactNumber +" and jobStatus='ACTIVE';";
                return query;
            }
            @Override
            public List<Swachchagrahi> execute(Query query) {

                query.setClass(Swachchagrahi.class);

                return (List<Swachchagrahi>) query.execute();
            }
        };
        List<Swachchagrahi> swcs = swcDataService.executeSQLQuery(queryExecution); //query result set cannot be modified in sorting
        List<Swachchagrahi> swachchagrahis = new ArrayList<>();
        swachchagrahis.addAll(swcs);

        Collections.sort(swachchagrahis, new Comparator<Swachchagrahi>() {
            @Override
            public int compare(Swachchagrahi t1, Swachchagrahi t2) {
                if (t1.getCreationDate().isBefore(t2.getCreationDate())) {
                    return 1;
                } else if (t1.getCreationDate().isAfter(t2.getCreationDate())) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        if (swachchagrahis.size() != 0) {
            return swachchagrahis.get(swcs.size() - 1);
        } else {
            return null;
        }
    }

    @Override
    public Swachchagrahi getByContactNumberAndCourseId(Long contactNumber,Integer courseId){
        SqlQueryExecution<List<Swachchagrahi>> queryExecution = new SqlQueryExecution<List<Swachchagrahi>>() {
            @Override
            public String getSqlQuery() {
                String query = "Select * FROM wash_swachchagrahi where contactNumber=" + contactNumber +" and "+"courseId="+courseId+" and jobStatus='ACTIVE';";
                return query;
            }
            @Override
            public List<Swachchagrahi> execute(Query query) {

                query.setClass(Swachchagrahi.class);

                return (List<Swachchagrahi>) query.execute();
            }
        };
        List<Swachchagrahi> swcs = swcDataService.executeSQLQuery(queryExecution); //query result set cannot be modified in sorting
        List<Swachchagrahi> swachchagrahis = new ArrayList<>();
        swachchagrahis.addAll(swcs);

        Collections.sort(swachchagrahis, new Comparator<Swachchagrahi>() {
            @Override
            public int compare(Swachchagrahi t1, Swachchagrahi t2) {
                if (t1.getCreationDate().isBefore(t2.getCreationDate())) {
                    return 1;
                } else if (t1.getCreationDate().isAfter(t2.getCreationDate())) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        if (swachchagrahis.size() != 0) {
            return swachchagrahis.get(swcs.size() - 1);
        } else {
            return null;
        }
    }

    @Override
    public Swachchagrahi getInctiveByContactNumber(Long contactNumber) {
        SqlQueryExecution<List<Swachchagrahi>> queryExecution = new SqlQueryExecution<List<Swachchagrahi>>() {
            @Override
            public String getSqlQuery() {
                String query = "Select * FROM wash_swachchagrahi where contactNumber=" + contactNumber +" and jobStatus='INACTIVE';";
                return query;
            }
            @Override
            public List<Swachchagrahi> execute(Query query) {

                query.setClass(Swachchagrahi.class);

                return (List<Swachchagrahi>) query.execute();
            }
        };
        List<Swachchagrahi> swcs = swcDataService.executeSQLQuery(queryExecution);
        if (swcs.size() != 0) {
            return swcs.get(swcs.size() - 1);
        } else {
            return null;
        }
    }

    @Override
    public Swachchagrahi getInctiveByContactNumberAndCourseId(Long contactNumber,Integer courseId) {
        SqlQueryExecution<List<Swachchagrahi>> queryExecution = new SqlQueryExecution<List<Swachchagrahi>>() {
            @Override
            public String getSqlQuery() {
                String query = "Select * FROM wash_swachchagrahi where contactNumber=" + contactNumber +" and "+"courseId="+courseId+" and jobStatus='INACTIVE';";
                return query;
            }
            @Override
            public List<Swachchagrahi> execute(Query query) {

                query.setClass(Swachchagrahi.class);

                return (List<Swachchagrahi>) query.execute();
            }
        };
        List<Swachchagrahi> swcs = swcDataService.executeSQLQuery(queryExecution);
        if (swcs.size() != 0) {
            return swcs.get(swcs.size() - 1);
        } else {
            return null;
        }
    }


    @Override
    public List<Swachchagrahi> getRecords() {
        return swcDataService.retrieveAll();
    }

    /**
     * Update Swachchagrahi. If specific fields are added to the record (name, contactNumber, languageLocation,
     * district, designation), the Swachchagrahi's status will also be updated.
     *
     * @param record The Swachchagrahi to update
     */
    @Override
    @Transactional
    public void update(Swachchagrahi record) {


        Swachchagrahi retrievedSwc = getByContactNumberAndCourseId(record.getContactNumber(),record.getCourseId());
        if (retrievedSwc == null) {
            swcDataService.update(record);
            return;
        }
        SwachchagrahiStatus oldStatus = retrievedSwc.getCourseStatus();

        if (oldStatus == SwachchagrahiStatus.ANONYMOUS) {
            // if the SWC was ANONYMOUS and the required fields get added, update her status to ACTIVE

            // TODO: also check for SWCDesignation once we get spec clarity on what that is
            if ((record.getName() != null) && (record.getContactNumber() != null) &&
                    (record.getDistrict() != null)) {

//                record.getLanguage() != null
                record.setCourseStatus(SwachchagrahiStatus.ACTIVE);
                swcDataService.update(record);
                SwcStatusUpdateAudit swcStatusUpdateAudit = new SwcStatusUpdateAudit(DateUtil.now(), record.getSwcId(), record.getContactNumber(), UpdateStatusType.ANONYMOUS_TO_ACTIVE);
                swcStatusUpdateAuditDataService.create(swcStatusUpdateAudit);

                return;
            }
        }

        swcDataService.update(record);

    }

    @Override
    public void delete(Swachchagrahi record) {
        swcDataService.delete(record);
    }

    @Override
    public void deletePreconditionCheck(Swachchagrahi swachchagrahi) {
        int weeksToKeepInvalidSWCs = Integer.parseInt(settingsFacade.getProperty(WEEKS_TO_KEEP_INVALID_SWCS));
        SwachchagrahiStatus status = SwachchagrahiStatus.INVALID;
        DateTime now = new DateTime();

        if (swachchagrahi.getCourseStatus() != status) {
            throw new IllegalStateException("Can not delete a valid SWC");
        }

        if (swachchagrahi.getInvalidationDate() == null) {
            throw new IllegalStateException(String.format("SWC in %s state with null invalidation date", status));
        }

        if (Math.abs(Weeks.weeksBetween(now, swachchagrahi.getInvalidationDate()).getWeeks()) < weeksToKeepInvalidSWCs) {
            throw new IllegalStateException(String.format("SWC must be in %s state for %s weeks before deleting",
                    status, weeksToKeepInvalidSWCs));
        }
    }

    @Override
    public Boolean isAnonymousAllowed(int courseId){
        String flag = settingsFacade.getProperty(ALLOW_ANONYMOUS_CALLS+courseId);
        if(flag.equalsIgnoreCase("true")) {
           return true;
        } else {
            return false;
        }
    }
}