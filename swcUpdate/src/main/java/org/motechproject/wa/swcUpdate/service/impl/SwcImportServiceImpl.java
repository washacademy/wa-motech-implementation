package org.motechproject.wa.swcUpdate.service.impl;

import org.apache.commons.lang.StringUtils;
import org.motechproject.wa.csv.exception.CsvImportDataException;
import org.motechproject.wa.csv.utils.GetString;
import org.motechproject.wa.csv.utils.CsvMapImporter;
import org.motechproject.wa.csv.utils.GetLong;
import org.motechproject.wa.csv.utils.GetLocalDate;
import org.motechproject.wa.csv.utils.CsvImporterBuilder;
import org.motechproject.wa.csv.utils.ConstraintViolationUtils;

import org.motechproject.wa.region.domain.*;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwcJobStatus;
import org.motechproject.wa.swc.domain.SwcError;
import org.motechproject.wa.swc.domain.SwachchagrahiStatus;
import org.motechproject.wa.swc.domain.SwcErrorReason;
import org.motechproject.wa.swc.exception.SwcExistingRecordException;
import org.motechproject.wa.swc.exception.SwcImportException;
import org.motechproject.wa.swc.repository.ContactNumberAuditDataService;
import org.motechproject.wa.swc.repository.SwcErrorDataService;
import org.motechproject.wa.swc.domain.RejectionReasons;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.swcUpdate.contract.SwcRecord;
import org.motechproject.wa.swcUpdate.service.SwcImportService;
import org.motechproject.wa.swc.utils.SwcConstants;
import org.motechproject.wa.swc.utils.SwcMapper;
import org.motechproject.wa.swc.domain.SubscriptionOrigin;
import org.motechproject.wa.swcUpdate.utils.RejectedObjectConverter;
import org.motechproject.wa.washacademy.service.WashAcademyService;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.region.exception.InvalidLocationException;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.repository.PanchayatDataService;
import org.motechproject.wa.region.service.LocationService;
import org.motechproject.wa.rejectionhandler.service.SwcRejectionService;
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

import static org.motechproject.wa.swc.utils.SwcMapper.createRchSwc;
import static org.motechproject.wa.swc.utils.SwcMapper.updateSwc;

