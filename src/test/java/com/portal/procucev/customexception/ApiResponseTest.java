package com.portal.procucev.customexception;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void testApiResponse() {
        Date now = new Date();
        ApiResponse<String> response = new ApiResponse<>("200", "Success", "OK", now, Collections.emptyList(), "Data");

        assertEquals("200", response.getStatusCode());
        assertEquals("Success", response.getMessage());
        assertEquals("OK", response.getStatus());
        assertEquals(now, response.getTimestamp());
        assertEquals(Collections.emptyList(), response.getErrorMsg());
        assertEquals("Data", response.getData());

        ApiResponse<String> defaultResp = new ApiResponse<>();
        defaultResp.setStatusCode("400");
        assertEquals("400", defaultResp.getStatusCode());
    }
}
