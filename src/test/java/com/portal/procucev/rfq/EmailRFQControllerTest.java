package com.portal.procucev.rfq;

import com.portal.procucev.rfq.controller.EmailRFQController;
import com.portal.procucev.rfq.dto.ApiResponse;
import com.portal.procucev.rfq.dto.ProcessingStats;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.service.EmailProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EmailRFQControllerTest {

    private EmailProcessorService emailProcessorService;
    private RFQRepository rfqRepository;
    private EmailRFQController controller;

    @BeforeEach
    void setUp() {
        emailProcessorService = Mockito.mock(EmailProcessorService.class);
        rfqRepository = Mockito.mock(RFQRepository.class);
        controller = new EmailRFQController(emailProcessorService, rfqRepository);
    }

    @Test
    @DisplayName("Test processEmailsManually endpoint")
    void testProcessEmailsManually() {
        ProcessingStats stats = ProcessingStats.builder().emailsProcessed(5).rfqsCreated(4).build();
        Mockito.when(emailProcessorService.processUnreadEmails()).thenReturn(stats);

        ResponseEntity<ApiResponse<ProcessingStats>> response = controller.processEmailsManually();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(5, response.getBody().getData().getEmailsProcessed());
    }

    @Test
    @DisplayName("Test getRfqByNumber endpoint found and not found")
    void testGetRfqByNumber() {
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn("buyer@example.com");

        RFQEntity entity = RFQEntity.builder().rfqNumber("RFQ-100").buyerEmail("buyer@example.com").build();
        Mockito.when(rfqRepository.findByRfqNumberAndBuyerEmailIgnoreCase("RFQ-100", "buyer@example.com"))
                .thenReturn(Optional.of(entity));
        Mockito.when(rfqRepository.findByRfqNumberAndBuyerEmailIgnoreCase("RFQ-NOT-FOUND", "buyer@example.com"))
                .thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<RFQEntity>> foundResp = controller.getRfqByNumber("RFQ-100", userDetails);
        assertEquals(HttpStatus.OK, foundResp.getStatusCode());
        assertEquals("RFQ-100", foundResp.getBody().getData().getRfqNumber());

        ResponseEntity<ApiResponse<RFQEntity>> notFoundResp = controller.getRfqByNumber("RFQ-NOT-FOUND", userDetails);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResp.getStatusCode());
        assertFalse(notFoundResp.getBody().isSuccess());
    }
}
