package org.motechproject.nms.swcUpdate.service.impl;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.motechproject.nms.csv.exception.CsvImportDataException;
import org.motechproject.nms.csv.utils.GetBoolean;
import org.motechproject.nms.csv.utils.GetInteger;
import org.motechproject.nms.csv.utils.GetString;
import org.motechproject.nms.csv.utils.CsvMapImporter;
import org.motechproject.nms.csv.utils.GetLong;
import org.motechproject.nms.csv.utils.GetLocalDate;
import org.motechproject.nms.csv.utils.CsvImporterBuilder;
import org.motechproject.nms.csv.utils.ConstraintViolationUtils;

import org.motechproject.nms.flw.domain.*;
import org.motechproject.nms.swc.domain.*;
import org.motechproject.nms.swc.exception.SwcExistingRecordException;
import org.motechproject.nms.swc.exception.SwcImportException;
import org.motechproject.nms.swc.repository.ContactNumberAuditDataService;
import org.motechproject.nms.swc.repository.SwcErrorDataService;
import org.motechproject.nms.swc.service.SwcService;
import org.motechproject.nms.swcUpdate.service.SwcImportService;
import org.motechproject.nms.kilkari.contract.AnmAshaRecord;
import org.motechproject.nms.kilkari.contract.RchAnmAshaRecord;
import org.motechproject.nms.kilkari.domain.RejectionReasons;
import org.motechproject.nms.kilkari.utils.FlwConstants;
import org.motechproject.nms.swc.utils.SwcMapper;
import org.motechproject.nms.kilkari.domain.SubscriptionOrigin;
import org.motechproject.nms.kilkari.utils.RejectedObjectConverter;
import org.motechproject.nms.washacademy.service.WashAcademyService;
import org.motechproject.nms.props.service.LogHelper;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.exception.InvalidLocationException;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.service.LocationService;
import org.motechproject.nms.rejectionhandler.service.FlwRejectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;

import javax.jdo.JDODataStoreException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.motechproject.nms.swc.utils.SwcMapper.createSwc;
import static org.motechproject.nms.swc.utils.SwcMapper.createRchSwc;
import static org.motechproject.nms.swc.utils.SwcMapper.updateSwc;

