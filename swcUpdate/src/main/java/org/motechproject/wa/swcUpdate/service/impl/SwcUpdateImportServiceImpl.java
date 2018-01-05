package org.motechproject.wa.swcUpdate.service.impl;

import org.motechproject.wa.csv.exception.CsvImportDataException;
import org.motechproject.wa.csv.utils.ConstraintViolationUtils;
import org.motechproject.wa.csv.utils.CsvImporterBuilder;
import org.motechproject.wa.csv.utils.CsvMapImporter;
import org.motechproject.wa.csv.utils.GetInstanceByLong;
import org.motechproject.wa.csv.utils.GetInstanceByString;
import org.motechproject.wa.csv.utils.GetLong;
import org.motechproject.wa.csv.utils.GetString;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.swcUpdate.service.SwcUpdateImportService;
import org.motechproject.wa.washacademy.service.WashAcademyService;
import org.motechproject.wa.region.domain.Language;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.LanguageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service("swcUpdateImportService")
public class SwcUpdateImportServiceImpl implements SwcUpdateImportService {

    public static final String wa_SWC_ID = "wa SWC-ID";
    public static final String MCTS_SWC_ID = "MCTS SWC-ID";
    public static final String STATE = "STATE";
    public static final String MSISDN = "MSISDN";
    public static final String LANGUAGE_CODE = "LANGUAGE CODE";
    public static final String NEW_MSISDN = "NEW MSISDN";

    private SwcService swcService;
    private LanguageService languageService;
    private StateDataService stateDataService;
    private WashAcademyService washAcademyService;

    /*
        Expected file format:
        * First line contains headers: wa SWC-ID, MCTS SWC-ID, MSISDN, LANGUAGE CODE
        * CSV data (comma-separated)
     */
    @Override
    @Transactional
    public void importLanguageData(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);

