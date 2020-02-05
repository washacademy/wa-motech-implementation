package org.motechproject.wa.api.ut;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.wa.api.web.BaseController;
import org.motechproject.wa.api.web.WashAcademyController;
import org.motechproject.wa.api.web.contract.washAcademy.SaveBookmarkRequest;
import org.motechproject.wa.washacademy.service.WashAcademyService;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Unit tests for Mobile academy controller
 */
public class WashAcademyControllerUnitTest {

    private static final String VALID_CALL_ID = "1234567890123456789012345";

    private WashAcademyController washAcademyController;
    
    @Mock
    private WashAcademyService washAcademyService;

    @Mock
    private EventRelay eventRelay;

    @Before
    public void setup() {
        washAcademyController = new WashAcademyController(washAcademyService, eventRelay);
        initMocks(this);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBookmarkNullCallingNumber() {
        SaveBookmarkRequest sb = new SaveBookmarkRequest();
        sb.setCallingNumber(null);
        sb.setCallId(VALID_CALL_ID);
        washAcademyController.saveBookmarkWithScore(sb);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBookmarkMinCallingNumber() {
        SaveBookmarkRequest sb = new SaveBookmarkRequest();
        sb.setCallingNumber(999999999L);
        sb.setCallId(VALID_CALL_ID);
        washAcademyController.saveBookmarkWithScore(sb);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullBookmark() {
        washAcademyController.saveBookmarkWithScore(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBookmarkMaxCallingNumber() {
        SaveBookmarkRequest sb = new SaveBookmarkRequest();
        sb.setCallingNumber(10000000000L);
        sb.setCallId(VALID_CALL_ID);
        washAcademyController.saveBookmarkWithScore(sb);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBookmarkNullCallId() {
        SaveBookmarkRequest sb = new SaveBookmarkRequest();
        sb.setCallingNumber(BaseController.SMALLEST_10_DIGIT_NUMBER);
        sb.setCallId(null);
        washAcademyController.saveBookmarkWithScore(sb);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBookmarkInvalidCallId() {
        SaveBookmarkRequest sb = new SaveBookmarkRequest();
        sb.setCallingNumber(BaseController.SMALLEST_10_DIGIT_NUMBER);
        sb.setCallId(VALID_CALL_ID.substring(1));
        washAcademyController.saveBookmarkWithScore(sb);
    }

}
