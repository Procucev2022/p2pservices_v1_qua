package com.portal.procucev.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.DailyRfqReportDispatchResponse;
import com.portal.procucev.Dto.DailyRfqReportPreviewDto;
import com.portal.procucev.dao.DailyRfqReportLogDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.model.DailyRfqReportLog;
import com.portal.procucev.model.Rfq;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRfqReportServiceImplTest {

    @Mock
    private RfqDao rfqDao;

    @Mock
    private DailyRfqReportLogDao dailyRfqReportLogDao;

    @Mock
    private JavaMailSender javaMailSender;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DailyRfqReportServiceImpl service;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultFromEmail", "notifications@procucev.com");
        ReflectionTestUtils.setField(service, "defaultToEmail", "veerababu.v@procucev.com");
        mimeMessage = new MimeMessage((Session) null);
    }

    private Rfq createMockRfq(String rfqId, String sourceType) {
        Rfq rfq = new Rfq();
        rfq.setId("uuid-" + rfqId);
        rfq.setRfqId(rfqId);
        rfq.setSourceType(sourceType);
        rfq.setByClient(true);
        rfq.setNoPrFlag(true);
        return rfq;
    }

    @Test
    void testPreviewDailyReportWithRfqs() {
        LocalDate date = LocalDate.of(2026, 9, 27);

        Rfq rfq1 = createMockRfq("RFQ262408469792", "TOOL");
        Rfq rfq2 = createMockRfq("RFQ262408469793", "WEB");
        Rfq rfq3 = createMockRfq("RFQ262408469794", "WHATSAPP");
        Rfq rfq4 = createMockRfq("RFQ262408469795", "EMAIL");

        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(rfq1, rfq2, rfq3, rfq4));

        DailyRfqReportPreviewDto preview = service.previewDailyReport(date);

        assertNotNull(preview);
        assertEquals(date, preview.getReportDate());
        assertEquals(2, preview.getWebPortalCount());
        assertEquals(1, preview.getWhatsappCount());
        assertEquals(1, preview.getEmailCount());
        assertEquals(4, preview.getTotalCount());
        assertEquals(Arrays.asList("RFQ262408469792", "RFQ262408469793"), preview.getWebPortalRfqIds());
        assertEquals(Collections.singletonList("RFQ262408469794"), preview.getWhatsappRfqIds());
        assertEquals(Collections.singletonList("RFQ262408469795"), preview.getEmailRfqIds());
        assertEquals(4, preview.getRfqIds().size());
        assertEquals("Daily RFQ Report for production - 27 Sep 2026", preview.getSubject());
        assertEquals("veerababu.v@procucev.com", preview.getRecipient());

        String expectedBody = "Daily RFQ Report\n\n" +
                "Web Portal : 2\n" +
                "  - RFQ262408469792\n" +
                "  - RFQ262408469793\n\n" +
                "WhatsApp   : 1\n" +
                "  - RFQ262408469794\n\n" +
                "Email      : 1\n" +
                "  - RFQ262408469795\n\n" +
                "Total      : 4";
        assertEquals(expectedBody, preview.getEmailText());

        verify(javaMailSender, never()).send(any(MimeMessage.class));
        verify(dailyRfqReportLogDao, never()).save(any());
    }

    @Test
    void testPreviewDailyReportZeroRfqsAndNullDate() {
        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());

        DailyRfqReportPreviewDto preview = service.previewDailyReport(null);

        assertNotNull(preview);
        assertNotNull(preview.getReportDate());
        assertEquals(0, preview.getWebPortalCount());
        assertEquals(0, preview.getWhatsappCount());
        assertEquals(0, preview.getEmailCount());
        assertEquals(0, preview.getTotalCount());

        String expectedBody = "Daily RFQ Report\n\n" +
                "Web Portal : 0\n\n" +
                "WhatsApp   : 0\n\n" +
                "Email      : 0\n\n" +
                "Total      : 0";
        assertEquals(expectedBody, preview.getEmailText());
    }

    @Test
    void testDispatchDailyReportSuccess() {
        LocalDate date = LocalDate.of(2026, 9, 27);

        Rfq rfq1 = createMockRfq("RFQ1", "W");
        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(rfq1));
        when(dailyRfqReportLogDao.findByReportDate(date)).thenReturn(Optional.empty());
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        DailyRfqReportLog savedMock = DailyRfqReportLog.builder()
                .id(1L)
                .reportDate(date)
                .webPortalCount(0)
                .whatsappCount(1)
                .emailCount(0)
                .totalCount(1)
                .recipient("veerababu.v@procucev.com")
                .dispatchedAt(LocalDateTime.now())
                .build();
        when(dailyRfqReportLogDao.save(any(DailyRfqReportLog.class))).thenReturn(savedMock);

        DailyRfqReportDispatchResponse response = service.dispatchDailyReport(date, false, null);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getLog());
        assertEquals(1L, response.getLog().getId());

        verify(javaMailSender).send(any(MimeMessage.class));

        ArgumentCaptor<DailyRfqReportLog> logCaptor = ArgumentCaptor.forClass(DailyRfqReportLog.class);
        verify(dailyRfqReportLogDao).save(logCaptor.capture());
        DailyRfqReportLog captured = logCaptor.getValue();
        assertEquals(date, captured.getReportDate());
        assertEquals(0, captured.getWebPortalCount());
        assertEquals(1, captured.getWhatsappCount());
        assertEquals(0, captured.getEmailCount());
        assertEquals(1, captured.getTotalCount());
        assertEquals("veerababu.v@procucev.com", captured.getRecipient());
        assertTrue(captured.getRfqIdsJson().contains("RFQ1"));
    }

    @Test
    void testDispatchDailyReportDeduplicationSkipped() {
        LocalDate date = LocalDate.of(2026, 9, 27);
        DailyRfqReportLog existing = DailyRfqReportLog.builder()
                .id(5L)
                .reportDate(date)
                .webPortalCount(1)
                .totalCount(1)
                .dispatchedAt(LocalDateTime.now().minusHours(2))
                .build();

        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(dailyRfqReportLogDao.findByReportDate(date)).thenReturn(Optional.of(existing));

        DailyRfqReportDispatchResponse response = service.dispatchDailyReport(date, false, "   ");

        assertNotNull(response);
        assertEquals("SKIPPED", response.getStatus());
        assertTrue(response.getMessage().contains("already exists"));
        assertEquals(existing, response.getLog());

        verify(javaMailSender, never()).send(any(MimeMessage.class));
        verify(dailyRfqReportLogDao, never()).save(any());
    }

    @Test
    void testDispatchDailyReportForceOverrideAndMultipleRecipients() {
        LocalDate date = LocalDate.of(2026, 9, 27);
        DailyRfqReportLog existing = DailyRfqReportLog.builder()
                .id(5L)
                .reportDate(date)
                .webPortalCount(0)
                .totalCount(0)
                .build();

        Rfq rfq1 = createMockRfq("RFQ100", "TOOL");
        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(rfq1));
        when(dailyRfqReportLogDao.findByReportDate(date)).thenReturn(Optional.of(existing));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(dailyRfqReportLogDao.save(any(DailyRfqReportLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyRfqReportDispatchResponse response = service.dispatchDailyReport(date, true, "custom1@test.com,custom2@test.com");

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        verify(javaMailSender).send(any(MimeMessage.class));

        ArgumentCaptor<DailyRfqReportLog> logCaptor = ArgumentCaptor.forClass(DailyRfqReportLog.class);
        verify(dailyRfqReportLogDao).save(logCaptor.capture());
        DailyRfqReportLog captured = logCaptor.getValue();
        assertEquals(5L, captured.getId());
        assertEquals("custom1@test.com,custom2@test.com", captured.getRecipient());
        assertEquals(1, captured.getWebPortalCount());
    }

    @Test
    void testDispatchDailyReportWithSemicolonAndSpacedRecipients() {
        LocalDate date = LocalDate.of(2026, 9, 27);
        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());
        when(dailyRfqReportLogDao.findByReportDate(date)).thenReturn(Optional.empty());
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(dailyRfqReportLogDao.save(any(DailyRfqReportLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyRfqReportDispatchResponse response = service.dispatchDailyReport(date, false, "quateam@procucev.com ; veerababu.v@procucev.com");

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        verify(javaMailSender).send(any(MimeMessage.class));
        assertEquals("quateam@procucev.com ; veerababu.v@procucev.com", response.getLog().getRecipient());
    }

    @Test
    void testDispatchDailyReportFallbackRfqIdWhenNullOrBlank() {
        LocalDate date = LocalDate.of(2026, 9, 27);

        Rfq rfq1 = new Rfq();
        rfq1.setId("uuid-111");
        rfq1.setRfqId(null);
        rfq1.setSourceType("W");
        rfq1.setByClient(true);
        rfq1.setNoPrFlag(true);

        Rfq rfq2 = new Rfq();
        rfq2.setId("uuid-222");
        rfq2.setRfqId("   ");
        rfq2.setSourceType("EMAIL");
        rfq2.setByClient(true);
        rfq2.setNoPrFlag(true);

        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(rfq1, rfq2));
        when(dailyRfqReportLogDao.findByReportDate(date)).thenReturn(Optional.empty());
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(dailyRfqReportLogDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DailyRfqReportDispatchResponse response = service.dispatchDailyReport(date, false, null);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getLog().getRfqIdsJson().contains("uuid-111"));
        assertTrue(response.getLog().getRfqIdsJson().contains("uuid-222"));
    }

    @Test
    void testDispatchDailyReportJsonSerializationExceptionFallback() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 27);
        Rfq rfq1 = createMockRfq("RFQ1", "TOOL");

        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(rfq1));
        when(dailyRfqReportLogDao.findByReportDate(date)).thenReturn(Optional.empty());
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(dailyRfqReportLogDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new JsonProcessingException("Test serialization failure") {})
                .when(objectMapper).writeValueAsString(any());

        DailyRfqReportDispatchResponse response = service.dispatchDailyReport(date, false, null);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("[RFQ1]", response.getLog().getRfqIdsJson());
    }

    @Test
    void testDispatchDailyReportSendEmailFailureThrows() {
        LocalDate date = LocalDate.of(2026, 9, 27);
        when(rfqDao.findDailyIntakeRfqs(any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());
        when(dailyRfqReportLogDao.findByReportDate(date)).thenReturn(Optional.empty());
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP Connection Refused"));

        assertThrows(RuntimeException.class, () -> service.dispatchDailyReport(date, false, null));
        verify(dailyRfqReportLogDao, never()).save(any());
    }

    @Test
    void testGetReportHistory() {
        List<DailyRfqReportLog> mockHistory = Arrays.asList(
                DailyRfqReportLog.builder().id(2L).reportDate(LocalDate.of(2026, 9, 27)).build(),
                DailyRfqReportLog.builder().id(1L).reportDate(LocalDate.of(2026, 9, 26)).build()
        );
        when(dailyRfqReportLogDao.findAllByOrderByReportDateDesc()).thenReturn(mockHistory);

        List<DailyRfqReportLog> history = service.getReportHistory();
        assertEquals(2, history.size());
        assertEquals(2L, history.get(0).getId());
    }

    @Test
    void testGenerateEmailBodyFormattingEdgeCases() {
        // Only WhatsApp has items
        String body1 = service.generateEmailBody(Collections.emptyList(), Collections.singletonList("WA-1"), Collections.emptyList());
        String expected1 = "Daily RFQ Report\n\n" +
                "Web Portal : 0\n\n" +
                "WhatsApp   : 1\n" +
                "  - WA-1\n\n" +
                "Email      : 0\n\n" +
                "Total      : 1";
        assertEquals(expected1, body1);

        // Only Email has items
        String body2 = service.generateEmailBody(Collections.emptyList(), Collections.emptyList(), Arrays.asList("EM-1", "EM-2"));
        String expected2 = "Daily RFQ Report\n\n" +
                "Web Portal : 0\n\n" +
                "WhatsApp   : 0\n\n" +
                "Email      : 2\n" +
                "  - EM-1\n" +
                "  - EM-2\n\n" +
                "Total      : 2";
        assertEquals(expected2, body2);

        // Null list parameters handled safely
        String body3 = service.generateEmailBody(null, null, null);
        String expected3 = "Daily RFQ Report\n\n" +
                "Web Portal : 0\n\n" +
                "WhatsApp   : 0\n\n" +
                "Email      : 0\n\n" +
                "Total      : 0";
        assertEquals(expected3, body3);
    }
}
