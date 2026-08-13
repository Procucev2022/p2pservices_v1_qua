package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.client.GeminiApiClient;
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

public class CategoryClassificationMasterUnitTest {

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
    private Buyer validBuyer;

    @BeforeEach
    void setUp() {
        emailReaderService = Mockito.mock(EmailReaderService.class);
        aiExtractionService = Mockito.mock(AIExtractionService.class);
        validationService = new ValidationService();
        buyerVerificationService = Mockito.mock(BuyerVerificationService.class);
        rfqBuilderService = Mockito.mock(RFQBuilderService.class);
        rfqApiService = Mockito.mock(RFQApiService.class);
        categoryClassificationService = new CategoryClassificationService();
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

        validBuyer = Buyer.builder()
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
        when(buyerVerificationService.verifyAndGetBuyer("buyer@company.com")).thenReturn(validBuyer);
        when(buyerVerificationService.verifyAndGetBuyer("unverified@buyer.com")).thenReturn(null);

        when(rfqRepository.save(any(RFQEntity.class))).thenAnswer(inv -> {
            RFQEntity entity = inv.getArgument(0);
            if (entity.getId() == null) entity.setId(System.nanoTime());
            return entity;
        });

        when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenReturn(RFQResponse.builder().status("SUCCESS").build());
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenAnswer(inv -> {
            ExtractedRFQ req = inv.getArgument(0);
            return RFQRequest.builder()
                    .rfqNumber("RFQ-" + System.nanoTime())
                    .buyerEmail(req.getBuyerEmail())
                    .deliveryDate(req.getDeliveryDate())
                    .clientdeliverylocationrfq(List.of(RFQRequest.LocationDto.builder().city(req.getDeliveryLocation()).build()))
                    .rfqItem(List.of(RFQRequest.RfqItemDto.builder().description("Item").quantity(10).build()))
                    .build();
        });
    }

    @Test
    @DisplayName("Test 1-5: Domain items classified into correct independent categories")
    void testItemClassificationIndependentCategories() {
        RFQItem i1 = RFQItem.builder().itemDescription("Dell Latitude 5450 Laptop").quantity(5.0).build();
        RFQItem i2 = RFQItem.builder().itemDescription("BATTERY 12V 150AH AMARON").quantity(2.0).build();
        RFQItem i3 = RFQItem.builder().itemDescription("5 Inch 60 Grit Fiber Disc").quantity(100.0).build();
        RFQItem i4 = RFQItem.builder().itemDescription("Digital Patient Monitor").quantity(3.0).build();
        RFQItem i5 = RFQItem.builder().itemDescription("OPC 53 Grade Cement").quantity(500.0).build();

        List<RFQItem> items = List.of(i1, i2, i3, i4, i5);
        categoryClassificationService.classifyItems(items);

        assertEquals("IT Hardware & Electronics", i1.getCategory());
        assertEquals("Batteries", i2.getCategory());
        assertEquals("Abrasives", i3.getCategory());
        assertEquals("Medical Equipment", i4.getCategory());
        assertEquals("Construction", i5.getCategory());
    }

    @Test
    @DisplayName("Test 19 & 20: First item category does NOT propagate to subsequent items")
    void testNoCategoryPropagation() {
        EmailData email = EmailData.builder().messageId("MSG-NO-PROP").subject("Requirement").senderEmail("buyer@company.com").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Dell Laptop").category("Laptop").quantity(5.0).deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Digital Patient Monitor").category("Medical Equipment").quantity(2.0).deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(ExtractedRFQ.builder().buyerEmail("buyer@company.com").items(new ArrayList<>(List.of(i1, i2))).build());

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", result);

        ArgumentCaptor<RFQEntity> captor = ArgumentCaptor.forClass(RFQEntity.class);
        verify(rfqRepository, times(2)).save(captor.capture());
        List<RFQEntity> saved = captor.getAllValues();

        assertNotEquals(saved.get(0).getCategory(), saved.get(1).getCategory());
    }
}
