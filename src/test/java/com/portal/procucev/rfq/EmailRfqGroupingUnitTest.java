package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.repository.RfqItemRecordRepository;
import com.portal.procucev.rfq.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EmailRfqGroupingUnitTest {

    private EmailReaderService emailReaderService;
    private AIExtractionService aiExtractionService;
    private ValidationService validationService;
    private BuyerVerificationService buyerVerificationService;
    private RFQBuilderService rfqBuilderService;
    private RFQApiService rfqApiService;
    private CategoryClassificationService categoryClassificationService;
    private JavaMailSender mailSender;
    private AcknowledgementEmailService acknowledgementEmailService;
    private RFQRepository rfqRepository;
    private EmailTransactionRepository emailTransactionRepository;
    private RfqItemRecordRepository rfqItemRecordRepository;
    private DateParser dateParser;
    private ObjectMapper objectMapper;

    private EmailProcessorService emailProcessorService;
    private Buyer defaultBuyer;

    @BeforeEach
    void setUp() {
        emailReaderService = Mockito.mock(EmailReaderService.class);
        aiExtractionService = Mockito.mock(AIExtractionService.class);
        validationService = new ValidationService();
        buyerVerificationService = Mockito.mock(BuyerVerificationService.class);
        rfqBuilderService = Mockito.mock(RFQBuilderService.class);
        rfqApiService = Mockito.mock(RFQApiService.class);
        categoryClassificationService = Mockito.mock(CategoryClassificationService.class);
        mailSender = Mockito.mock(JavaMailSender.class);
        rfqRepository = Mockito.mock(RFQRepository.class);
        emailTransactionRepository = Mockito.mock(EmailTransactionRepository.class);
        rfqItemRecordRepository = Mockito.mock(RfqItemRecordRepository.class);
        dateParser = new DateParser();
        objectMapper = new ObjectMapper();

        acknowledgementEmailService = new AcknowledgementEmailService(mailSender, objectMapper);

        emailProcessorService = new EmailProcessorService(
                emailReaderService,
                aiExtractionService,
                validationService,
                buyerVerificationService,
                rfqBuilderService,
                rfqApiService,
                categoryClassificationService,
                acknowledgementEmailService,
                rfqRepository,
                emailTransactionRepository,
                rfqItemRecordRepository,
                dateParser,
                objectMapper
        );

        defaultBuyer = Buyer.builder()
                .email("buyer@company.com")
                .name("Jane Buyer")
                .verified(true)
                .orgId("1")
                .userId("1")
                .address("100 Corporate Tech Park")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560103")
                .build();
        when(buyerVerificationService.verifyAndGetBuyer(anyString())).thenReturn(defaultBuyer);

        when(rfqRepository.save(any(RFQEntity.class))).thenAnswer(invocation -> {
            RFQEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(System.nanoTime());
            }
            return entity;
        });

        when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenReturn(
                RFQResponse.builder().status("SUCCESS").message("Created").build()
        );

        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenAnswer(inv -> {
            ExtractedRFQ req = inv.getArgument(0);
            return RFQRequest.builder()
                    .rfqNumber("RFQ-" + System.nanoTime())
                    .buyerEmail(req.getBuyerEmail())
                    .deliveryDate(req.getDeliveryDate())
                    .clientdeliverylocationrfq(List.of(RFQRequest.LocationDto.builder().city(req.getDeliveryLocation()).build()))
                    .rfqItem(List.of(RFQRequest.RfqItemDto.builder().description("Item").quantity(10.0).build()))
                    .build();
        });
    }

    @Test
    @DisplayName("TEST 6: 6-item email grouping into exactly 3 RFQs by location and date")
    void testSixItemsGroupingKeyRules() {
        EmailData email = EmailData.builder().messageId("MSG-TEST6").subject("Procurement").senderEmail("buyer@company.com").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop 1").category("IT Hardware").quantity(5.0).deliveryLocation("Bengaluru").deliveryDate("30-Sep-2026").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Laptop 2").category("IT Hardware").quantity(5.0).deliveryLocation("Bengaluru").deliveryDate("30-Sep-2026").build();
        RFQItem i3 = RFQItem.builder().itemDescription("Laptop 3").category("IT Hardware").quantity(5.0).deliveryLocation("Hyderabad").deliveryDate("30-Sep-2026").build();
        RFQItem i4 = RFQItem.builder().itemDescription("Laptop 4").category("IT Hardware").quantity(5.0).deliveryLocation("Bengaluru").deliveryDate("01-Oct-2026").build();
        RFQItem i5 = RFQItem.builder().itemDescription("Cement 1").category("Construction").quantity(100.0).deliveryLocation("Bengaluru").deliveryDate("30-Sep-2026").build();
        RFQItem i6 = RFQItem.builder().itemDescription("Cement 2").category("Construction").quantity(200.0).deliveryLocation("Bengaluru").deliveryDate("30-Sep-2026").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@company.com").items(new ArrayList<>(List.of(i1, i2, i3, i4, i5, i6))).build();
        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", result);

        // Verify exactly 3 RFQs are saved (grouped by location & date)
        verify(rfqRepository, times(3)).save(any(RFQEntity.class));

        // Verify ONLY 1 consolidated acknowledgement email is sent
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        assertEquals("rfq@procucev.com", mail.getFrom());
        assertEquals("buyer@company.com", mail.getTo()[0]);
        assertEquals("support@procucev.com", mail.getCc()[0]);
    }
}
