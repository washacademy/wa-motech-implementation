package org.motechproject.wa.swc.utils;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.exception.InvalidLocationException;
import org.motechproject.wa.swc.domain.SubscriptionOrigin;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwachchagrahiStatus;
import org.motechproject.wa.swc.domain.SwcJobStatus;

import java.util.Map;

/**
 * Helper class to set swc properties
 */
public final class SwcMapper {

    private static final String ACTIVE = "ACTIVE";

    private SwcMapper() { }

    public static Swachchagrahi createSwc(Map<String, Object> record, Map<String, Object> location)
            throws InvalidLocationException {
        Long contactNumber = (Long) record.get(SwcConstants.CONTACT_NO);
        String gfStatus = (String) record.get(SwcConstants.JOB_STATUS);
        if (gfStatus != null && !gfStatus.isEmpty() && ACTIVE.equals(gfStatus)) {
            Swachchagrahi swc = new Swachchagrahi(contactNumber);
            swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);

            return updateSwc(swc, record, location, SubscriptionOrigin.MCTS_IMPORT);
        } else {
            return null;
        }
    }

    // NO CHECKSTYLE Cyclomatic Complexity
    public static Swachchagrahi createRchSwc(Map<String, Object> record, Map<String, Object> location)
            throws InvalidLocationException {
        Long contactNumber = (Long) record.get(SwcConstants.MOBILE_NO);
        String gfStatus = (String) record.get(SwcConstants.JOB_STATUS);
        if (gfStatus != null && !gfStatus.isEmpty() && ACTIVE.equals(gfStatus)) {
            Swachchagrahi swc = new Swachchagrahi(contactNumber);
            swc.setCourseStatus(SwachchagrahiStatus.INACTIVE);

            return updateSwc(swc, record, location, SubscriptionOrigin.RCH_IMPORT);
        } else {
            return null;
        }
    }

    // CHECKSTYLE:OFF
    public static Swachchagrahi updateSwc(Swachchagrahi swc, Map<String, Object> record, Map<String, Object> location, SubscriptionOrigin importOrigin)
            throws InvalidLocationException {

        String swcId;
        Long contactNumber;
        String name;
        swcId = (String) record.get(SwcConstants.ID);
        contactNumber = (Long) record.get(SwcConstants.MOBILE_NO);
        name = (String) record.get(SwcConstants.NAME);
        String sex;
        Long age;
        String type;
        sex = (String) record.get(SwcConstants.SWC_SEX);
        age = (Long) record.get(SwcConstants.SWC_AGE);
        type = (String) record.get(SwcConstants.TYPE);




        if (contactNumber != null) {
            swc.setContactNumber(contactNumber);
        }

        if (swcId != null) {
            swc.setSwcId(swcId);
        }

        if (name != null) {
            swc.setName(name);
        }
        if (sex != null) {
            swc.setSex(sex);
        }
        if (age != null) {
            swc.setAge(age.intValue());
        }

            SwcJobStatus jobStatus = SwcJobStatus.ACTIVE;
            swc.setJobStatus(jobStatus);


        setSwcLocation(swc, location);

        if (swc.getDesignation() == null) {
            swc.setDesignation(type);
        }

        LocalDate date;
        String datePattern = "\\d{4}-\\d{2}-\\d{2}";
        DateTimeFormatter dtf1 = DateTimeFormat.forPattern("yyyy-MM-dd");
        DateTimeFormatter dtf2 = DateTimeFormat.forPattern("dd-MM-yyyy");
        if (importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT)) {
            date = record.get(SwcConstants.UPDATED_ON) == null || record.get(SwcConstants.UPDATED_ON).toString().trim().isEmpty() ? null :
                    (record.get(SwcConstants.UPDATED_ON).toString().matches(datePattern) ?
                            LocalDate.parse(record.get(SwcConstants.UPDATED_ON).toString(), dtf1) :
                            LocalDate.parse(record.get(SwcConstants.UPDATED_ON).toString(), dtf2));
        } else {
            date = record.get(SwcConstants.EXEC_DATE) == null || record.get(SwcConstants.EXEC_DATE).toString().trim().isEmpty() ? null :
                    (record.get(SwcConstants.EXEC_DATE).toString().matches(datePattern) ?
                            LocalDate.parse(record.get(SwcConstants.EXEC_DATE).toString(), dtf1) :
                            LocalDate.parse(record.get(SwcConstants.EXEC_DATE).toString(), dtf2));
        }
        if (date != null) {
            swc.setUpdatedDateNic(date);
        }


        return swc;
    }

    //CHECKSTYLE:ON
    public static Swachchagrahi setSwcLocation(Swachchagrahi swc, Map<String, Object> locations) throws InvalidLocationException {
        if (locations.get(SwcConstants.STATE_ID) == null && locations.get(SwcConstants.DISTRICT_ID) == null) {
            throw new InvalidLocationException("Missing mandatory state and district fields");
        }

        if (locations.get(SwcConstants.STATE_ID) == null) {
            throw new InvalidLocationException("Missing mandatory state field");
        }

        if (locations.get(SwcConstants.DISTRICT_ID) == null) {
            throw new InvalidLocationException("Missing mandatory district field");
        }

        swc.setState((State) locations.get(SwcConstants.STATE_ID));
        swc.setDistrict((District) locations.get(SwcConstants.DISTRICT_ID));
        swc.setBlock((Block) locations.get(SwcConstants.BLOCK_ID));
        swc.setPanchayat((Panchayat) locations.get(SwcConstants.PANCHAYAT_ID));
        return swc;
    }
}
