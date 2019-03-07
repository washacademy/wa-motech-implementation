package org.motechproject.wa.api.web;

import org.motechproject.wa.api.web.contract.AddSwcRequest;
import org.motechproject.wa.api.web.contract.washAcademy.GetBookmarkResponse;
import org.motechproject.wa.api.web.converter.WashAcademyConverter;
import org.motechproject.wa.api.web.service.SwcCsvService;
import org.motechproject.wa.props.service.LogHelper;
import org.motechproject.wa.swc.domain.DeactivationReason;
import org.motechproject.wa.swc.repository.SwcDataService;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.washacademy.dto.WaBookmark;
import org.motechproject.wa.washacademy.service.WashAcademyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller to expose methods for OPS personnel
 */
@SuppressWarnings("PMD")
@RequestMapping("/ops")
@Controller
public class OpsController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpsController.class);

    @Autowired
    private SwcDataService swcDataService;

    @Autowired
    private SwcService subscriberService;;

    @Autowired
    private WashAcademyService washAcademyService;

    @Autowired
    private SwcCsvService swcCsvService;

    private final String contactNumber = "contactNumber";

    //only for debugging purposes and will not be returned anywhere

    /**
     * Provided for OPS as a crutch to be able to empty all MDS cache directly after modifying the database by hand
     */
    @RequestMapping("/evictAllCache")
    @ResponseStatus(HttpStatus.OK)
    public void evictAllCache() {
        LOGGER.info("/evictAllCache()");
        swcDataService.evictAllCache();
    }

    @RequestMapping("/ping")
    @ResponseStatus(HttpStatus.OK)
    public String ping() {
        LOGGER.info("/ping()");
        return "PING";
    }

//    @RequestMapping("/cleanCallRecords")
//    @ResponseStatus(HttpStatus.OK)
//    public void clearCallRecords() {
//
//        LOGGER.info("/cleanCdr()");
//        cdrFileService.cleanOldCallRecords();
//    }
    @RequestMapping(value = "/createUpdateRchSwc",
            method = RequestMethod.POST,
            headers = { "Content-type=application/json" })
    @ResponseStatus(HttpStatus.OK)
    public void createUpdateRchSwc(@RequestBody AddSwcRequest addSwcRequest) {
        StringBuilder failureReasons = swcCsvService.csvUploadRch(addSwcRequest);
//        LOGGER.info(failureReasons.toString());
        if (failureReasons != null) {
            throw new IllegalArgumentException(failureReasons.toString());
        } else {
            swcCsvService.persistSwcRch(addSwcRequest);
        }
    }

    @RequestMapping(value = "/csvimportRchSwc",
            method = RequestMethod.POST,
            headers = { "Content-type=application/json" })
    @ResponseStatus(HttpStatus.OK)
    public void csvimportRchSwc(@RequestBody List<AddSwcRequest> addSwcRequestList) {
        swcCsvService.createLocation();
        for(AddSwcRequest addSwcRequest: addSwcRequestList) {
            StringBuilder failureReasons = swcCsvService.csvUploadRch(addSwcRequest);
//        LOGGER.info(failureReasons.toString());
            if (failureReasons != null) {
                throw new IllegalArgumentException(failureReasons.toString());
            } else {
                swcCsvService.persistCsvSwcRch(addSwcRequest);
            }
        }
    }

    @RequestMapping("/getbookmark")
    @ResponseBody
    public GetBookmarkResponse getBookmarkWithScore(@RequestParam(required = false) Long callingNumber) {
        LOGGER.info("/getbookmark");
        WaBookmark bookmark = washAcademyService.getBookmarkOps(callingNumber);
        GetBookmarkResponse ret = WashAcademyConverter.convertBookmarkDto(bookmark);
        log("RESPONSE: /ops/getbookmark", String.format("bookmark=%s", ret.toString()));
        return ret;
    }

    @RequestMapping(value = "/deactivationRequest",
            method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void deactivationRequest(@RequestParam(value = "msisdn") Long msisdn, @RequestParam(value = "deactivationReason") String deactivationReason) {
        log("REQUEST: /ops/deactivationRequest", String.format(
                "callingNumber=%s",
                LogHelper.obscure(msisdn)));
        StringBuilder failureReasons = new StringBuilder();
        validateField10Digits(failureReasons, contactNumber, msisdn);
        validateFieldPositiveLong(failureReasons, contactNumber, msisdn);
        validateDeactivationReason(failureReasons, "deactivationReason", deactivationReason);
        if (failureReasons.length() > 0) {
            throw new IllegalArgumentException(failureReasons.toString());
        }
        DeactivationReason reason = DeactivationReason.valueOf(deactivationReason);
        subscriberService.getRecords();
        LOGGER.info(reason.name());
    }

    @RequestMapping("/getScores")
    @ResponseBody
    public String getScoresForNumber(@RequestParam(required = true) Long callingNumber) {
        LOGGER.info("/getScores Getting scores for user");
        String scores = washAcademyService.getScoresForUser(callingNumber);
        LOGGER.info("Scores: " + scores);
        return scores;
    }


}

