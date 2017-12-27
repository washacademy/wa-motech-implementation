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

import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.domain.Panchayat;
import org.motechproject.nms.swc.domain.Swachchagrahi;
import org.motechproject.nms.swc.domain.ContactNumberAudit;
import org.motechproject.nms.swc.domain.SwcJobStatus;
import org.motechproject.nms.swc.domain.SwcError;
import org.motechproject.nms.swc.domain.SwachchagrahiStatus;
import org.motechproject.nms.swc.domain.SwcErrorReason;
import org.motechproject.nms.swc.exception.SwcExistingRecordException;
import org.motechproject.nms.swc.exception.SwcImportException;
import org.motechproject.nms.swc.repository.ContactNumberAuditDataService;
import org.motechproject.nms.swc.repository.SwcErrorDataService;
import org.motechproject.nms.swc.domain.RejectionReasons;
import org.motechproject.nms.swc.service.SwcService;
import org.motechproject.nms.swcUpdate.contract.SwcRecord;
import org.motechproject.nms.swcUpdate.service.SwcImportService;
import org.motechproject.nms.swc.utils.SwcConstants;
import org.motechproject.nms.swc.utils.SwcMapper;
import org.motechproject.nms.swc.domain.SubscriptionOrigin;
import org.motechproject.nms.swcUpdate.utils.RejectedObjectConverter;
import org.motechproject.nms.washacademy.service.WashAcademyService;
import org.motechproject.nms.props.service.LogHelper;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.exception.InvalidLocationException;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.repository.PanchayatDataService;
import org.motechproject.nms.region.service.LocationService;
import org.motechproject.nms.rejectionhandler.service.SwcRejectionService;
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
    private SwcRejectionService flwRejectionService;

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
        String designation;
            csvImporter = new CsvImporterBuilder()
                    .setProcessorMapping(getRchProcessorMapping())
                    .setPreferences(CsvPreference.TAB_PREFERENCE)
                    .createAndOpen(bufferedReader);
            try {
                Map<String, Object> record;
                while (null != (record = csvImporter.read())) {
                    designation = (String) record.get(SwcConstants.GF_TYPE);
                    designation = (designation != null) ? designation.trim() : designation;
                    if (SwcConstants.ASHA_TYPE.equalsIgnoreCase(designation)) {
                        importRchFrontLineWorker(record, panchayat);
                    }
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
    public void importRchFrontLineWorker(Map<String, Object> record, Panchayat state) throws InvalidLocationException, SwcExistingRecordException {
        String flwId = (String) record.get(SwcConstants.GF_ID);
        Long msisdn = (Long) record.get(SwcConstants.MOBILE_NO);

        record.put(SwcConstants.PANCHAYAT_ID, state.getPanchayatCode());
        Map<String, Object> location = locationService.getLocations(record);

        Swachchagrahi flw = swcService.getByMctsFlwIdAndPanchayat(flwId, state);
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
                    swcErrorDataService.create(new SwcError(flwId, (long) record.get(SwcConstants.STATE_ID), (long) record.get(SwcConstants.DISTRICT_ID), SwcErrorReason.PHONE_NUMBER_IN_USE));
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
                    swcErrorDataService.create(new SwcError(flwId, (long) record.get(SwcConstants.STATE_ID), (long) record.get(SwcConstants.DISTRICT_ID), SwcErrorReason.PHONE_NUMBER_IN_USE));
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

        long stateId = (long) flw.get(SwcConstants.STATE_ID);
        long districtId = (long) flw.get(SwcConstants.DISTRICT_ID);
        String blockId = (String) flw.get(SwcConstants.BLOCK_ID);
        long panchayatId = (long) flw.get(SwcConstants.PANCHAYAT_ID);
        String flwId = importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT) ? flw.get(SwcConstants.ID).toString() : flw.get(SwcConstants.GF_ID).toString();
        long contactNumber = importOrigin.equals(SubscriptionOrigin.MCTS_IMPORT) ? (long) flw.get(SwcConstants.CONTACT_NO) : (long) flw.get(SwcConstants.MOBILE_NO);
        String action = "";

        State state = locationService.getState(stateId);
        if (state == null) {
            swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_STATE));
//                action = this.flwActionFinder(convertMapToAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            return false;
        }
        District district = locationService.getDistrict(stateId, districtId);
        if (district == null) {
            swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_DISTRICT));
                action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));

            return false;
        }
        Block block = locationService.getBlock(stateId, districtId, blockId);
        if (block == null) {
            swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_DISTRICT));
            action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
            flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));

            return false;
        }
        Panchayat panchayat = locationService.getPanchayat(stateId, districtId, blockId, panchayatId, panchayatId);
        if (panchayat == null) {
            swcErrorDataService.create(new SwcError(flwId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_DISTRICT));
            action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
            flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));

            return false;
        }

        Swachchagrahi existingFlwByNumber = swcService.getByContactNumber(contactNumber);
        Swachchagrahi existingFlwByFlwId = swcService.getByMctsFlwIdAndPanchayat(flwId, panchayat);
        Map<String, Object> location = new HashMap<>();
        try {
            location = locationService.getLocations(flw, false);

                action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
                if (existingFlwByFlwId != null && existingFlwByNumber != null) {

                    if (existingFlwByFlwId.getSwcId().equalsIgnoreCase(existingFlwByNumber.getSwcId()) &&
                            existingFlwByFlwId.getState().equals(existingFlwByNumber.getState())) {
                        // we are trying to update the same existing flw. set fields and update
                        LOGGER.debug("Updating existing user with same phone number");
                        swcService.update(SwcMapper.updateSwc(existingFlwByFlwId, flw, location, SubscriptionOrigin.RCH_IMPORT));
                        flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), true, null, action));
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
                        flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
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
                    flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), true, null, action));
                    return true;
                } else if (existingFlwByFlwId == null && existingFlwByNumber != null) {

                    if (existingFlwByNumber.getSwcId() == null) {
                        // we just got data from rch for a previous anonymous user that subscribed by phone number
                        // merging those records
                        LOGGER.debug("Merging rch data with previously anonymous user");
                        swcService.update(SwcMapper.updateSwc(existingFlwByNumber, flw, location, SubscriptionOrigin.RCH_IMPORT));
                        flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), true, null, action));
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
                        flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                        return false;
                    }

                } else { // existingFlwByMctsFlwId & existingFlwByNumber are null)
                    // new user. set fields and add
                    LOGGER.debug("Adding new RCH flw user");
                    Swachchagrahi swachchagrahi = SwcMapper.createRchSwc(flw, location);
                    if (swachchagrahi != null) {
                        swcService.add(swachchagrahi);
                        flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), true, null, action));
                        return true;
                    } else {
                        LOGGER.error("GF Status is INACTIVE. So cannot create record.");
                        flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), false, RejectionReasons.GF_STATUS_INACTIVE.toString(), action));
                        return false;
                    }
                }


        } catch (InvalidLocationException ile) {
            LOGGER.debug(ile.toString());
                action = this.rchFlwActionFinder(convertMapToRchAsha(flw));
                flwRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(flw), false, RejectionReasons.INVALID_LOCATION.toString(), action));
            return false;
        }
    }

    private void updateFlwMaMsisdn(Swachchagrahi flwInstance, Long existingMsisdn, Long newMsisdn) {
        swcService.update(flwInstance);
        washAcademyService.updateMsisdn(flwInstance.getId(), existingMsisdn, newMsisdn);
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

    private Swachchagrahi flwFromRecord(Map<String, Object> record, Panchayat state) { //NO CHECKSTYLE CyclomaticComplexity
        Swachchagrahi flw = null;

        String mctsFlwId = (String) record.get(SwcConstants.ID);
        Long msisdn = (Long) record.get(SwcConstants.CONTACT_NO);

        if (mctsFlwId != null) {
            flw = swcService.getByMctsFlwIdAndPanchayat(mctsFlwId, state);
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
        mapping.put(SwcConstants.STATE_ID, new Optional(new GetLong()));

        mapping.put(SwcConstants.DISTRICT_ID, new Optional(new GetLong()));
        mapping.put(SwcConstants.DISTRICT_NAME, new Optional(new GetString()));

        mapping.put(SwcConstants.BLOCK_ID, new Optional(new GetString()));
        mapping.put(SwcConstants.BLOCK_NAME, new Optional(new GetString()));

        mapping.put(SwcConstants.PANCHAYAT_ID, new Optional(new GetLong()));
        mapping.put(SwcConstants.PANCHAYAT_NAME, new Optional(new GetString()));
    }

    private Map<String, CellProcessor> getMctsProcessorMapping() {
        Map<String, CellProcessor> mapping = new HashMap<>();
        mapping.put(SwcConstants.ID, new GetString());
        mapping.put(SwcConstants.CONTACT_NO, new GetLong());
        mapping.put(SwcConstants.NAME, new GetString());
        getMapping(mapping);
        mapping.put(SwcConstants.TYPE, new Optional(new GetString()));
        mapping.put(SwcConstants.GF_STATUS, new Optional(new GetString()));
        mapping.put(SwcConstants.UPDATED_ON, new Optional(new GetLocalDate()));

        mapping.put("Reg_Date", new Optional(new GetString()));
        mapping.put("Sex", new Optional(new GetString()));
        mapping.put("SMS_Reply", new Optional(new GetString()));
        mapping.put(SwcConstants.AADHAR_NO, new Optional(new GetInteger()));
        mapping.put("Created_On", new Optional(new GetString()));
        mapping.put("Updated_On", new Optional(new GetString()));
        mapping.put(SwcConstants.BANK_ID, new Optional(new GetInteger()));
        mapping.put("Branch_Name", new Optional(new GetString()));
        mapping.put("IFSC_ID_Code", new Optional(new GetString()));
        mapping.put("Bank_Name", new Optional(new GetString()));
        mapping.put("Acc_No", new Optional(new GetString()));
        mapping.put("Is_Aadhar_linked", new Optional(new GetBoolean()));
        mapping.put("Verify_Date", new Optional(new GetString()));
        mapping.put("Verifier_Name", new Optional(new GetString()));
        mapping.put(SwcConstants.VERIFIER_ID, new Optional(new GetInteger()));
        mapping.put("Call_Ans", new Optional(new GetBoolean()));
        mapping.put("IsPhoneNoCorrect", new Optional(new GetBoolean()));
        mapping.put(SwcConstants.NOCALLREASON, new Optional(new GetInteger()));
        mapping.put(SwcConstants.NOPHONEREASON, new Optional(new GetInteger()));
        mapping.put("Verifier_Remarks", new Optional(new GetString()));
        mapping.put("GF_Address", new Optional(new GetString()));
        mapping.put("Husband_Name", new Optional(new GetString()));

        return mapping;
    }

    private Map<String, CellProcessor> getRchProcessorMapping() {
        Map<String, CellProcessor> mapping = new HashMap<>();
        mapping.put(SwcConstants.GF_ID, new GetString());
        mapping.put(SwcConstants.MOBILE_NO, new GetLong());
        mapping.put(SwcConstants.GF_NAME, new GetString());
        getMapping(mapping);
        mapping.put(SwcConstants.GF_TYPE, new Optional(new GetString()));
        mapping.put(SwcConstants.EXEC_DATE, new Optional(new GetLocalDate()));
        mapping.put(SwcConstants.GF_STATUS, new Optional(new GetString()));

        return mapping;
    }

    private static SwcRecord convertMapToRchAsha(Map<String, Object> record) { //NO CHECKSTYLE CyclomaticComplexity
        SwcRecord rchAnmAshaRecord = new SwcRecord();
        rchAnmAshaRecord.setStateId(record.get(SwcConstants.STATE_ID) == null ? null : (Long) record.get(SwcConstants.STATE_ID));
        rchAnmAshaRecord.setDistrictId(record.get(SwcConstants.DISTRICT_ID) == null ? null : (Long) record.get(SwcConstants.DISTRICT_ID));
        rchAnmAshaRecord.setDistrictName(record.get(SwcConstants.DISTRICT_NAME) == null ? null : (String) record.get(SwcConstants.DISTRICT_NAME));

        rchAnmAshaRecord.setBlockId(record.get(SwcConstants.BLOCK_ID) == null ? null : (Long) record.get(SwcConstants.BLOCK_ID));
        rchAnmAshaRecord.setBlockName(record.get(SwcConstants.BLOCK_NAME) == null ? null : (String) record.get(SwcConstants.BLOCK_NAME));

        rchAnmAshaRecord.setPanchayatId(record.get(SwcConstants.PANCHAYAT_ID) == null ? null : (Long) record.get(SwcConstants.PANCHAYAT_ID));
        rchAnmAshaRecord.setPanchayatName(record.get(SwcConstants.PANCHAYAT_NAME) == null ? null : (String) record.get(SwcConstants.PANCHAYAT_NAME));
        rchAnmAshaRecord.setGfId(record.get(SwcConstants.GF_ID) == null ? null : (Long) record.get(SwcConstants.GF_ID));
        rchAnmAshaRecord.setMobileNo(record.get(SwcConstants.MOBILE_NO) == null ? null : (String) record.get(SwcConstants.MOBILE_NO));
        rchAnmAshaRecord.setGfName(record.get(SwcConstants.GF_NAME) == null ? null : (String) record.get(SwcConstants.GF_NAME));
        rchAnmAshaRecord.setGfType(record.get(SwcConstants.GF_TYPE) == null ? null : (String) record.get(SwcConstants.GF_TYPE));
        rchAnmAshaRecord.setExecDate(record.get(SwcConstants.EXEC_DATE) == null ? null : (String) record.get(SwcConstants.EXEC_DATE));
        rchAnmAshaRecord.setGfStatus(record.get(SwcConstants.GF_STATUS) == null ? null : (String) record.get(SwcConstants.GF_STATUS));
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

//    private String flwActionFinder(AnmAshaRecord record) {
//        if (swcService.getByMctsFlwIdAndState(record.getId().toString(), stateDataService.findByCode(record.getStateId())) == null) {
//            return "CREATE";
//        } else {
//            return "UPDATE";
//        }
//    }

    private String rchFlwActionFinder(SwcRecord record) {
        if (swcService.getByMctsFlwIdAndPanchayat(record.getGfId().toString(), panchayatDataService.findByCode(record.getPanchayatId())) == null) {
            return "CREATE";
        } else {
            return "UPDATE";
        }
    }
}