        CsvMapImporter csvImporter = new CsvImporterBuilder().setProcessorMapping(getLanguageProcessorMapping())
                                                             .setPreferences(CsvPreference.STANDARD_PREFERENCE)
                                                             .createAndOpen(bufferedReader);
        try {
            Map<String, Object> record;
            while (null != (record = csvImporter.read())) {
                Language language = languageService.getForCode((String) record.get(LANGUAGE_CODE));
                if (language == null) {
                    throw new CsvImportDataException(createErrorMessage(String.format("Unable to locate language: %s(%s)",
                                    LANGUAGE_CODE, record.get(LANGUAGE_CODE)),
                            csvImporter.getRowNumber()));
                }

                Swachchagrahi swc = swcFromRecord(record);
                if (swc == null) {
                    throw new CsvImportDataException(createErrorMessage(String.format("Unable to locate SWC: %s(%s) %s(%s) %s(%s)",
                                    wa_SWC_ID, record.get(wa_SWC_ID),
                                    MCTS_SWC_ID, record.get(MCTS_SWC_ID),
                                    MSISDN, record.get(MSISDN)),
                                                                        csvImporter.getRowNumber()));
                }

                swcService.update(swc);
            }
        } catch (ConstraintViolationException e) {
            throw new CsvImportDataException(createErrorMessage(e.getConstraintViolations(), csvImporter.getRowNumber()), e);
        }
    }

    /*
        Expected file format:
        * First line contains headers: wa SWC-ID, MCTS SWC-ID, MSISDN, NEW MSISDN
        * CSV data (comma-separated)
     */
    @Override
    @Transactional
    public void importMSISDNData(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);

        CsvMapImporter csvImporter = new CsvImporterBuilder()
                .setProcessorMapping(getMSISDNProcessorMapping())
                .setPreferences(CsvPreference.STANDARD_PREFERENCE)
                .createAndOpen(bufferedReader);
        try {
            Map<String, Object> record;
            while (null != (record = csvImporter.read())) {
                Swachchagrahi swc = swcFromRecord(record);
                if (swc == null) {
                    throw new CsvImportDataException(createErrorMessage(String.format("Unable to locate SWC: %s(%s) %s(%s) %s(%s)",
                                    wa_SWC_ID, record.get(wa_SWC_ID),
                                    MCTS_SWC_ID, record.get(MCTS_SWC_ID),
                                    MSISDN, record.get(MSISDN)),
                            csvImporter.getRowNumber()));
                }

                Long msisdn = (Long) record.get(NEW_MSISDN);

                Swachchagrahi swcWithNewMSISDN = swcService.getByContactNumber(msisdn);

                if (swcWithNewMSISDN != null && swcWithNewMSISDN != swc) {
                    throw new CsvImportDataException(
                            createErrorMessage(String
                                            .format("Attempt to assign an msisdn when an existing SWC " +
                                                            "already has that number SWC in CSV: %s(%s) %s(%s) %s(%s) " +
                                                            "Existing SWC:  %s(%s) %s(%s) %s(%s)",
                                                    wa_SWC_ID, record.get(wa_SWC_ID),
                                                    MCTS_SWC_ID, record.get(MCTS_SWC_ID),
                                                    MSISDN, record.get(MSISDN),
                                                    wa_SWC_ID, swcWithNewMSISDN.getSwcId(),
                                                    MSISDN, swcWithNewMSISDN.getContactNumber()),
                                    csvImporter.getRowNumber()));
                }

                Long oldMsisdn = swc.getContactNumber();
                swc.setContactNumber(msisdn);
                swcService.update(swc);
                washAcademyService.updateMsisdn(swc.getId(), oldMsisdn, msisdn);
            }
        } catch (ConstraintViolationException e) {
            throw new CsvImportDataException(createErrorMessage(e.getConstraintViolations(), csvImporter.getRowNumber()), e);
        } catch (NumberFormatException e) {
            throw new CsvImportDataException(createErrorMessage("Invalid number", csvImporter.getRowNumber()), e);
        }
    }

    private Swachchagrahi swcFromRecord(Map<String, Object> record) {
        Swachchagrahi swc = null;

        String waFlWId = (String) record.get(wa_SWC_ID);
        String mctsSwcId = (String) record.get(MCTS_SWC_ID);
        Panchayat state = (Panchayat) record.get(STATE);
        Long msisdn = (Long) record.get(MSISDN);

        if (waFlWId != null) {
            swc = swcService.getBySwcId(waFlWId);
        }

        if (swc == null && mctsSwcId != null) {
            swc = swcService.getBySwcIdAndPanchayat(mctsSwcId, state);
        }

        if (swc == null && msisdn != null) {
            swc = swcService.getByContactNumber(msisdn);
        }

        return swc;
    }


    private Map<String, CellProcessor> getIDProcessorMapping() {
        Map<String, CellProcessor> mapping = new HashMap<>();
        mapping.put(wa_SWC_ID, new Optional(new GetString()));
        mapping.put(MCTS_SWC_ID, new Optional(new GetString()));
        mapping.put(MSISDN, new Optional(new GetLong()));
        mapping.put(STATE, new GetInstanceByLong<State>() {
            @Override
            public State retrieve(Long value) {
                return stateDataService.findByCode(value);
            }
        });

        return mapping;
    }

    private Map<String, CellProcessor> getLanguageProcessorMapping() {
        Map<String, CellProcessor> mapping = getIDProcessorMapping();

        mapping.put(LANGUAGE_CODE, new GetString());

        return mapping;
    }

    private Map<String, CellProcessor> getMSISDNProcessorMapping() {
        Map<String, CellProcessor> mapping = getIDProcessorMapping();

        mapping.put(NEW_MSISDN, new GetInstanceByString<Long>() {
            @Override
            public Long retrieve(String value) {
                if (value.length() < 10) {
                    throw new NumberFormatException(String.format("%s too short, must be at least 10 digits", NEW_MSISDN));
                }
                String msisdn = value.substring(value.length() - 10);

                return Long.parseLong(msisdn);
            }
        });

        return mapping;
    }

    private String createErrorMessage(String message, int rowNumber) {
        return String.format("CSV instance error [row: %d]: %s", rowNumber, message);
    }

    private String createErrorMessage(Set<ConstraintViolation<?>> violations, int rowNumber) {
        return String.format("CSV instance error [row: %d]: validation failed for instance of type %s, violations: %s",
                rowNumber, Swachchagrahi.class.getName(), ConstraintViolationUtils.toString(violations));
    }

    @Autowired
    public void setSwcService(SwcService swcService) {
        this.swcService = swcService;
    }

    @Autowired
    public void setLanguageService(LanguageService languageService) {
        this.languageService = languageService;
    }

    @Autowired
    public void setStateDataService(StateDataService stateDataService) {
        this.stateDataService = stateDataService;
    }

    @Autowired
    public void setWashAcademyService(WashAcademyService washAcademyService) {
        this.washAcademyService = washAcademyService;
    }

}
