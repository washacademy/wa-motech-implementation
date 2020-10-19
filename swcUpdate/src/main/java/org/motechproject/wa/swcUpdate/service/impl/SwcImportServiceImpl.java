package org.motechproject.wa.swcUpdate.service.impl;

import org.apache.commons.lang.StringUtils;
import org.motechproject.wa.csv.exception.CsvImportDataException;
import org.motechproject.wa.csv.utils.*;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.exception.InvalidLocationException;
import org.motechproject.wa.region.repository.PanchayatDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.LocationService;
import org.motechproject.wa.rejectionhandler.service.SwcRejectionService;
import org.motechproject.wa.swc.domain.*;
import org.motechproject.wa.swc.exception.SwcExistingRecordException;
import org.motechproject.wa.swc.exception.SwcImportException;
import org.motechproject.wa.swc.repository.ContactNumberAuditDataService;
import org.motechproject.wa.swc.repository.SwcErrorDataService;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.swc.utils.SwcConstants;
import org.motechproject.wa.swc.utils.SwcMapper;
import org.motechproject.wa.swcUpdate.contract.SwcRecord;
import org.motechproject.wa.swcUpdate.service.SwcImportService;
import org.motechproject.wa.swcUpdate.utils.RejectedObjectConverter;
import org.motechproject.wa.washacademy.service.WashAcademyService;
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

        State state = importHeader(bufferedReader);

        CsvMapImporter csvImporter;
            csvImporter = new CsvImporterBuilder()
                    .setProcessorMapping(getRchProcessorMapping())
                    .setPreferences(CsvPreference.TAB_PREFERENCE)
                    .createAndOpen(bufferedReader);
            try {
                Map<String, Object> record;
                while (null != (record = csvImporter.read())) {
                    LOGGER.info("state {}", record.toString());
                        importRchFrontLineWorker(record, panchayatDataService.findByCode((Long)record.get(SwcConstants.PANCHAYAT_ID)));

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
        int courseId = (int) record.get(SwcConstants.COURSE_ID);
        LOGGER.info("panchayat {}", panchayat);
        record.put(SwcConstants.PANCHAYAT_ID, panchayat.getPanchayatCode());
        Map<String, Object> location = locationService.getLocations(record);

        Swachchagrahi swc = swcService.getBySwcIdAndPanchayatAndCourseId(swcId, panchayat, courseId);
        if (swc != null) {
            Swachchagrahi swc2 = swcService.getByContactNumberAndCourseId(msisdn,courseId);
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
            Swachchagrahi swachchagrahi = swcService.getByContactNumberAndCourseId(msisdn,courseId);
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
        long blockId = (long) swc.get(SwcConstants.BLOCK_ID);
        long panchayatId = (long) swc.get(SwcConstants.PANCHAYAT_ID);
        String swcId = swc.get(SwcConstants.ID).toString();
        long contactNumber = (long) swc.get(SwcConstants.MOBILE_NO);
        int courseId = (int)swc.get(SwcConstants.COURSE_ID);

        String rejectionAction = "";
        rejectionAction = this.rejectionSwcActionFinder(convertMapToRchAsha(swc));

        State state = locationService.getState(stateId);
        LOGGER.info("fetching state");
        if (state == null) {
            LOGGER.info("state not present");
            swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_STATE));
//                action = this.swcActionFinder(convertMapToAsha(swc));
                swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), rejectionAction));
            return false;
        }
        LOGGER.info("state valid, fetching district");
        District district = locationService.getDistrict(stateId, districtId);
        LOGGER.info("district fetched");
        if (district == null) {
            swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_DISTRICT));
                swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), rejectionAction));

            return false;
        }
        LOGGER.info("fetching block");
        Block block = locationService.getBlock(stateId, districtId, blockId);
//        if (block == null) {
//            swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_BLOCK));
//            swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), rejectionAction));
//
//            return false;
//        }
        Panchayat panchayat = locationService.getPanchayat(stateId, districtId, blockId, panchayatId, panchayatId);
//        if (panchayat == null) {
//            swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.INVALID_LOCATION_PANCHAYAT));
//            swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), rejectionAction));
//
//            return false;
//        }
        LOGGER.info("fetching swc by contact number and courseId");
        Swachchagrahi existingSwcByNumberAndCourseId = swcService.getByContactNumberAndCourseId(contactNumber,courseId);
        if(existingSwcByNumberAndCourseId==null) LOGGER.info("failed to fetch swc by contact number and courseId");
