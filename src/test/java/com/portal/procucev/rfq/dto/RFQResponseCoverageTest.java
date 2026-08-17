package com.portal.procucev.rfq.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class RFQResponseCoverageTest {

    @Test
    void testRFQResponseFullCoverage() {
        LocalDateTime now = LocalDateTime.now();
        RFQResponse response = RFQResponse.builder()
                .rfqNumber("RFQ-20260817001")
                .status("SUCCESS")
                .buyerEmail("buyer@example.com")
                .message("RFQ created successfully")
                .createdAt(now)
                .build();

        assertEquals("RFQ-20260817001", response.getRfqNumber());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("buyer@example.com", response.getBuyerEmail());
        assertEquals("RFQ created successfully", response.getMessage());
        assertEquals(now, response.getCreatedAt());

        assertNotNull(response.getDisplayRfqNumber());
        assertNotNull(response.getShortRfqNumber());

        RFQResponse response2 = new RFQResponse();
        response2.setRfqNumber("RFQ-20260817001");
        response2.setStatus("SUCCESS");
        response2.setBuyerEmail("buyer@example.com");
        response2.setMessage("RFQ created successfully");
        response2.setCreatedAt(now);

        assertEquals(response, response2);
        assertEquals(response.hashCode(), response2.hashCode());
        assertNotNull(response.toString());
    }
}