@SuppressWarnings("PMD")
@Service("swcImportService")
public class SwcImportServiceImpl implements SwcImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwcImportServiceImpl.class);
    private SwcService swcService;
    private StateDataService stateDataService;
    private PanchayatDataService panchayatDataService;
    private LocationService locationService;
    private SwcErrorDataService swcErrorDataService;
    private WashAcademyService washAcademyService;
    private ContactNumberAuditDataService contactNumberAuditDataService;

    @Autowired
    private SwcRejectionService swcRejectionService;

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

        Panchayat panchayat = importHeader(bufferedReader);
        CsvMapImporter csvImporter;
            csvImporter = new CsvImporterBuilder()
                    .setProcessorMapping(getRchProcessorMapping())
                    .setPreferences(CsvPreference.TAB_PREFERENCE)
                    .createAndOpen(bufferedReader);
            try {
                Map<String, Object> record;
                while (null != (record = csvImporter.read())) {
                        importRchFrontLineWorker(record, panchayat);

                }
            } catch (ConstraintViolationException e) {
                throw new CsvImportDataException(createErrorMessage(e.getConstraintViolations(), csvImporter.getRowNumber()), e);
            } catch (InvalidLocationException | SwcImportException | JDODataStoreException | SwcExistingRecordException e) {
                throw new CsvImportDataException(createErrorMessage(e.getMessage(), csvImporter.getRowNumber()), e);
            }

    }

    // CHECKSTYLE:ON
    @Override // NO CHECKSTYLE Cyclomatic Complexity
    @Transactional
    public void importMctsFrontLineWorker(Map<String, Object> record, State state) throws InvalidLocationException, SwcExistingRecordException {

    }

    @Override //NO CHECKSTYLE CyclomaticComplexity
    @Transactional

    public void importRchFrontLineWorker(Map<String, Object> record, Panchayat panchayat) throws InvalidLocationException, SwcExistingRecordException {
        String swcId = (String) record.get(SwcConstants.ID);
        Long msisdn = (Long) record.get(SwcConstants.MOBILE_NO);

        record.put(SwcConstants.PANCHAYAT_ID, panchayat.getPanchayatCode());
        Map<String, Object> location = locationService.getLocations(record);

        Swachchagrahi swc = swcService.getBySwcIdAndPanchayat(swcId, panchayat);
        if (swc != null) {
            Swachchagrahi swc2 = swcService.getByContactNumber(msisdn);
            if (swc2 == null) {
                // update msisdn of existing asha worker
                    Swachchagrahi swcInstance = updateSwc(swc, record, location, SubscriptionOrigin.RCH_IMPORT);
                    swcService.update(swcInstance);

            } else {
                //we got here because an SWC exists with active job status and the same msisdn
                //check if both these records are the same or not
                if (swc.equals(swc2)) {
                    Swachchagrahi swcInstance = updateSwc(swc, record, location, SubscriptionOrigin.RCH_IMPORT);
                    swcService.update(swcInstance);
                } else {
                    LOGGER.debug("New swc but phone number(update) already in use");
                    swcErrorDataService.create(new SwcError(swcId, (long) record.get(SwcConstants.STATE_ID), (long) record.get(SwcConstants.DISTRICT_ID), SwcErrorReason.PHONE_NUMBER_IN_USE));
                    throw new SwcExistingRecordException("Msisdn already in use.");
                }
            }
        } else {
            Swachchagrahi swachchagrahi = swcService.getByContactNumber(msisdn);
            if (swachchagrahi != null && swachchagrahi.getCourseStatus().equals(SwachchagrahiStatus.ACTIVE)) {
                // check if anonymous SWC
                if (swachchagrahi.getSwcId() == null) {
                    Swachchagrahi swcInstance = updateSwc(swachchagrahi, record, location, SubscriptionOrigin.RCH_IMPORT);
                    swcService.update(swcInstance);
                } else {
                    // reject the record
                    LOGGER.debug("Existing SWC with provided msisdn");

                    swcErrorDataService.create(new SwcError(swcId, (long) record.get(SwcConstants.STATE_ID), (long) record.get(SwcConstants.DISTRICT_ID), SwcErrorReason.PHONE_NUMBER_IN_USE));

                    throw new SwcExistingRecordException("Msisdn already in use.");
                }
            } else if (swachchagrahi != null && swachchagrahi.getCourseStatus().equals(SwachchagrahiStatus.ANONYMOUS)) {
                Swachchagrahi swcInstance = updateSwc(swachchagrahi, record, location, SubscriptionOrigin.RCH_IMPORT);
                swcService.update(swcInstance);
            } else {
                // create new SWC record with provided swcId and msisdn
                Swachchagrahi newSwc = createRchSwc(record, location);
                if (newSwc != null) {
                    swcService.add(newSwc);
                }
            }
        }
    }

    @Override // NO CHECKSTYLE Cyclomatic Complexity

    public boolean createUpdate(Map<String, Object> swc, SubscriptionOrigin importOrigin) { //NOPMD NcssMethodCount

        long stateId = (long) swc.get(SwcConstants.STATE_ID);
        long districtId = (long) swc.get(SwcConstants.DISTRICT_ID);
        String blockId = (String) swc.get(SwcConstants.BLOCK_ID);
        long panchayatId = (long) swc.get(SwcConstants.PANCHAYAT_ID);
        String swcId = importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT) ? swc.get(SwcConstants.ID).toString() : swc.get(SwcConstants.GF_ID).toString();
        long contactNumber = importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT) ? (long) swc.get(SwcConstants.CONTACT_NO) : (long) swc.get(SwcConstants.MOBILE_NO);

        String action = "";

        State state = locationService.getState(stateId);
        if (state == null) {
            swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_STATE));
