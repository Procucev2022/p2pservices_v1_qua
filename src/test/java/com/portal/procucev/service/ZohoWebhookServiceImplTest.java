package com.portal.procucev.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.EventObject;
import com.portal.procucev.Dto.PaymentLinks;
import com.portal.procucev.Dto.ZohoPaymentLinkWebhookRequest;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.PaymentLinkRepository;
import com.portal.procucev.dao.ZohoPaymentWebhookRepository;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PaymentLink;
import com.portal.procucev.model.SubscriptionPlan;
import com.portal.procucev.model.ZohoPaymentWebhookEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZohoWebhookServiceImplTest {

    @Mock
    private PaymentLinkRepository paymentLinkRepo;
    @Mock
    private ZohoPaymentWebhookRepository webhookRepo;
    @Mock
    private OrgDao orgDao;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ZohoWebhookServiceImpl service;

    private ZohoPaymentLinkWebhookRequest req;
    private PaymentLink link;
    private Organization org;
    private SubscriptionPlan plan;

    @BeforeEach
    void setUp() throws Exception {
        req = new ZohoPaymentLinkWebhookRequest();
        req.setEvent_id(12345L);
        req.setEvent_type("payment_link.paid");
        req.setEvent_time(System.currentTimeMillis());

        EventObject eventObj = new EventObject();
        PaymentLinks pLinks = new PaymentLinks();
        pLinks.setPayment_link_id("PL123");
        pLinks.setStatus("PAID");
        eventObj.setPayment_links(pLinks);
        req.setEvent_object(eventObj);

        org = new Organization();
        org.setId("ORG1");
        org.setRfqCredits(10);

        plan = new SubscriptionPlan();
        plan.setId("PLAN1");
        plan.setSubscriptionPeriodMonths(12);
        plan.setRfqBundleSize(100);

        link = new PaymentLink();
        link.setId(1L);
        link.setZohoPaymentLinkId("PL123");
        link.setOrganization(org);
        link.setPlan(plan);

        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":true}");
    }

    @Test
    void testProcessWebhook_AlreadyProcessed_Skips() {
        ZohoPaymentWebhookEntity existing = new ZohoPaymentWebhookEntity();
        existing.setProcessed(true);
        when(webhookRepo.findByEventId(12345L)).thenReturn(existing);

        service.processWebhook(req);
        verify(paymentLinkRepo, never()).findByZohoPaymentLinkId(anyString());
    }

    @Test
    void testProcessWebhook_ExistingNotProcessed() {
        ZohoPaymentWebhookEntity existing = new ZohoPaymentWebhookEntity();
        existing.setProcessed(false);
        when(webhookRepo.findByEventId(12345L)).thenReturn(existing);
        when(paymentLinkRepo.findByZohoPaymentLinkId("PL123")).thenReturn(Optional.of(link));

        service.processWebhook(req);
        assertEquals("PAID", link.getStatus());
        verify(orgDao).save(org);
    }

    @Test
    void testProcessWebhook_Canceled_Expired_Default() {
        when(webhookRepo.findByEventId(12345L)).thenReturn(null);
        when(paymentLinkRepo.findByZohoPaymentLinkId("PL123")).thenReturn(Optional.of(link));

        req.setEvent_type("payment_link.canceled");
        service.processWebhook(req);
        assertEquals("CANCELED", link.getStatus());

        req.setEvent_type("payment_link.expired");
        service.processWebhook(req);
        assertEquals("EXPIRED", link.getStatus());

        req.setEvent_type("payment_link.unknown");
        service.processWebhook(req);
    }

    @Test
    void testProcessWebhook_NullOrg_And_NullPlan() {
        when(webhookRepo.findByEventId(12345L)).thenReturn(null);
        when(paymentLinkRepo.findByZohoPaymentLinkId("PL123")).thenReturn(Optional.of(link));

        link.setOrganization(null);
        req.setEvent_type("payment_link.paid");
        service.processWebhook(req);

        link.setOrganization(org);
        link.setPlan(null);
        service.processWebhook(req);
    }

    @Test
    void testProcessWebhook_PaymentLinkNotFound() {
        when(webhookRepo.findByEventId(12345L)).thenReturn(null);
        when(paymentLinkRepo.findByZohoPaymentLinkId("PL123")).thenReturn(Optional.empty());

        service.processWebhook(req);
        verify(paymentLinkRepo, never()).save(any(PaymentLink.class));
    }

    @Test
    void testProcessWebhook_EventTimeNullOrNegative() {
        when(webhookRepo.findByEventId(12345L)).thenReturn(null);
        when(paymentLinkRepo.findByZohoPaymentLinkId("PL123")).thenReturn(Optional.of(link));

        req.setEvent_time(null);
        service.processWebhook(req);

        req.setEvent_time(-1L);
        service.processWebhook(req);
    }

    @Test
    void testActivateSubscription_ExceptionHandled() {
        when(webhookRepo.findByEventId(12345L)).thenReturn(null);
        when(paymentLinkRepo.findByZohoPaymentLinkId("PL123")).thenReturn(Optional.of(link));
        doThrow(new RuntimeException("db error")).when(orgDao).save(any(Organization.class));

        assertDoesNotThrow(() -> service.processWebhook(req));
    }

    @Test
    void testProcessWebhook_JsonProcessingException() throws Exception {
        when(webhookRepo.findByEventId(12345L)).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("json err") {});

        assertThrows(RuntimeException.class, () -> service.processWebhook(req));
    }

    @Test
    void testProcessWebhook_ActivateSubscriptionException() {
        when(webhookRepo.findByEventId(12345L)).thenReturn(null);
        when(paymentLinkRepo.findByZohoPaymentLinkId("PL123")).thenReturn(Optional.of(link));
        doThrow(new RuntimeException("db err")).when(orgDao).save(any());

        req.setEvent_type("payment_link.paid");
        assertDoesNotThrow(() -> service.processWebhook(req));
    }
}