//        LOGGER.info(existingSwcByNumber.toString());
        Swachchagrahi existingSwcBySwcId = swcService.getBySwcIdAndPanchayatAndCourseId(swcId, panchayat,courseId);
        Map<String, Object> location = new HashMap<>();
        try {
            location = locationService.getLocations(swc, true);

                if (existingSwcBySwcId != null && existingSwcByNumberAndCourseId != null) {

                    if (existingSwcBySwcId.getSwcId().equalsIgnoreCase(existingSwcByNumberAndCourseId.getSwcId()) &&
                            existingSwcBySwcId.getPanchayat().equals(existingSwcByNumberAndCourseId.getPanchayat())) {
                        // we are trying to update the same existing swc. set fields and update
                        LOGGER.debug("Updating existing user with same phone number");
                        swcService.update(SwcMapper.updateSwc(existingSwcBySwcId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, rejectionAction));
                        return true;
                    } else if ((!existingSwcBySwcId.getSwcId().equalsIgnoreCase(existingSwcByNumberAndCourseId.getSwcId()) ||
                            !existingSwcBySwcId.getPanchayat().equals(existingSwcByNumberAndCourseId.getPanchayat())) &&
                            existingSwcByNumberAndCourseId.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                        LOGGER.debug("Updating existing user with same phone number");
                        swcService.update(SwcMapper.updateSwc(existingSwcBySwcId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                        return true;
                    } else {
                        // we are trying to update 2 different users and/or phone number used by someone else
                        LOGGER.debug("Existing swc but phone number(update) already in use");
                        swcErrorDataService.create(new SwcError(swcId, stateId, districtId, SwcErrorReason.PHONE_NUMBER_IN_USE));
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), rejectionAction));
                        return false;
                    }
                } else if (existingSwcBySwcId != null && existingSwcByNumberAndCourseId == null) {
                    Swachchagrahi existingSwcByNumberAndJobStatus = swcService.getInctiveByContactNumberAndCourseId(contactNumber,courseId);
                    if(existingSwcByNumberAndJobStatus!=null) {
                        LOGGER.info("job status inactive");
                        swcService.update(SwcMapper.updateSwc(existingSwcBySwcId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                        LOGGER.info("updated existed swc job status to active");
                        return true;

                    } else {
                        // trying to update the phone number of the person. possible migration scenario
                        // making design decision that swc will lose all progress when phone number is changed. Usage and tracking is not
                        // worth the effort & we don't really know that its the same swc
                        LOGGER.debug("Updating phone number for swc");
                        long existingContactNumber = existingSwcBySwcId.getContactNumber();
                        Swachchagrahi swcInstance = SwcMapper.updateSwc(existingSwcBySwcId, swc, location, SubscriptionOrigin.RCH_IMPORT);
                        updateSwcMaMsisdn(swcInstance, existingContactNumber, contactNumber);
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, rejectionAction));
                        return true;
                    }
                } else if (existingSwcBySwcId == null && existingSwcByNumberAndCourseId != null) {

                    if (existingSwcByNumberAndCourseId.getSwcId() == null) {
                        // we just got data from rch for a previous anonymous user that subscribed by phone number
                        // merging those records
                        LOGGER.debug("Merging rch data with previously anonymous user");
                        swcService.update(SwcMapper.updateSwc(existingSwcByNumberAndCourseId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, rejectionAction));
                        return true;
                    } else if (existingSwcByNumberAndCourseId.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
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
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), rejectionAction));
                        return false;
                    }

                } else { // existingSwcByMctsSwcId & existingSwcByNumber are null)
                    // new user. set fields and add
                    LOGGER.debug("Adding new RCH swc user");
                    Swachchagrahi swachchagrahi = SwcMapper.createRchSwc(swc, location);
                    if (swachchagrahi != null) {
                        swcService.add(swachchagrahi);
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, rejectionAction));
                        return true;
                    } else {
                        LOGGER.error("Job Status is INACTIVE. So cannot create record.");
                        swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.GF_STATUS_INACTIVE.toString(), rejectionAction));
                        return false;
                    }
                }


        } catch (InvalidLocationException ile) {
            LOGGER.debug(ile.toString());
                swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), rejectionAction));
            return false;
        }
    }

    @Override
    public boolean createCsvUpdate(Map<String, Object> swc, SubscriptionOrigin importOrigin,
                                   HashMap<String,State> sMap, HashMap<String, District> dMap,
                                   HashMap<String, Block> bMap, HashMap<String,Panchayat> pMap) { //NOPMD NcssMethodCount

        String stateId = swc.get(SwcConstants.STATE_ID).toString();
        String districtId = swc.get(SwcConstants.DISTRICT_ID).toString();
        String blockId = swc.get(SwcConstants.BLOCK_ID).toString();
        String panchayatId = swc.get(SwcConstants.PANCHAYAT_ID).toString();
        String swcId = swc.get(SwcConstants.ID).toString();
        long contactNumber = (long) swc.get(SwcConstants.MOBILE_NO);
        int courseId = (int) swc.get(SwcConstants.COURSE_ID);

        String rejectionAction = "";
        rejectionAction = this.rejectionSwcActionFinder(convertMapToRchAsha(swc));

        State state = sMap.get(stateId);
//        LOGGER.info("fetching state");
        if (state == null) {
//            LOGGER.info("state not present");
            swcErrorDataService.create(new SwcError(swcId, Long.parseLong(stateId), Long.parseLong(districtId), SwcErrorReason.INVALID_LOCATION_STATE));
//                action = this.swcActionFinder(convertMapToAsha(swc));
            swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), rejectionAction));
            return false;
        }
