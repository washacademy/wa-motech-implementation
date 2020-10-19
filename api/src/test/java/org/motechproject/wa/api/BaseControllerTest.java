package org.motechproject.wa.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.motechproject.wa.api.web.contract.BadRequest;
import org.motechproject.wa.swc.service.SwcService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BaseControllerTest {

   
    @Mock
    private SwcService subscriberService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        
    }

    @Test
    public void testInternalError() throws Exception {
        String message = "error";
        ObjectMapper objectMapper = new ObjectMapper();
        BadRequest badRequest = new BadRequest(message);
        when(subscriberService.getByContactNumberAndCourseId(anyLong(),anyInt())).thenThrow(new NullPointerException(message));

        String url = "/kilkari/inbox?callingNumber=1111111111&callId=1234567891234561234512345";

       
    }
}
