package org.motechproject.wa.imi.web;

import org.apache.commons.lang3.StringUtils;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.wa.imi.exception.InvalidCallRecordFileException;
import org.motechproject.wa.imi.exception.NotFoundException;
import org.motechproject.wa.imi.web.contract.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * IMI Controller - handles all API interaction with the IMI IVR vendor
 */
@SuppressWarnings("PMD")
@Controller
public class ImiController {

    public static final String NOT_PRESENT = "<%s: Not Present>";
    public static final String INVALID = "<%s: Invalid>";
    public static final String INVALID_STATUS_ENUM = "Can not construct instance of " +
            "FileProcessedStatus from String value";
    public static final Pattern TARGET_FILENAME_PATTERN = Pattern.compile("OBD_wa_[0-9]{14}\\.csv");
    public static final String IVR_INTERACTION_LOG = "IVR INTERACTION: %s";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImiController.class);
    public static final String LOG_RESPONSE_FORMAT = "RESPONSE: HTTP %d - %s";

    private SettingsFacade settingsFacade;
    private AlertService alertService;


    @Autowired
    public ImiController(SettingsFacade settingsFacade, AlertService alertService) {
        this.settingsFacade = settingsFacade;
        this.alertService = alertService;
    }


    protected static void log(final String endpoint, final String s) {
        LOGGER.info(IVR_INTERACTION_LOG.format(endpoint) + (StringUtils.isBlank(s) ? "" : " : " + s));
    }

    @ExceptionHandler({ NotFoundException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public BadRequest handleException(NotFoundException e, HttpServletRequest request) {
        log(String.format(LOG_RESPONSE_FORMAT, HttpStatus.NOT_FOUND.value(), request.getRequestURI()), e.getMessage());
        return new BadRequest(e.getMessage());
    }


    @ExceptionHandler({ RuntimeException.class })
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public BadRequest handleException(RuntimeException e, HttpServletRequest request) {
        log(String.format(LOG_RESPONSE_FORMAT, HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI()), e.getMessage());
        return new BadRequest(e.toString());
    }

    @ExceptionHandler({ IllegalArgumentException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public BadRequest handleException(IllegalArgumentException e, HttpServletRequest request) {
        log(String.format(LOG_RESPONSE_FORMAT, HttpStatus.BAD_REQUEST.value(), request.getRequestURI()), e.getMessage());
        return new BadRequest(e.getMessage());
    }


    /**
     * Handles malformed JSON, returns a slightly more informative message than a generic HTTP-400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BadRequest handleException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log(String.format(LOG_RESPONSE_FORMAT, HttpStatus.BAD_REQUEST.value(), request.getRequestURI()), e.getMessage());
        if (e.getLocalizedMessage().contains(INVALID_STATUS_ENUM)) {
            return new BadRequest("<fileProcessedStatus: Invalid Value>");
        }
        return new BadRequest(e.getMessage());
    }




    /**
     * Handles InvalidCallRecordFileException - potentially a large amount of errors all in one list of string
     */
    //todo: IT or UT
    @ExceptionHandler(InvalidCallRecordFileException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AggregateBadRequest handleException(InvalidCallRecordFileException e, HttpServletRequest request) {
        log(String.format(LOG_RESPONSE_FORMAT, HttpStatus.BAD_REQUEST.value(), request.getRequestURI()), e.getMessages().toString());
        return new AggregateBadRequest(e.getMessages());
    }


    /**
     * Handles any other exception
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BadRequest handleException(Exception e, HttpServletRequest request) {
        log(String.format(LOG_RESPONSE_FORMAT, HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI()), e.getMessage());
        return new BadRequest(e.getMessage());
    }
}
