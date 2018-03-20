package org.motechproject.wa.swcUpdate.web;

import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.wa.csv.exception.CsvImportDataException;
import org.motechproject.wa.csv.exception.CsvImportException;
import org.motechproject.wa.csv.service.CsvAuditService;
import org.motechproject.wa.swc.domain.SubscriptionOrigin;
import org.motechproject.wa.swcUpdate.service.SwcImportService;
import org.motechproject.wa.swcUpdate.service.SwcUpdateImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;

@SuppressWarnings("PMD")
@Controller
public class SwcImportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwcImportController.class);

    private AlertService alertService;
    private SwcImportService swcImportService;
    private SwcUpdateImportService swcUpdateImportService;
    private CsvAuditService csvAuditService;

    @ExceptionHandler(CsvImportDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleException(CsvImportDataException e) {
        return e.getMessage();
    }

    @RequestMapping(value = "/update/language", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void updateFrontLineWorkersLanguage(@RequestParam MultipartFile csvFile) {

        try {
            try (InputStream in = csvFile.getInputStream()) {
                swcUpdateImportService.importLanguageData(new InputStreamReader(in));
                csvAuditService.auditSuccess(csvFile.getOriginalFilename(), "/swcUpdate/update/language");
            }
        } catch (CsvImportDataException e) {
            logError(csvFile.getOriginalFilename(), "/swcUpdate/update/language", e, "front_line_workers_language_import_error",
                    "Front line workers language import error");
            throw e;
        } catch (Exception e) {
            logError(csvFile.getOriginalFilename(), "/swcUpdate/update/language", e, "front_line_workers_language_import_error",
                    "Front line workers language import error");
            throw new CsvImportException("An error occurred during CSV import", e);
        }
    }

    @RequestMapping(value = "/update/msisdn", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void updateFrontLineWorkersMSISDN(@RequestParam MultipartFile csvFile) {

        try {
            try (InputStream in = csvFile.getInputStream()) {
                swcUpdateImportService.importMSISDNData(new InputStreamReader(in));
                csvAuditService.auditSuccess(csvFile.getOriginalFilename(), "/swcUpdate/update/msisdn");
            }
        } catch (CsvImportDataException e) {
            logError(csvFile.getOriginalFilename(), "/swcUpdate/update/msisdn", e, "front_line_workers_msisdn_import_error",
                    "Front line workers msisdn import error");
            throw e;
        } catch (Exception e) {
            logError(csvFile.getOriginalFilename(), "/swcUpdate/update/msisdn", e, "front_line_workers_msisdn_import_error",
                    "Front line workers msisdn import error");
            throw new CsvImportException("An error occurred during CSV import", e);
        }
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void importFrontLineWorkers(@RequestParam MultipartFile csvFile) {

        try {
            try (InputStream in = csvFile.getInputStream()) {
                swcImportService.importData(new InputStreamReader(in), SubscriptionOrigin.MCTS_IMPORT);
                csvAuditService.auditSuccess(csvFile.getOriginalFilename(), "/swcUpdate/import");
            }
        } catch (CsvImportDataException e) {
            logError(csvFile.getOriginalFilename(), "/swcUpdate/import", e, "front_line_workers_import_error",
                    "Front line workers import error");
            throw e;
        } catch (Exception e) {
            logError(csvFile.getOriginalFilename(), "/swcUpdate/import", e, "front_line_workers_import_error",
                    "Front line workers import error");
            throw new CsvImportException("An error occurred during CSV import", e);
        }
    }

    @RequestMapping(value = "/rchImport", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void importRchFrontLineWorkers(@RequestParam MultipartFile csvFile) {

        try {
            try (InputStream in = csvFile.getInputStream()) {
                swcImportService.importData(new InputStreamReader(in), SubscriptionOrigin.RCH_IMPORT);
                csvAuditService.auditSuccess(csvFile.getOriginalFilename(), "/swcUpdate/import");
            }
        } catch (CsvImportDataException e) {
            logError(csvFile.getOriginalFilename(), "/swcUpdate/rchImport", e, "rch_front_line_workers_import_error",
                    "RCH Front line workers import error");
            throw e;
        } catch (Exception e) {
            logError(csvFile.getOriginalFilename(), "/swcUpdate/rchImport", e, "rch_front_line_workers_import_error",
                    "RCH Front line workers import error");
            throw new CsvImportException("An error occurred during CSV import", e);
        }
    }

    private void logError(String fileName, String endpoint, Exception exception, String entityId, String name) {
        LOGGER.error(exception.getMessage(), exception);
        csvAuditService.auditFailure(fileName, endpoint, exception.getMessage());
        alertService.create(entityId, name, exception.getMessage(), AlertType.CRITICAL, AlertStatus.NEW, 0, null);
    }

    @Autowired
    public void setAlertService(AlertService alertService) {
        this.alertService = alertService;
    }

    @Autowired
    public void setSwcImportService(SwcImportService swcImportService) {
        this.swcImportService = swcImportService;
    }

    @Autowired
    public void setSwcUpdateImportService(SwcUpdateImportService swcUpdateImportService) {
        this.swcUpdateImportService = swcUpdateImportService;
    }

    @Autowired
    public void setCsvAuditService(CsvAuditService csvAuditService) {
        this.csvAuditService = csvAuditService;
    }
}
