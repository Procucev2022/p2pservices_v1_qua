package com.portal.procucev.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.dao.PaymentLinkRepository;
import com.portal.procucev.model.PaymentLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationJobTest {

    @Mock
    private PaymentLinkRepository paymentLinkRepo;
    @Mock
    private ZohoApiClient zohoApiClient;
    @Mock
    private ObjectMapper objectMapper;

    private PaymentReconciliationJob job;

    @BeforeEach
    void setUp() {
        job = new PaymentReconciliationJob(paymentLinkRepo, zohoApiClient, objectMapper);
        ReflectionTestUtils.setField(job, "baseUrl", "http://zoho.com");
        ReflectionTestUtils.setField(job, "accountId", "ACC1");
    }

    @Test
    void testReconcilePayments_Disabled() {
        ReflectionTestUtils.setField(job, "isEnabled", false);
        job.reconcilePayments();
        verifyNoInteractions(paymentLinkRepo);
    }

    @Test
    void testReconcilePayments_Enabled_SuccessAndException() throws Exception {
        ReflectionTestUtils.setField(job, "isEnabled", true);

        PaymentLink link1 = new PaymentLink();
        link1.setZohoPaymentLinkId("PL1");
        PaymentLink link2 = new PaymentLink();
        link2.setZohoPaymentLinkId("PL2");

        when(paymentLinkRepo.findByStatusIn(anyList())).thenReturn(List.of(link1, link2));

        Map<String, Object> respMap = Map.of("payment_links", Map.of("status", "PAID", "amount_paid", "100"));
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(respMap);

        when(zohoApiClient.get("http://zoho.com/paymentlinks/PL1?account_id=ACC1", Map.class)).thenReturn(responseEntity);
        when(zohoApiClient.get("http://zoho.com/paymentlinks/PL2?account_id=ACC1", Map.class)).thenThrow(new RuntimeException("API error"));

        when(objectMapper.writeValueAsString(respMap)).thenReturn("{}");

        job.reconcilePayments();

        verify(paymentLinkRepo, times(1)).save(link1);
        verify(paymentLinkRepo, never()).save(link2);
    }
}