//                action = this.swcActionFinder(convertMapToAsha(swc));
                swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            return false;
        }
        District district = locationService.getDistrict(stateId, districtId);
        if (district == null) {
            swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_DISTRICT));
                action = this.rchSwcActionFinder(convertMapToRchAsha(swc));
                swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), action));

            return false;
        }
        Block block = locationService.getBlock(stateId, districtId, blockId);
        if (block == null) {
            swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_BLOCK));
            action = this.rchSwcActionFinder(convertMapToRchAsha(swc));
            swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), action));

            return false;
        }
        Panchayat panchayat = locationService.getPanchayat(stateId, districtId, blockId, panchayatId, panchayatId);
        if (panchayat == null) {
            swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_PANCHAYAT));
            action = this.rchSwcActionFinder(convertMapToRchAsha(swc));
            swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), action));

            return false;
        }

        Swachchagrahi existingSwcByNumber = swcService.getByContactNumber(contactNumber);
        Swachchagrahi existingSwcBySwcId = swcService.getBySwcIdAndPanchayat(swcId, panchayat);
        Map<String, Object> location = new HashMap<>();
        try {
            location = locationService.getLocations(swc, false);

                action = this.rchSwcActionFinder(convertMapToRchAsha(swc));
                if (existingSwcBySwcId != null && existingSwcByNumber != null) {

                    if (existingSwcBySwcId.getSwcId().equalsIgnoreCase(existingSwcByNumber.getSwcId()) &&
                            existingSwcBySwcId.getPanchayat().equals(existingSwcByNumber.getPanchayat())) {
                        // we are trying to update the same existing swc. set fields and update
                        LOGGER.debug("Updating existing user with same phone number");
                        swcService.update(SwcMapper.updateSwc(existingSwcBySwcId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, action));
                        return true;
                    } else if ((!existingSwcBySwcId.getSwcId().equalsIgnoreCase(existingSwcByNumber.getSwcId()) ||
                            !existingSwcBySwcId.getPanchayat().equals(existingSwcByNumber.getPanchayat())) &&
                            existingSwcByNumber.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                        LOGGER.debug("Updating existing user with same phone number");
                        swcService.update(SwcMapper.updateSwc(existingSwcBySwcId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                        return true;
                    } else {
                        // we are trying to update 2 different users and/or phone number used by someone else
                        LOGGER.debug("Existing swc but phone number(update) already in use");
                        swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.PHONE_NUMBER_IN_USE));
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                        return false;
                    }
                } else if (existingSwcBySwcId != null && existingSwcByNumber == null) {
                    // trying to update the phone number of the person. possible migration scenario
                    // making design decision that swc will lose all progress when phone number is changed. Usage and tracking is not
                    // worth the effort & we don't really know that its the same swc
                    LOGGER.debug("Updating phone number for swc");
                    long existingContactNumber = existingSwcBySwcId.getContactNumber();
                    Swachchagrahi swcInstance = SwcMapper.updateSwc(existingSwcBySwcId, swc, location, SubscriptionOrigin.RCH_IMPORT);
                    updateSwcMaMsisdn(swcInstance, existingContactNumber, contactNumber);
                    swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, action));
                    return true;
                } else if (existingSwcBySwcId == null && existingSwcByNumber != null) {

                    if (existingSwcByNumber.getSwcId() == null) {
                        // we just got data from rch for a previous anonymous user that subscribed by phone number
                        // merging those records
                        LOGGER.debug("Merging rch data with previously anonymous user");
                        swcService.update(SwcMapper.updateSwc(existingSwcByNumber, swc, location, SubscriptionOrigin.RCH_IMPORT));
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, action));
                        return true;
                    } else if (existingSwcByNumber.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                        LOGGER.debug("Adding new RCH swc user");
                        Swachchagrahi swachchagrahi = SwcMapper.createRchSwc(swc, location);
                        if (swachchagrahi != null) {
                            swcService.add(swachchagrahi);
                            return true;
                        } else {
                            LOGGER.error("GF Status is INACTIVE. So cannot create record.");
                            return false;
                        }
                    } else {
                        // phone number used by someone else.
                        LOGGER.debug("New swc but phone number(update) already in use");
                        swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.PHONE_NUMBER_IN_USE));
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                        return false;
                    }

                } else { // existingSwcByMctsSwcId & existingSwcByNumber are null)
                    // new user. set fields and add
                    LOGGER.debug("Adding new RCH swc user");
                    Swachchagrahi swachchagrahi = SwcMapper.createRchSwc(swc, location);
                    if (swachchagrahi != null) {
                        swcService.add(swachchagrahi);
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, action));
                        return true;
                    } else {
                        LOGGER.error("GF Status is INACTIVE. So cannot create record.");
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.GF_STATUS_INACTIVE.toString(), action));
                        return false;
                    }
                }


        } catch (InvalidLocationException ile) {
            LOGGER.debug(ile.toString());
                action = this.rchSwcActionFinder(convertMapToRchAsha(swc));
                swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            return false;
        }
    }

    private void updateSwcMaMsisdn(Swachchagrahi swcInstance, Long existingMsisdn, Long newMsisdn) {
        swcService.update(swcInstance);
        washAcademyService.updateMsisdn(swcInstance.getId(), existingMsisdn, newMsisdn);
    }

    private Panchayat importHeader(BufferedReader bufferedReader) throws IOException {
        String line = readLineWhileBlank(bufferedReader);
        // expect state name in the first line
        if (line.matches("^Panchayat Name : .*$")) {
            String panchayatName = line.substring(line.indexOf(':') + 1).trim();
            Panchayat panchayat = panchayatDataService.findByName(panchayatName);
            verify(null != panchayat, "panchayat does not exists");
            readLineWhileNotBlank(bufferedReader);
            return panchayat;
        } else {
            throw new IllegalArgumentException("Invalid file format");
        }
    }

    private Swachchagrahi swcFromRecord(Map<String, Object> record, Panchayat state) { //NO CHECKSTYLE CyclomaticComplexity
        Swachchagrahi swc = null;

        String mctsSwcId = (String) record.get(SwcConstants.ID);
        Long msisdn = (Long) record.get(SwcConstants.CONTACT_NO);


        if (mctsSwcId != null) {
            swc = swcService.getBySwcIdAndPanchayat(mctsSwcId, state);
        }

        if (swc == null && msisdn != null) {
            swc = swcService.getByContactNumber(msisdn);

            // If we loaded the swc by msisdn but the swc we found has a different mcts id
            // then the data needs to be hand corrected since we don't know if the msisdn has changed or
            // if the mcts id has changed.
            if (swc != null && mctsSwcId != null && swc.getSwcId() != null && !mctsSwcId.equals(swc.getSwcId())) {
                if (swc.getJobStatus().equals(SwcJobStatus.ACTIVE)) {
                    throw new CsvImportDataException(String.format("Existing SWC with same MSISDN (%s) but " +
                            "different MCTS ID (%s != %s) in the state of Active jobStatus", LogHelper.obscure(msisdn), mctsSwcId, swc.getSwcId()));
                } else {
                    throw new CsvImportDataException(String.format("Existing SWC with same MSISDN (%s) but " +
                            "different MCTS ID (%s != %s)", LogHelper.obscure(msisdn), mctsSwcId, swc.getSwcId()));
                }
            }

        } else if (swc != null && msisdn != null) {
            Long id = swc.getId();
            swc = swcService.getByContactNumber(msisdn);

            if (swc != null && swc.getId() != id) {
                throw new CsvImportDataException(String.format("Existing SWC with same MSISDN (%s) but " +
                        "different MCTS ID (%s != %s)", LogHelper.obscure(msisdn), mctsSwcId, swc.getSwcId()));
            } else if (swc == null) {
                swc = swcService.getById(id);
            }
        }

        return swc;
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
        mapping.put(SwcConstants.STATE_ID, new Optional(new GetLong()));
        mapping.put(SwcConstants.STATE_NAME, new Optional(new GetString()));

        mapping.put(SwcConstants.DISTRICT_ID, new Optional(new GetLong()));
        mapping.put(SwcConstants.DISTRICT_NAME, new Optional(new GetString()));

        mapping.put(SwcConstants.BLOCK_ID, new Optional(new GetString()));
        mapping.put(SwcConstants.BLOCK_NAME, new Optional(new GetString()));

        mapping.put(SwcConstants.PANCHAYAT_ID, new Optional(new GetLong()));
        mapping.put(SwcConstants.PANCHAYAT_NAME, new Optional(new GetString()));

        mapping.put(SwcConstants.GF_AGE, new Optional(new GetLong()));
        mapping.put(SwcConstants.GF_SEX, new Optional(new GetString()));
    }


    private Map<String, CellProcessor> getRchProcessorMapping() {
        Map<String, CellProcessor> mapping = new HashMap<>();
        mapping.put(SwcConstants.ID, new GetString());
        mapping.put(SwcConstants.MOBILE_NO, new GetLong());
        mapping.put(SwcConstants.NAME, new GetString());
        getMapping(mapping);
        mapping.put(SwcConstants.EXEC_DATE, new Optional(new GetLocalDate()));
        return mapping;
    }

    private static SwcRecord convertMapToRchAsha(Map<String, Object> record) { //NO CHECKSTYLE CyclomaticComplexity
        SwcRecord rchAnmAshaRecord = new SwcRecord();
        rchAnmAshaRecord.setStateId(record.get(SwcConstants.STATE_ID) == null ? null : (Long) record.get(SwcConstants.STATE_ID));
        rchAnmAshaRecord.setStateName(record.get(SwcConstants.STATE_NAME) == null ? null : (String) record.get(SwcConstants.STATE_NAME));
        rchAnmAshaRecord.setDistrictId(record.get(SwcConstants.DISTRICT_ID) == null ? null : (Long) record.get(SwcConstants.DISTRICT_ID));
        rchAnmAshaRecord.setDistrictName(record.get(SwcConstants.DISTRICT_NAME) == null ? null : (String) record.get(SwcConstants.DISTRICT_NAME));

        rchAnmAshaRecord.setBlockId(record.get(SwcConstants.BLOCK_ID) == null ? null : (Long) record.get(SwcConstants.BLOCK_ID));
        rchAnmAshaRecord.setBlockName(record.get(SwcConstants.BLOCK_NAME) == null ? null : (String) record.get(SwcConstants.BLOCK_NAME));

        rchAnmAshaRecord.setPanchayatId(record.get(SwcConstants.PANCHAYAT_ID) == null ? null : (Long) record.get(SwcConstants.PANCHAYAT_ID));
        rchAnmAshaRecord.setPanchayatName(record.get(SwcConstants.PANCHAYAT_NAME) == null ? null : (String) record.get(SwcConstants.PANCHAYAT_NAME));
        rchAnmAshaRecord.setGfId(record.get(SwcConstants.ID) == null ? null : (Long) record.get(SwcConstants.ID));
        rchAnmAshaRecord.setMobileNo(record.get(SwcConstants.MOBILE_NO) == null ? null : (String) record.get(SwcConstants.MOBILE_NO));
        rchAnmAshaRecord.setGfName(record.get(SwcConstants.GF_NAME) == null ? null : (String) record.get(SwcConstants.GF_NAME));
        rchAnmAshaRecord.setGfAge(record.get(SwcConstants.GF_AGE) == null ? null : (long) record.get(SwcConstants.GF_AGE));
        rchAnmAshaRecord.setGfSex(record.get(SwcConstants.GF_SEX) == null ? null : (String) record.get(SwcConstants.GF_SEX));
        rchAnmAshaRecord.setExecDate(record.get(SwcConstants.EXEC_DATE) == null ? null : (String) record.get(SwcConstants.EXEC_DATE));
        return rchAnmAshaRecord;
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

//    private String swcActionFinder(AnmAshaRecord record) {
//        if (swcService.getByMctsSwcIdAndState(record.getId().toString(), stateDataService.findByCode(record.getStateId())) == null) {
//            return "CREATE";
//        } else {
//            return "UPDATE";
//        }
//    }


    private String rchSwcActionFinder(SwcRecord record) {
        if (swcService.getBySwcIdAndPanchayat(record.getGfId().toString(), panchayatDataService.findByCode(record.getPanchayatId())) == null) {
            return "CREATE";
        } else {
            return "UPDATE";
        }
    }
}