//        LOGGER.info("state valid, fetching district");
        District district = dMap.get(stateId+districtId);
        if (district == null) {
            swcErrorDataService.create(new SwcError(swcId,Long.parseLong(stateId), Long.parseLong(districtId), SwcErrorReason.INVALID_LOCATION_DISTRICT));
            swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), rejectionAction));

            return false;
        }
//        LOGGER.info("district fetched");
//        LOGGER.info("fetching block");
        Block block = bMap.get(stateId+districtId+blockId);
        Panchayat panchayat =null;
        Swachchagrahi existingSwcBySwcIdAndCourseId = null;
        if (block == null) {
            block = locationService.createBlock(district,Long.parseLong(blockId),swc.get(SwcConstants.BLOCK_NAME).toString());
            panchayat = locationService.createPanchayat(block,Long.parseLong(panchayatId),swc.get(SwcConstants.PANCHAYAT_NAME).toString());
        } else {
            panchayat = pMap.get(stateId + districtId + blockId + panchayatId);
        }

        if (panchayat!=null) {
            existingSwcBySwcIdAndCourseId = swcService.getBySwcIdAndPanchayatAndCourseId(swcId, panchayat, courseId);
        }else {
            panchayat = locationService.createPanchayat(block,Long.parseLong(panchayatId),swc.get(SwcConstants.PANCHAYAT_NAME).toString());
        }
//        LOGGER.info("fetching swc by contact number");
        Swachchagrahi existingSwcByNumber = swcService.getByContactNumberAndCourseId(contactNumber,courseId);
        if(existingSwcByNumber==null) LOGGER.info("failed to fetch swc by contact number");
        bMap.put(stateId+districtId+blockId,block);
        pMap.put(stateId+districtId+blockId+panchayatId,panchayat);