@Service("swcImportService")
public class SwcImportServiceImpl implements SwcImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwcImportServiceImpl.class);
    private SwcService swcService;
    private StateDataService stateDataService;
    private LocationService locationService;
    private SwcErrorDataService swcErrorDataService;
    private WashAcademyService washAcademyService;
    private ContactNumberAuditDataService contactNumberAuditDataService;

    @Autowired
    private FlwRejectionService flwRejectionService;

    /*
        Expected file format:
        * any number of empty lines
        * first non blank line to contain state name in the following format:  State Name : ACTUAL STATE_ID NAME
        * any number of additional header lines
        * one empty line
        * CSV data (tab-separated)
     */
    // CHECKSTYLE:OFF
    @Override
    @Transactional
    public void importData(Reader reader, SubscriptionOrigin importOrigin) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);

        State state = importHeader(bufferedReader);
        CsvMapImporter csvImporter;
        String designation;

        if (importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT)) {
            csvImporter = new CsvImporterBuilder()
                    .setProcessorMapping(getMctsProcessorMapping())
                    .setPreferences(CsvPreference.TAB_PREFERENCE)
                    .createAndOpen(bufferedReader);
            try {
                Map<String, Object> record;
                while (null != (record = csvImporter.read())) {
                    designation = (String) record.get(FlwConstants.TYPE);
                    designation = (designation != null) ? designation.trim() : designation;
                    if (FlwConstants.ASHA_TYPE.equalsIgnoreCase(designation)) {
                        importMctsFrontLineWorker(record, state);
                    }
                }
            } catch (ConstraintViolationException e) {
                throw new CsvImportDataException(createErrorMessage(e.getConstraintViolations(), csvImporter.getRowNumber()), e);
            } catch (InvalidLocationException | SwcImportException | JDODataStoreException | SwcExistingRecordException e) {
                throw new CsvImportDataException(createErrorMessage(e.getMessage(), csvImporter.getRowNumber()), e);
            }
        } else {
            csvImporter = new CsvImporterBuilder()
                    .setProcessorMapping(getRchProcessorMapping())
                    .setPreferences(CsvPreference.TAB_PREFERENCE)
                    .createAndOpen(bufferedReader);
            try {
                Map<String, Object> record;
                while (null != (record = csvImporter.read())) {
                    designation = (String) record.get(FlwConstants.GF_TYPE);
                    designation = (designation != null) ? designation.trim() : designation;
                    if (FlwConstants.ASHA_TYPE.equalsIgnoreCase(designation)) {
                        importRchFrontLineWorker(record, state);
                    }
                }
            } catch (ConstraintViolationException e) {
                throw new CsvImportDataException(createErrorMessage(e.getConstraintViolations(), csvImporter.getRowNumber()), e);
            } catch (InvalidLocationException | SwcImportException | JDODataStoreException | SwcExistingRecordException e) {
                throw new CsvImportDataException(createErrorMessage(e.getMessage(), csvImporter.getRowNumber()), e);
            }
        }
    }

    // CHECKSTYLE:ON
    @Override // NO CHECKSTYLE Cyclomatic Complexity
    @Transactional
    public void importMctsFrontLineWorker(Map<String, Object> record, State state) throws InvalidLocationException, SwcExistingRecordException {
        Swachchagrahi flw = flwFromRecord(record, state);

        record.put(FlwConstants.STATE_ID, state.getCode());
        Map<String, Object> location = locationService.getLocations(record);

        if (flw == null) {
            Swachchagrahi swachchagrahi = createSwc(record, location);
            if (swachchagrahi != null) {
                swcService.add(swachchagrahi);
            }
        } else {
            String datePattern = "\\d{4}-\\d{2}-\\d{2}";
            DateTimeFormatter dtf1 = DateTimeFormat.forPattern("dd-MM-yyyy");
            DateTimeFormatter dtf2 = DateTimeFormat.forPattern("yyyy-MM-dd");
            LocalDate mctsUpdatedDateNic = record.get(FlwConstants.UPDATED_ON) == null || record.get(FlwConstants.UPDATED_ON).toString().trim().isEmpty() ? null :
                    (record.get(FlwConstants.UPDATED_ON).toString().matches(datePattern) ?
                            LocalDate.parse(record.get(FlwConstants.UPDATED_ON).toString(), dtf2) :
                            LocalDate.parse(record.get(FlwConstants.UPDATED_ON).toString(), dtf1));
            //It updated_date_nic from mcts is not null,then it's not a new record. Compare it with the record from database and update
            if (mctsUpdatedDateNic != null && (flw.getUpdatedDateNic() == null || mctsUpdatedDateNic.isAfter(flw.getUpdatedDateNic()) || mctsUpdatedDateNic.isEqual(flw.getUpdatedDateNic()))) {
                Long oldMsisdn = flw.getContactNumber();
                Swachchagrahi flwInstance = updateSwc(flw, record, location, SubscriptionOrigin.MCTS_IMPORT);
                swcService.update(flwInstance);
                Long newMsisdn = (Long) record.get(FlwConstants.CONTACT_NO);
                if (!oldMsisdn.equals(newMsisdn)) {
                    washAcademyService.updateMsisdn(flwInstance.getId(), oldMsisdn, newMsisdn);
                    ContactNumberAudit contactNumberAudit = new ContactNumberAudit(flwInstance.getId());
                    contactNumberAudit.setOldCallingNumber(oldMsisdn);
                    contactNumberAudit.setNewCallingNumber(newMsisdn);
                    contactNumberAudit.setImportDate(LocalDate.now());
                    contactNumberAuditDataService.create(contactNumberAudit);
                }
            } else {
                throw new SwcExistingRecordException("Updated record exists in the database");
            }
        }
    }

    @Override //NO CHECKSTYLE CyclomaticComplexity
    @Transactional
    public void importRchFrontLineWorker(Map<String, Object> record, State state) throws InvalidLocationException, SwcExistingRecordException {
        String flwId = (String) record.get(FlwConstants.GF_ID);
        Long msisdn = (Long) record.get(FlwConstants.MOBILE_NO);

        record.put(FlwConstants.STATE_ID, state.getCode());
        Map<String, Object> location = locationService.getLocations(record);

        Swachchagrahi flw = swcService.getByMctsFlwIdAndState(flwId, state);
        if (flw != null) {
            Swachchagrahi flw2 = swcService.getByContactNumber(msisdn);
            if (flw2 == null || flw2.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                // update msisdn of existing asha worker
                Swachchagrahi newFlw = createRchSwc(record, location);
                if (newFlw != null) {
                    swcService.add(newFlw);
                }
            } else {
                //we got here because an FLW exists with active job status and the same msisdn
                //check if both these records are the same or not
                if (flw.equals(flw2)) {
                    Swachchagrahi flwInstance = updateSwc(flw, record, location, SubscriptionOrigin.RCH_IMPORT);
                    swcService.update(flwInstance);
                } else {
                    LOGGER.debug("New flw but phone number(update) already in use");
                    swcErrorDataService.create(new SwcError(flwId, (long) record.get(FlwConstants.STATE_ID), (long) record.get(FlwConstants.DISTRICT_ID), SwcErrorReason.PHONE_NUMBER_IN_USE));
                    throw new SwcExistingRecordException("Msisdn already in use.");
                }
            }
        } else {
            Swachchagrahi swachchagrahi = swcService.getByContactNumber(msisdn);
            if (swachchagrahi != null && swachchagrahi.getCourseStatus().equals(SwachchagrahiStatus.ACTIVE)) {
                // check if anonymous FLW
                if (swachchagrahi.getSwcId() == null) {
                    Swachchagrahi flwInstance = updateSwc(swachchagrahi, record, location, SubscriptionOrigin.RCH_IMPORT);
                    swcService.update(flwInstance);
                } else {
                    // reject the record
                    LOGGER.debug("Existing FLW with provided msisdn");
                    swcErrorDataService.create(new SwcError(flwId, (long) record.get(FlwConstants.STATE_ID), (long) record.get(FlwConstants.DISTRICT_ID), SwcErrorReason.PHONE_NUMBER_IN_USE));
                    throw new SwcExistingRecordException("Msisdn already in use.");
                }
            } else if (swachchagrahi != null && swachchagrahi.getCourseStatus().equals(SwachchagrahiStatus.ANONYMOUS)) {
                Swachchagrahi flwInstance = updateSwc(swachchagrahi, record, location, SubscriptionOrigin.RCH_IMPORT);
                swcService.update(flwInstance);
            } else {
                // create new FLW record with provided flwId and msisdn
                Swachchagrahi newFlw = createRchSwc(record, location);
                if (newFlw != null) {
                    swcService.add(newFlw);
                }
            }
        }
    }

    @Override // NO CHECKSTYLE Cyclomatic Complexity
    public boolean createUpdate(Map<String, Object> flw, SubscriptionOrigin importOrigin) { //NOPMD NcssMethodCount

        long stateId = (long) flw.get(FlwConstants.STATE_ID);
        long districtId = (long) flw.get(FlwConstants.DISTRICT_ID);
        String flwId = importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT) ? flw.get(FlwConstants.ID).toString() : flw.get(FlwConstants.GF_ID).toString();
        long contactNumber = importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT) ? (long) flw.get(FlwConstants.CONTACT_NO) : (long) flw.get(FlwConstants.MOBILE_NO);
        String action = "";

        State state = locationService.getState(stateId);
        if (state == null) {
            swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_STATE));
            if (importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT)) {
                action = this.flwActionFinder(convertMapToAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            } else {
                action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            }
            return false;
        }
        District district = locationService.getDistrict(stateId, districtId);
        if (district == null) {
            swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_DISTRICT));
            if (importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT)) {
                action = this.flwActionFinder(convertMapToAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            } else {
                action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            }
            return false;
        }

        Swachchagrahi existingFlwByNumber = swcService.getByContactNumber(contactNumber);
        Swachchagrahi existingFlwByFlwId = swcService.getByMctsFlwIdAndState(flwId, state);
        Map<String, Object> location = new HashMap<>();
        try {
            location = locationService.getLocations(flw, false);

            if (importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT)) {
                action = this.flwActionFinder(convertMapToAsha(flw));
                if (existingFlwByFlwId != null && existingFlwByNumber != null) {

                    if (existingFlwByFlwId.getSwcId().equalsIgnoreCase(existingFlwByNumber.getSwcId()) &&
                            existingFlwByFlwId.getState().equals(existingFlwByNumber.getState())) {
                        // we are trying to update the same existing flw. set fields and update
                        LOGGER.debug("Updating existing user with the same phone number");
                        swcService.update(SwcMapper.updateSwc(existingFlwByFlwId, flw, location, SubscriptionOrigin.MCTS_IMPORT));
                        return true;
                    } else if ((!existingFlwByFlwId.getSwcId().equalsIgnoreCase(existingFlwByNumber.getSwcId()) ||
                            !existingFlwByFlwId.getState().equals(existingFlwByNumber.getState())) &&
                            existingFlwByNumber.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                        LOGGER.debug("Updating existing user with same phone number");
                        swcService.update(SwcMapper.updateSwc(existingFlwByFlwId, flw, location, SubscriptionOrigin.MCTS_IMPORT));
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), true, null, action));
                        return true;
                    } else {
                        // we are trying to update 2 different users and/or phone number used by someone else
                        LOGGER.debug("Existing flw but phone number(update) already in use");
                        swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.PHONE_NUMBER_IN_USE));
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                        return false;
                    }
                } else if (existingFlwByFlwId != null && existingFlwByNumber == null) {
                    // trying to update the phone number of the person. possible migration scenario
                    // making design decision that flw will lose all progress when phone number is changed. Usage and tracking is not
                    // worth the effort & we don't really know that its the same flw
                    LOGGER.debug("Updating phone number for flw");
                    long existingContactNumber = existingFlwByFlwId.getContactNumber();
                    Swachchagrahi flwInstance = SwcMapper.updateSwc(existingFlwByFlwId, flw, location, SubscriptionOrigin.MCTS_IMPORT);
                    updateFlwMaMsisdn(flwInstance, existingContactNumber, contactNumber);
                    flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), true, null, action));
                    return true;
                } else if (existingFlwByFlwId == null && existingFlwByNumber != null) {

                    if (existingFlwByNumber.getSwcId() == null) {
                        // we just got data from mcts for a previous anonymous user that subscribed by phone number
                        // merging those records
                        LOGGER.debug("Merging mcts data with previously anonymous user");
                        swcService.update(SwcMapper.updateSwc(existingFlwByNumber, flw, location, SubscriptionOrigin.MCTS_IMPORT));
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), true, null, action));
                        return true;
                    } else if (existingFlwByNumber.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                        LOGGER.debug("Adding new flw user");
                        Swachchagrahi swachchagrahi = SwcMapper.createSwc(flw, location);
                        if (swachchagrahi != null) {
                            swcService.add(swachchagrahi);
                            return true;
                        } else {
                            LOGGER.error("Job Status is INACTIVE. So cannot create record.");
                            return false;
                        }
                    } else {
                        // phone number used by someone else.
                        LOGGER.debug("New flw but phone number(update) already in use");
                        swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.PHONE_NUMBER_IN_USE));
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                        return false;
                    }

                } else { // existingFlwByMctsFlwId & existingFlwByNumber are null)
                    // new user. set fields and add
                    LOGGER.debug("Adding new flw user");
                    Swachchagrahi swachchagrahi = SwcMapper.createSwc(flw, location);
                    if (swachchagrahi != null) {
                        swcService.add(swachchagrahi);
                        return true;
                    } else {
                        LOGGER.error("GF Status is INACTIVE. So cannot create record.");
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), false, RejectionReasons.GF_STATUS_INACTIVE.toString(), action));
                        return false;
                    }
                }
            } else {
                action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
                if (existingFlwByFlwId != null && existingFlwByNumber != null) {

                    if (existingFlwByFlwId.getSwcId().equalsIgnoreCase(existingFlwByNumber.getSwcId()) &&
                            existingFlwByFlwId.getState().equals(existingFlwByNumber.getState())) {
                        // we are trying to update the same existing flw. set fields and update
                        LOGGER.debug("Updating existing user with same phone number");
                        swcService.update(SwcMapper.updateSwc(existingFlwByFlwId, flw, location, SubscriptionOrigin.RCH_IMPORT));
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), true, null, action));
                        return true;
                    } else if ((!existingFlwByFlwId.getSwcId().equalsIgnoreCase(existingFlwByNumber.getSwcId()) ||
                            !existingFlwByFlwId.getState().equals(existingFlwByNumber.getState())) &&
                            existingFlwByNumber.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                        LOGGER.debug("Updating existing user with same phone number");
                        swcService.update(SwcMapper.updateSwc(existingFlwByFlwId, flw, location, SubscriptionOrigin.RCH_IMPORT));
                        return true;
                    } else {
                        // we are trying to update 2 different users and/or phone number used by someone else
                        LOGGER.debug("Existing flw but phone number(update) already in use");
                        swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.PHONE_NUMBER_IN_USE));
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                        return false;
                    }
                } else if (existingFlwByFlwId != null && existingFlwByNumber == null) {
                    // trying to update the phone number of the person. possible migration scenario
                    // making design decision that flw will lose all progress when phone number is changed. Usage and tracking is not
                    // worth the effort & we don't really know that its the same flw
                    LOGGER.debug("Updating phone number for flw");
                    long existingContactNumber = existingFlwByFlwId.getContactNumber();
                    Swachchagrahi flwInstance = SwcMapper.updateSwc(existingFlwByFlwId, flw, location, SubscriptionOrigin.RCH_IMPORT);
                    updateFlwMaMsisdn(flwInstance, existingContactNumber, contactNumber);
                    flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), true, null, action));
                    return true;
                } else if (existingFlwByFlwId == null && existingFlwByNumber != null) {

                    if (existingFlwByNumber.getSwcId() == null) {
                        // we just got data from rch for a previous anonymous user that subscribed by phone number
                        // merging those records
                        LOGGER.debug("Merging rch data with previously anonymous user");
                        swcService.update(SwcMapper.updateSwc(existingFlwByNumber, flw, location, SubscriptionOrigin.RCH_IMPORT));
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), true, null, action));
                        return true;
                    } else if (existingFlwByNumber.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                        LOGGER.debug("Adding new RCH flw user");
                        Swachchagrahi swachchagrahi = SwcMapper.createRchSwc(flw, location);
                        if (swachchagrahi != null) {
                            swcService.add(swachchagrahi);
                            return true;
                        } else {
                            LOGGER.error("GF Status is INACTIVE. So cannot create record.");
                            return false;
                        }
                    } else {
                        // phone number used by someone else.
                        LOGGER.debug("New flw but phone number(update) already in use");
                        swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.PHONE_NUMBER_IN_USE));
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                        return false;
                    }

                } else { // existingFlwByMctsFlwId & existingFlwByNumber are null)
                    // new user. set fields and add
                    LOGGER.debug("Adding new RCH flw user");
                    Swachchagrahi swachchagrahi = SwcMapper.createRchSwc(flw, location);
                    if (swachchagrahi != null) {
                        swcService.add(swachchagrahi);
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), true, null, action));
                        return true;
                    } else {
                        LOGGER.error("GF Status is INACTIVE. So cannot create record.");
                        flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), false, RejectionReasons.GF_STATUS_INACTIVE.toString(), action));
                        return false;
                    }
                }
            }

        } catch (InvalidLocationException ile) {
            LOGGER.debug(ile.toString());
            if (importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT)) {
                action = this.flwActionFinder(convertMapToAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionMcts(convertMapToAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            } else {
                action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.flwRejectionRch(convertMapToRchAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            }
            return false;
        }
    }

    private void updateFlwMaMsisdn(Swachchagrahi flwInstance, Long existingMsisdn, Long newMsisdn) {
        swcService.update(flwInstance);
        washAcademyService.updateMsisdn(flwInstance.getId(), existingMsisdn, newMsisdn);
    }

    private State importHeader(BufferedReader bufferedReader) throws IOException {
        String line = readLineWhileBlank(bufferedReader);
        // expect state name in the first line
        if (line.matches("^State Name : .*$")) {
            String stateName = line.substring(line.indexOf(':') + 1).trim();
            State state = stateDataService.findByName(stateName);
            verify(null != state, "State does not exists");
            readLineWhileNotBlank(bufferedReader);
            return state;
        } else {
            throw new IllegalArgumentException("Invalid file format");
        }
    }

    private Swachchagrahi flwFromRecord(Map<String, Object> record, State state) { //NO CHECKSTYLE CyclomaticComplexity
        Swachchagrahi flw = null;

        String mctsFlwId = (String) record.get(FlwConstants.ID);
        Long msisdn = (Long) record.get(FlwConstants.CONTACT_NO);

        if (mctsFlwId != null) {
            flw = swcService.getByMctsFlwIdAndState(mctsFlwId, state);
        }

        if (flw == null && msisdn != null) {
            flw = swcService.getByContactNumber(msisdn);

            // If we loaded the flw by msisdn but the flw we found has a different mcts id
            // then the data needs to be hand corrected since we don't know if the msisdn has changed or
            // if the mcts id has changed.
            if (flw != null && mctsFlwId != null && flw.getSwcId() != null && !mctsFlwId.equals(flw.getSwcId())) {
                if (flw.getJobStatus().equals(SwcJobStatus.ACTIVE)) {
                    throw new CsvImportDataException(String.format("Existing FLW with same MSISDN (%s) but " +
                            "different MCTS ID (%s != %s) in the state of Active jobStatus", LogHelper.obscure(msisdn), mctsFlwId, flw.getSwcId()));
                } else {
                    throw new CsvImportDataException(String.format("Existing FLW with same MSISDN (%s) but " +
                            "different MCTS ID (%s != %s)", LogHelper.obscure(msisdn), mctsFlwId, flw.getSwcId()));
                }
            }

        } else if (flw != null && msisdn != null) {
            Long id = flw.getId();
            flw = swcService.getByContactNumber(msisdn);

            if (flw != null && flw.getId() != id) {
                throw new CsvImportDataException(String.format("Existing FLW with same MSISDN (%s) but " +
                        "different MCTS ID (%s != %s)", LogHelper.obscure(msisdn), mctsFlwId, flw.getSwcId()));
            } else if (flw == null) {
                flw = swcService.getById(id);
            }
        }

        return flw;
    }

    private String readLineWhileBlank(BufferedReader bufferedReader) throws IOException {
        String line;
        do {
            line = bufferedReader.readLine();
        } while (null != line && StringUtils.isBlank(line));
        return line;
    }

    private String readLineWhileNotBlank(BufferedReader bufferedReader) throws IOException {
        String line;
        do {
            line = bufferedReader.readLine();
        } while (null != line && StringUtils.isNotBlank(line));
        return line;
    }

    private void getMapping(Map<String, CellProcessor> mapping) {
        mapping.put(FlwConstants.STATE_ID, new Optional(new GetLong()));

        mapping.put(FlwConstants.DISTRICT_ID, new Optional(new GetLong()));
        mapping.put(FlwConstants.DISTRICT_NAME, new Optional(new GetString()));

        mapping.put(FlwConstants.TALUKA_ID, new Optional(new GetString()));
        mapping.put(FlwConstants.TALUKA_NAME, new Optional(new GetString()));

        mapping.put(FlwConstants.HEALTH_BLOCK_ID, new Optional(new GetLong()));
        mapping.put(FlwConstants.HEALTH_BLOCK_NAME, new Optional(new GetString()));

        mapping.put(FlwConstants.PHC_ID, new Optional(new GetLong()));
        mapping.put(FlwConstants.PHC_NAME, new Optional(new GetString()));

        mapping.put(FlwConstants.SUB_CENTRE_ID, new Optional(new GetLong()));
        mapping.put(FlwConstants.SUB_CENTRE_NAME, new Optional(new GetString()));

        mapping.put(FlwConstants.CENSUS_VILLAGE_ID, new Optional(new GetLong()));
        mapping.put(FlwConstants.NON_CENSUS_VILLAGE_ID, new Optional(new GetLong()));
        mapping.put(FlwConstants.VILLAGE_NAME, new Optional(new GetString()));
    }

    private Map<String, CellProcessor> getMctsProcessorMapping() {
        Map<String, CellProcessor> mapping = new HashMap<>();
        mapping.put(FlwConstants.ID, new GetString());
        mapping.put(FlwConstants.CONTACT_NO, new GetLong());
        mapping.put(FlwConstants.NAME, new GetString());
        getMapping(mapping);
        mapping.put(FlwConstants.TYPE, new Optional(new GetString()));
        mapping.put(FlwConstants.GF_STATUS, new Optional(new GetString()));
        mapping.put(FlwConstants.UPDATED_ON, new Optional(new GetLocalDate()));

        mapping.put("Reg_Date", new Optional(new GetString()));
        mapping.put("Sex", new Optional(new GetString()));
        mapping.put("SMS_Reply", new Optional(new GetString()));
        mapping.put(FlwConstants.AADHAR_NO, new Optional(new GetInteger()));
        mapping.put("Created_On", new Optional(new GetString()));
        mapping.put("Updated_On", new Optional(new GetString()));
        mapping.put(FlwConstants.BANK_ID, new Optional(new GetInteger()));
        mapping.put("Branch_Name", new Optional(new GetString()));
        mapping.put("IFSC_ID_Code", new Optional(new GetString()));
        mapping.put("Bank_Name", new Optional(new GetString()));
        mapping.put("Acc_No", new Optional(new GetString()));
        mapping.put("Is_Aadhar_linked", new Optional(new GetBoolean()));
        mapping.put("Verify_Date", new Optional(new GetString()));
        mapping.put("Verifier_Name", new Optional(new GetString()));
        mapping.put(FlwConstants.VERIFIER_ID, new Optional(new GetInteger()));
        mapping.put("Call_Ans", new Optional(new GetBoolean()));
        mapping.put("IsPhoneNoCorrect", new Optional(new GetBoolean()));
        mapping.put(FlwConstants.NOCALLREASON, new Optional(new GetInteger()));
        mapping.put(FlwConstants.NOPHONEREASON, new Optional(new GetInteger()));
        mapping.put("Verifier_Remarks", new Optional(new GetString()));
        mapping.put("GF_Address", new Optional(new GetString()));
        mapping.put("Husband_Name", new Optional(new GetString()));

        return mapping;
    }

    private Map<String, CellProcessor> getRchProcessorMapping() {
        Map<String, CellProcessor> mapping = new HashMap<>();
        mapping.put(FlwConstants.GF_ID, new GetString());
        mapping.put(FlwConstants.MOBILE_NO, new GetLong());
        mapping.put(FlwConstants.GF_NAME, new GetString());
        getMapping(mapping);
        mapping.put(FlwConstants.GF_TYPE, new Optional(new GetString()));
        mapping.put(FlwConstants.EXEC_DATE, new Optional(new GetLocalDate()));
        mapping.put(FlwConstants.GF_STATUS, new Optional(new GetString()));

        return mapping;
    }

    private static RchAnmAshaRecord convertMapToRchAsha(Map<String, Object> record) { //NO CHECKSTYLE CyclomaticComplexity
        RchAnmAshaRecord rchAnmAshaRecord = new RchAnmAshaRecord();
        rchAnmAshaRecord.setStateId(record.get(FlwConstants.STATE_ID) == null ? null : (Long) record.get(FlwConstants.STATE_ID));
        rchAnmAshaRecord.setDistrictId(record.get(FlwConstants.DISTRICT_ID) == null ? null : (Long) record.get(FlwConstants.DISTRICT_ID));
        rchAnmAshaRecord.setDistrictName(record.get(FlwConstants.DISTRICT_NAME) == null ? null : (String) record.get(FlwConstants.DISTRICT_NAME));

        rchAnmAshaRecord.setTalukaId(record.get(FlwConstants.TALUKA_ID) == null ? null : (String) record.get(FlwConstants.TALUKA_ID));
        rchAnmAshaRecord.setTalukaName(record.get(FlwConstants.TALUKA_NAME) == null ? null : (String) record.get(FlwConstants.TALUKA_NAME));

        rchAnmAshaRecord.setHealthBlockId(record.get(FlwConstants.HEALTH_BLOCK_ID) == null ? null : (Long) record.get(FlwConstants.HEALTH_BLOCK_ID));
        rchAnmAshaRecord.setHealthBlockName(record.get(FlwConstants.HEALTH_BLOCK_NAME) == null ? null : (String) record.get(FlwConstants.HEALTH_BLOCK_NAME));

        rchAnmAshaRecord.setPhcId(record.get(FlwConstants.PHC_ID) == null ? null : (Long) record.get(FlwConstants.PHC_ID));
        rchAnmAshaRecord.setPhcName(record.get(FlwConstants.PHC_NAME) == null ? null : (String) record.get(FlwConstants.PHC_NAME));

        rchAnmAshaRecord.setSubCentreId(record.get(FlwConstants.SUB_CENTRE_ID) == null ? null : (Long) record.get(FlwConstants.SUB_CENTRE_ID));
        rchAnmAshaRecord.setSubCentreName(record.get(FlwConstants.SUB_CENTRE_NAME) == null ? null : (String) record.get(FlwConstants.SUB_CENTRE_NAME));

        rchAnmAshaRecord.setVillageId(record.get(FlwConstants.CENSUS_VILLAGE_ID) == null ? null : (Long) record.get(FlwConstants.CENSUS_VILLAGE_ID));
        rchAnmAshaRecord.setVillageName(record.get(FlwConstants.VILLAGE_NAME) == null ? null : (String) record.get(FlwConstants.VILLAGE_NAME));
        rchAnmAshaRecord.setGfId(record.get(FlwConstants.GF_ID) == null ? null : (Long) record.get(FlwConstants.GF_ID));
        rchAnmAshaRecord.setMobileNo(record.get(FlwConstants.MOBILE_NO) == null ? null : (String) record.get(FlwConstants.MOBILE_NO));
        rchAnmAshaRecord.setGfName(record.get(FlwConstants.GF_NAME) == null ? null : (String) record.get(FlwConstants.GF_NAME));
        rchAnmAshaRecord.setGfType(record.get(FlwConstants.GF_TYPE) == null ? null : (String) record.get(FlwConstants.GF_TYPE));
        rchAnmAshaRecord.setExecDate(record.get(FlwConstants.EXEC_DATE) == null ? null : (String) record.get(FlwConstants.EXEC_DATE));
        rchAnmAshaRecord.setGfStatus(record.get(FlwConstants.GF_STATUS) == null ? null : (String) record.get(FlwConstants.GF_STATUS));
        return rchAnmAshaRecord;
    }

    private static AnmAshaRecord convertMapToAsha(Map<String, Object> record) { //NO CHECKSTYLE CyclomaticComplexity
        AnmAshaRecord anmAshaRecord = new AnmAshaRecord();
        anmAshaRecord.setStateId(record.get(FlwConstants.STATE_ID) == null ? null : (Long) record.get(FlwConstants.STATE_ID));
        anmAshaRecord.setDistrictId(record.get(FlwConstants.DISTRICT_ID) == null ? null : (Long) record.get(FlwConstants.DISTRICT_ID));
        anmAshaRecord.setDistrictName(record.get(FlwConstants.DISTRICT_NAME) == null ? null : (String) record.get(FlwConstants.DISTRICT_NAME));

        anmAshaRecord.setTalukaId(record.get(FlwConstants.TALUKA_ID) == null ? null : (String) record.get(FlwConstants.TALUKA_ID));
        anmAshaRecord.setTalukaName(record.get(FlwConstants.TALUKA_NAME) == null ? null : (String) record.get(FlwConstants.TALUKA_NAME));

        anmAshaRecord.setHealthBlockId(record.get(FlwConstants.HEALTH_BLOCK_ID) == null ? null : (Long) record.get(FlwConstants.HEALTH_BLOCK_ID));
        anmAshaRecord.setHealthBlockName(record.get(FlwConstants.HEALTH_BLOCK_NAME) == null ? null : (String) record.get(FlwConstants.HEALTH_BLOCK_NAME));

        anmAshaRecord.setPhcId(record.get(FlwConstants.PHC_ID) == null ? null : (Long) record.get(FlwConstants.PHC_ID));
        anmAshaRecord.setPhcName(record.get(FlwConstants.PHC_NAME) == null ? null : (String) record.get(FlwConstants.PHC_NAME));

        anmAshaRecord.setSubCentreId(record.get(FlwConstants.SUB_CENTRE_ID) == null ? null : (Long) record.get(FlwConstants.SUB_CENTRE_ID));
        anmAshaRecord.setSubCentreName(record.get(FlwConstants.SUB_CENTRE_NAME) == null ? null : (String) record.get(FlwConstants.SUB_CENTRE_NAME));

        anmAshaRecord.setVillageId(record.get(FlwConstants.CENSUS_VILLAGE_ID) == null ? null : (Long) record.get(FlwConstants.CENSUS_VILLAGE_ID));
        anmAshaRecord.setVillageName(record.get(FlwConstants.VILLAGE_NAME) == null ? null : (String) record.get(FlwConstants.VILLAGE_NAME));
        anmAshaRecord.setId(record.get(FlwConstants.ID) == null || record.get(FlwConstants.ID).toString().isEmpty() ? null : Long.parseLong(record.get(FlwConstants.ID).toString()));
        anmAshaRecord.setContactNo(record.get(FlwConstants.CONTACT_NO) == null ? null : record.get(FlwConstants.CONTACT_NO).toString());
        anmAshaRecord.setName(record.get(FlwConstants.NAME) == null ? null : (String) record.get(FlwConstants.NAME));
        anmAshaRecord.setType(record.get(FlwConstants.TYPE) == null ? null : (String) record.get(FlwConstants.TYPE));
        anmAshaRecord.setUpdatedOn(record.get(FlwConstants.UPDATED_ON) == null ? null : (String) record.get(FlwConstants.UPDATED_ON));
        anmAshaRecord.setGfStatus(record.get(FlwConstants.GF_STATUS) == null ? null : (String) record.get(FlwConstants.GF_STATUS));

        anmAshaRecord.setRegDate(record.get("Reg_Date") == null ? null : (String) record.get("Reg_Date"));
        anmAshaRecord.setSex(record.get("Sex") == null ? null : (String) record.get("Sex"));
        anmAshaRecord.setSmsReply(record.get("SMS_Reply") == null ? null : (String) record.get("SMS_Reply"));
        anmAshaRecord.setAadharNo(record.get(FlwConstants.AADHAR_NO) == null || record.get(FlwConstants.AADHAR_NO).toString().trim().isEmpty() ? null : (Integer) record.get(FlwConstants.AADHAR_NO));
        anmAshaRecord.setCreatedOn(record.get("Created_On") == null ? null : (String) record.get("Created_On"));
        anmAshaRecord.setUpdatedOn(record.get("Updated_On") == null ? null : (String) record.get("Updated_On"));
        anmAshaRecord.setBankId(record.get(FlwConstants.BANK_ID) == null || record.get(FlwConstants.BANK_ID).toString().trim().isEmpty() ? null : (Integer) record.get(FlwConstants.BANK_ID));
        anmAshaRecord.setBranchName(record.get("Branch_Name") == null ? null : (String) record.get("Branch_Name"));
        anmAshaRecord.setIfscIdCode(record.get("IFSC_ID_Code") == null ? null : (String) record.get("IFSC_ID_Code"));
        anmAshaRecord.setBankName(record.get("Bank_Name") == null ? null : (String) record.get("Bank_Name"));
        anmAshaRecord.setAccNo(record.get("Acc_No") == null ? null : (String) record.get("Acc_No"));
        anmAshaRecord.setIsAadharLinked(record.get("Is_Aadhar_linked") == null ? null : (Boolean) record.get("Is_Aadhar_linked"));
        anmAshaRecord.setVerifyDate(record.get("Verify_Date") == null ? null : (String) record.get("Verify_Date"));
        anmAshaRecord.setVerifierName(record.get("Verifier_Name") == null ? null : (String) record.get("Verifier_Name"));
        anmAshaRecord.setVerifierId(record.get(FlwConstants.VERIFIER_ID) == null || record.get(FlwConstants.VERIFIER_ID).toString().trim().isEmpty() ? null : (Integer) record.get(FlwConstants.VERIFIER_ID));
        anmAshaRecord.setCallAns(record.get("Call_Ans") == null ? null : (Boolean) record.get("Call_Ans"));
        anmAshaRecord.setIsPhoneNoCorrect(record.get("IsPhoneNoCorrect") == null ? null : (Boolean) record.get("IsPhoneNoCorrect"));
        anmAshaRecord.setNoCallReason(record.get(FlwConstants.NOCALLREASON) == null || record.get(FlwConstants.NOCALLREASON).toString().trim().isEmpty() ? null : (Integer) record.get(FlwConstants.NOCALLREASON));
        anmAshaRecord.setNoPhoneReason(record.get(FlwConstants.NOPHONEREASON) == null || record.get(FlwConstants.NOPHONEREASON).toString().trim().isEmpty() ? null : (Integer) record.get(FlwConstants.NOPHONEREASON));
        anmAshaRecord.setVerifierRemarks(record.get("Verifier_Remarks") == null ? null : (String) record.get("Verifier_Remarks"));
        anmAshaRecord.setGfAddress(record.get("GF_Address") == null ? null : (String) record.get("GF_Address"));
        anmAshaRecord.setHusbandName(record.get("Husband_Name") == null ? null : (String) record.get("Husband_Name"));
        return anmAshaRecord;
    }

        private String createErrorMessage(String message, int rowNumber) {
        return String.format("CSV instance error [row: %d]: %s", rowNumber, message);
    }

    private String createErrorMessage(Set<ConstraintViolation<?>> violations, int rowNumber) {
        return String.format("CSV instance error [row: %d]: validation failed for instance of type %s, violations: %s",
                rowNumber, Swachchagrahi.class.getName(), ConstraintViolationUtils.toString(violations));
    }

    private void verify(boolean condition, String message) {
        if (!condition) {
            throw new CsvImportDataException(message);
        }
    }

    @Autowired
    public void setSwcService(SwcService swcService) {
        this.swcService = swcService;
    }

    @Autowired
    public void setStateDataService(StateDataService stateDataService) {
        this.stateDataService = stateDataService;
    }

    @Autowired
    public void setLocationService(LocationService locationService) {
        this.locationService = locationService;
    }

    @Autowired
    public void setSwcErrorDataService(SwcErrorDataService swcErrorDataService) {
        this.swcErrorDataService = swcErrorDataService;
    }

    @Autowired
    public void setWashAcademyService(WashAcademyService washAcademyService) {
        this.washAcademyService = washAcademyService;
    }

    @Autowired
    public void setContactNumberAuditDataService(ContactNumberAuditDataService contactNumberAuditDataService) {
        this.contactNumberAuditDataService = contactNumberAuditDataService;
    }

    private String flwActionFinder(AnmAshaRecord record) {
        if (swcService.getByMctsFlwIdAndState(record.getId().toString(), stateDataService.findByCode(record.getStateId())) == null) {
            return "CREATE";
        } else {
            return "UPDATE";
        }
    }

    private String rchFlwActionFinder(RchAnmAshaRecord record) {
        if (swcService.getByMctsFlwIdAndState(record.getGfId().toString(), stateDataService.findByCode(record.getStateId())) == null) {
            return "CREATE";
        } else {
            return "UPDATE";
        }
    }
}
