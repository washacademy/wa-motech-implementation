package org.motechproject.wa.region.controller;

import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.wa.csv.exception.CsvImportException;
import org.motechproject.wa.csv.service.CsvAuditService;
import org.motechproject.wa.region.csv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LocationDataImportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDataImportController.class);

    private AlertService alertService;

    private StateImportService stateImportService;
    private DistrictImportService districtImportService;
    private BlockImportService blockImportService;
    private PanchayatImportService panchayatImportService;
    private CsvAuditService csvAuditService;

    private Map<String, LocationDataImportService> locationDataImportServiceMapping;

    @RequestMapping(value = "/data/import/{location}", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void importLocationData(@RequestParam MultipartFile csvFile, @PathVariable String location) {
        String endpoint = String.format("region/data/import/%s", location);
        try {
            try (InputStream in = csvFile.getInputStream()) {
                LocationDataImportService importService = getLocationDataImportServiceMapping().get(location);
                if (null != importService) {
                    importService.importData(new InputStreamReader(in));
                    csvAuditService.auditSuccess(csvFile.getOriginalFilename(), endpoint);
                } else {
                    String error = String.format("Location type '%s' not supported", location);
                    csvAuditService.auditFailure(csvFile.getOriginalFilename(), endpoint, error);
                    throw new IllegalArgumentException(error);
                }
            }
        } catch (CsvImportException e) {
            logError(csvFile.getOriginalFilename(), endpoint, location, e);
            throw e;
        } catch (Exception e) {
            logError(csvFile.getOriginalFilename(), endpoint, location, e);
            throw new CsvImportException("An error occurred during CSV import", e);
        }
    }

    private void logError(String fileName, String endpoint, String location, Exception exception) {
        LOGGER.error(exception.getMessage(), exception);
        csvAuditService.auditFailure(fileName, endpoint, exception.getMessage());
        alertService.create("location_data_import_error",
                String.format("Location data import error: %s", location),
                exception.getMessage(), AlertType.CRITICAL, AlertStatus.NEW, 0, null);
    }

    @ExceptionHandler(CsvImportException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleCsvImportException() {
    }

    @Autowired
    public void setAlertService(AlertService alertService) {
        this.alertService = alertService;
    }

    @Autowired
    public void setStateImportService(StateImportService stateImportService) {
        this.stateImportService = stateImportService;
    }

    @Autowired
    public void setDistrictImportService(DistrictImportService districtImportService) {
        this.districtImportService = districtImportService;
    }

    @Autowired
    public void setBlockImportService(BlockImportService blockImportService) {
        this.blockImportService = blockImportService;
    }
    
    @Autowired
    public void setPanchayatImportService(PanchayatImportService panchayatImportService) {
        this.panchayatImportService = panchayatImportService;
    }


    @Autowired
    public void setCsvAuditService(CsvAuditService csvAuditService) {
        this.csvAuditService = csvAuditService;
    }

    private Map<String, LocationDataImportService> getLocationDataImportServiceMapping() {
        if (null == locationDataImportServiceMapping) {
            locationDataImportServiceMapping = new HashMap<>();
            locationDataImportServiceMapping.put("state", stateImportService);
            locationDataImportServiceMapping.put("district", districtImportService);
            locationDataImportServiceMapping.put("block", blockImportService);
            locationDataImportServiceMapping.put("panchayat", panchayatImportService);
        }
        return locationDataImportServiceMapping;
    }
}