//        LOGGER.info("blockMap:"+ bMap.toString());
//        LOGGER.info("panchayatMap:"+ pMap.toString());
        Map<String, Object> location = new HashMap<>();
        location.put("StateID",state);
        location.put("District_ID",district);
        location.put("Block_ID",block);
        location.put("Panchayat_ID",panchayat);
        try {

            if (existingSwcBySwcIdAndCourseId != null && existingSwcByNumber != null) {

                if (existingSwcBySwcIdAndCourseId.getSwcId().equalsIgnoreCase(existingSwcByNumber.getSwcId()) &&
                        existingSwcBySwcIdAndCourseId.getPanchayat().equals(existingSwcByNumber.getPanchayat())) {
                    // we are trying to update the same existing swc. set fields and update
                    LOGGER.debug("Updating existing user with same phone number");
                    swcService.update(SwcMapper.updateSwc(existingSwcBySwcIdAndCourseId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                    swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, rejectionAction));
                    return true;
                } else if ((!existingSwcBySwcIdAndCourseId.getSwcId().equalsIgnoreCase(existingSwcByNumber.getSwcId()) ||
                        !existingSwcBySwcIdAndCourseId.getPanchayat().equals(existingSwcByNumber.getPanchayat())) &&
                        existingSwcByNumber.getJobStatus().equals(SwcJobStatus.INACTIVE)) {
                    LOGGER.debug("Updating existing user with same phone number");
                    swcService.update(SwcMapper.updateSwc(existingSwcBySwcIdAndCourseId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                    return true;
                } else {
                    // we are trying to update 2 different users and/or phone number used by someone else
                    LOGGER.debug("Existing swc but phone number(update) already in use");
                    swcErrorDataService.create(new SwcError(swcId, Long.parseLong(stateId), Long.parseLong(districtId), SwcErrorReason.PHONE_NUMBER_IN_USE));
                    swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), rejectionAction));
                    return false;
                }
            } else if (existingSwcBySwcIdAndCourseId != null && existingSwcByNumber == null) {
                Swachchagrahi existingSwcByNumberAndJobStatus = swcService.getInctiveByContactNumberAndCourseId(contactNumber,courseId);
                if(existingSwcByNumberAndJobStatus!=null) {
                    LOGGER.info("job status inactive");
                    swcService.update(SwcMapper.updateSwc(existingSwcBySwcIdAndCourseId, swc, location, SubscriptionOrigin.RCH_IMPORT));
                    LOGGER.info("updated existed swc job status to active");
                    return true;

                } else {
                    // trying to update the phone number of the person. possible migration scenario
                    // making design decision that swc will lose all progress when phone number is changed. Usage and tracking is not
                    // worth the effort & we don't really know that its the same swc
                    LOGGER.debug("Updating phone number for swc");
                    long existingContactNumber = existingSwcBySwcIdAndCourseId.getContactNumber();
                    Swachchagrahi swcInstance = SwcMapper.updateSwc(existingSwcBySwcIdAndCourseId, swc, location, SubscriptionOrigin.RCH_IMPORT);
                    updateSwcMaMsisdn(swcInstance, existingContactNumber, contactNumber);
                    swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, rejectionAction));
                    return true;
                }
            } else if (existingSwcBySwcIdAndCourseId == null && existingSwcByNumber != null) {

                if (existingSwcByNumber.getSwcId() == null) {
                    // we just got data from rch for a previous anonymous user that subscribed by phone number
                    // merging those records
                    LOGGER.debug("Merging rch data with previously anonymous user");
                    swcService.update(SwcMapper.updateSwc(existingSwcByNumber, swc, location, SubscriptionOrigin.RCH_IMPORT));
                    swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, rejectionAction));
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
                    swcErrorDataService.create(new SwcError(swcId, Long.parseLong(stateId), Long.parseLong(districtId), SwcErrorReason.PHONE_NUMBER_IN_USE));
                    swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), rejectionAction));
                    return false;
                }

            } else { // existingSwcByMctsSwcId & existingSwcByNumber are null)
                // new user. set fields and add
                LOGGER.debug("Adding new RCH swc user");
                Swachchagrahi swachchagrahi = SwcMapper.createRchSwc(swc, location);
                if (swachchagrahi != null) {
                    swcService.add(swachchagrahi);
                    swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), true, null, rejectionAction));
                    return true;
                } else {
                    LOGGER.error("Job Status is INACTIVE. So cannot create record.");
                    swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.GF_STATUS_INACTIVE.toString(), rejectionAction));
                    return false;
                }
            }


        } catch (InvalidLocationException ile) {
            LOGGER.debug(ile.toString());
            swcRejectionService.createUpdate(RejectedObjectConverter.swcRejection(convertMapToRchAsha(swc), false, RejectionReasons.INVALID_LOCATION.toString(), rejectionAction));
            return false;
        }
    }

    private void updateSwcMaMsisdn(Swachchagrahi swcInstance, Long existingMsisdn, Long newMsisdn) {
        swcService.update(swcInstance);
        washAcademyService.updateMsisdn(swcInstance.getId(), existingMsisdn, newMsisdn);
    }

    private State importHeader(BufferedReader bufferedReader) throws IOException {
        String line = readLineWhileBlank(bufferedReader);
        // expect state name in the first line
        if (line.matches("^State Name : .*$")) {
            String stateName = line.substring(line.indexOf(':') + 1).trim();
            State state = stateDataService.findByName(stateName);
            verify(null != state, "state does not exists");
            readLineWhileNotBlank(bufferedReader);
            return state;
        } else {
            throw new IllegalArgumentException("Invalid file format");
        }
    }

    private Swachchagrahi swcFromRecord(Map<String, Object> record, Panchayat state) { //NO CHECKSTYLE CyclomaticComplexity
        Swachchagrahi swc = null;

        String mctsSwcId = (String) record.get(SwcConstants.ID);
        Long msisdn = (Long) record.get(SwcConstants.CONTACT_NO);
        int courseId = (int) record.get(SwcConstants.COURSE_ID);


        if (mctsSwcId != null) {
            swc = swcService.getBySwcIdAndPanchayatAndCourseId(mctsSwcId, state, courseId);
        }

        if (swc == null && msisdn != null) {
            swc = swcService.getByContactNumberAndCourseId(msisdn,courseId);

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
            swc = swcService.getByContactNumberAndCourseId(msisdn,courseId);

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

        mapping.put(SwcConstants.BLOCK_ID, new Optional(new GetLong()));
        mapping.put(SwcConstants.BLOCK_NAME, new Optional(new GetString()));

        mapping.put(SwcConstants.PANCHAYAT_ID, new Optional(new GetLong()));
        mapping.put(SwcConstants.PANCHAYAT_NAME, new Optional(new GetString()));

        mapping.put(SwcConstants.SWC_AGE, new Optional(new GetLong()));
        mapping.put(SwcConstants.SWC_SEX, new Optional(new GetString()));
    }


    private Map<String, CellProcessor> getRchProcessorMapping() {
        Map<String, CellProcessor> mapping = new HashMap<>();
        mapping.put(SwcConstants.ID, new GetString());
        mapping.put(SwcConstants.MOBILE_NO, new GetLong());
        mapping.put(SwcConstants.NAME, new GetString());
        getMapping(mapping);
        mapping.put(SwcConstants.EXEC_DATE, new Optional(new GetLocalDate()));
        mapping.put(SwcConstants.COURSE_ID,new GetString());
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
        rchAnmAshaRecord.setGfId(record.get(SwcConstants.ID) == null ? null : Long.parseLong(record.get(SwcConstants.ID).toString()));
        rchAnmAshaRecord.setMobileNo(record.get(SwcConstants.MOBILE_NO) == null ? null : record.get(SwcConstants.MOBILE_NO).toString());
        rchAnmAshaRecord.setGfName(record.get(SwcConstants.NAME) == null ? null : (String) record.get(SwcConstants.NAME));
        rchAnmAshaRecord.setGfAge(record.get(SwcConstants.SWC_AGE) == null ? null : (long) record.get(SwcConstants.SWC_AGE));
        rchAnmAshaRecord.setGfSex(record.get(SwcConstants.SWC_SEX) == null ? null : (String) record.get(SwcConstants.SWC_SEX));
        rchAnmAshaRecord.setExecDate(record.get(SwcConstants.EXEC_DATE) == null ? null : (String) record.get(SwcConstants.EXEC_DATE));
        rchAnmAshaRecord.setType(record.get(SwcConstants.TYPE) == null ? null : (String) record.get(SwcConstants.TYPE));
        rchAnmAshaRecord.setJobStatus(record.get(SwcConstants.JOB_STATUS) == null ? null : (String) record.get(SwcConstants.JOB_STATUS));
        rchAnmAshaRecord.setCourseId(record.get(SwcConstants.COURSE_ID) == null ? null : (Integer) record.get(SwcConstants.COURSE_ID));

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
    public void setPanchayatDataService(PanchayatDataService panchayatDataService) {
        this.panchayatDataService = panchayatDataService;
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
    private String rejectionSwcActionFinder(SwcRecord record) {
        Long swcId = record.getGfId().toString() == null ? null : Long.parseLong(record.getGfId().toString());
        if (swcRejectionService.findBySwcIdAndPanchayatIdAndCourseId(swcId, record.getPanchayatId(), record.getCourseId()) == null) {
            return "CREATE";
        } else {
            return "UPDATE";
        }
    }


    private String rchSwcActionFinder(SwcRecord record) {
        if (swcService.getBySwcIdAndPanchayatAndCourseId(record.getGfId().toString(), panchayatDataService.findByCode(record.getPanchayatId()),record.getCourseId()) == null) {
            return "CREATE";
        } else {
            return "UPDATE";
        }
    }


}
