package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.controller.BuyerProfileCompletionController;
import com.portal.procucev.rfq.dto.BuyerProfileCompletionRequest;
import com.portal.procucev.rfq.entity.BuyerEntity;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.repository.BuyerRepository;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.repository.RfqItemRecordRepository;
import com.portal.procucev.rfq.service.*;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.service.SmsService;
import com.portal.procucev.utils.StatusConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnregisteredBuyerEmailRfqFlowTest {

    @Mock
    private EmailReaderService emailReaderService;
    @Mock
    private AIExtractionService aiExtractionService;
    @Mock
    private ValidationService validationService;
    @Mock
    private BuyerVerificationService buyerVerificationService;
    @Mock
    private RFQBuilderService rfqBuilderService;
    @Mock
    private RFQApiService rfqApiService;
    @Mock
    private CategoryClassificationService categoryClassificationService;
    @Mock
    private AcknowledgementEmailService acknowledgementEmailService;
    @Mock
    private RFQRepository rfqRepository;
    @Mock
    private EmailTransactionRepository emailTransactionRepository;
    @Mock
    private RfqItemRecordRepository rfqItemRecordRepository;
    @Mock
    private com.portal.procucev.rfq.parser.DateParser dateParser;
    @Mock
    private DemoBuyerRegistrationService demoBuyerRegistrationService;

    // Profile completion controller mocks
    @Mock
    private UserDao userDao;
    @Mock
    private ClientDao clientDao;
    @Mock
    private PincodeDao pincodeDao;
    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private SelfRegistrationService selfRegistrationService;
    @Mock
    private SmsService smsService;

    private EmailProcessorService emailProcessorService;
    private PendingRfqResumeService pendingRfqResumeService;
    private BuyerProfileCompletionController profileController;

    @BeforeEach
    void setUp() {
        emailProcessorService = new EmailProcessorService(
                emailReaderService, aiExtractionService, validationService,
                buyerVerificationService, rfqBuilderService, rfqApiService,
                categoryClassificationService, acknowledgementEmailService,
                rfqRepository, emailTransactionRepository, rfqItemRecordRepository,
                new com.portal.procucev.rfq.parser.DateParser(), new ObjectMapper()
        );
        emailProcessorService.setDemoBuyerRegistrationService(demoBuyerRegistrationService);
        ReflectionTestUtils.setField(emailProcessorService, "processedFolder", "Processed");
        ReflectionTestUtils.setField(emailProcessorService, "errorFolder", "Error");
        ReflectionTestUtils.setField(emailProcessorService, "buyerPortalUrl", "https://p2pv1dev-ana9azfph7chftea.centralindia-01.azurewebsites.net/login");

        pendingRfqResumeService = new PendingRfqResumeService(
                emailTransactionRepository, emailProcessorService,
                demoBuyerRegistrationService, userDao
        );

        profileController = new BuyerProfileCompletionController(
                userDao, clientDao, pincodeDao, buyerRepository,
                emailTransactionRepository, selfRegistrationService,
                smsService, pendingRfqResumeService
        );
    }

    @Test
    @DisplayName("End-to-End Unregistered Buyer Flow: Email Arrival -> Demo Account -> Deferred RFQ -> Profile Completion -> Auto Resume")
    void testCompleteUnregisteredBuyerLifecycle() {
        String senderEmail = "newclient@industrialcorp.com";
        String messageId = "MSG-NEW-1001";

        EmailData incomingEmail = EmailData.builder()
                .messageId(messageId)
                .subject("Need urgent quotation for 50 Ball Valves")
                .senderEmail(senderEmail)
                .senderName("Jane Doe")
                .receivedDate(new Date())
                .body("Hello, please provide pricing for 50 Ball Valves to Bengaluru.")
                .attachmentText("")
                .fileSizeExceeded(false)
                .build();

        // ══════════════════════════════════════════════════════════
        // STEP 1: Email arrives from unregistered sender
        // ══════════════════════════════════════════════════════════
        when(emailTransactionRepository.findByMessageId(messageId)).thenReturn(Optional.empty());

        // Buyer verification returns unverified
        Buyer unverifiedBuyer = Buyer.builder()
                .email(senderEmail)
                .name("Jane Doe")
                .verified(false)
                .build();
        when(buyerVerificationService.verifyAndGetBuyer(senderEmail)).thenReturn(unverifiedBuyer);

        // Demo account creation
        User demoUser = new User();
        demoUser.setId("USER-DEMO-1");
        demoUser.setUsername(senderEmail);
        demoUser.setFullName("Jane Doe");
        demoUser.setPhone("9999999991");
        demoUser.setVerificationStatus(StatusConstants.DEMO_BUYER);
        demoUser.setActive(true);

        when(demoBuyerRegistrationService.createDemoBuyer(senderEmail, "Jane Doe")).thenReturn(demoUser);

        // Process email
        String result = emailProcessorService.processSingleEmail(incomingEmail);

        // Assert Step 1 outcomes:
        assertEquals(StatusConstants.PENDING_BUYER_REGISTRATION, result);
        verifyNoInteractions(aiExtractionService); // CRITICAL: No Gemini AI call for unregistered sender
        verify(acknowledgementEmailService).sendDemoBuyerRegistrationEmail(eq(senderEmail), eq("Jane Doe"), eq("https://p2pv1dev-ana9azfph7chftea.centralindia-01.azurewebsites.net/login"));
        verify(emailReaderService).moveMessageToFolder(messageId, "Processed");
        verify(emailTransactionRepository, atLeastOnce()).save(argThat(tx ->
                StatusConstants.PENDING_BUYER_REGISTRATION.equals(tx.getStatus())
                && tx.getEmailBody().contains("50 Ball Valves")
        ));

        // ══════════════════════════════════════════════════════════
        // STEP 2: Buyer completes profile via controller
        // ══════════════════════════════════════════════════════════
        Organization org = new Organization();
        org.setId("ORG-DEMO-1");
        demoUser.setOrg(org);

        PincodeData pincodeData = new PincodeData();
        pincodeData.setPincode("560001");
        pincodeData.setCity("Bengaluru");
        pincodeData.setState("Karnataka");

        when(userDao.findByUsernameAndActive(senderEmail, true)).thenReturn(demoUser);
        when(pincodeDao.existsByPincode("560001")).thenReturn(true);
        when(pincodeDao.findByPincode("560001")).thenReturn(pincodeData);
        when(buyerRepository.findByEmailIgnoreCase(senderEmail)).thenReturn(Optional.empty());

        // Setup resume mock behavior:
        when(demoBuyerRegistrationService.isBuyerFullyVerified(demoUser)).thenReturn(true);

        EmailTransaction pendingTx = EmailTransaction.builder()
                .messageId(messageId)
                .subject("Need urgent quotation for 50 Ball Valves")
                .senderEmail(senderEmail)
                .emailBody("Hello, please provide pricing for 50 Ball Valves to Bengaluru.")
                .attachmentText("")
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus(senderEmail, StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(List.of(pendingTx));
        when(emailTransactionRepository.existsByMessageIdAndStatus(messageId, "RFQ_CREATED")).thenReturn(false);

        // Now buyer is verified for the resumed email!
        when(emailTransactionRepository.findByMessageId(messageId)).thenReturn(Optional.of(pendingTx));
        Buyer verifiedBuyer = Buyer.builder()
                .email(senderEmail)
                .userId("USER-DEMO-1")
                .orgId("ORG-DEMO-1")
                .name("Jane Doe")
                .verified(true)
                .build();
        when(buyerVerificationService.verifyAndGetBuyer(senderEmail)).thenReturn(verifiedBuyer);

        // Mock AI extraction & RFQ pipeline for resumed email:
        com.portal.procucev.rfq.model.ExtractedRFQ extractedRfq = com.portal.procucev.rfq.model.ExtractedRFQ.builder()
                .buyerEmail(senderEmail)
                .deliveryLocation("Bengaluru")
                .deliveryDate("2026-09-15")
                .items(List.of(com.portal.procucev.rfq.model.RFQItem.builder()
                        .itemDescription("Ball Valve")
                        .quantity(50.0)
                        .deliveryLocation("Bengaluru")
                        .deliveryDate("2026-09-15")
                        .build()))
                .build();
        when(aiExtractionService.extractRFQFromEmail(any())).thenReturn(extractedRfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        when(validationService.validateWithDetails(any())).thenReturn(valResult);

        com.portal.procucev.rfq.dto.RFQRequest request = com.portal.procucev.rfq.dto.RFQRequest.builder()
                .rfqNumber("RFQ-NEW-1001")
                .deliveryDate("2026-09-15")
                .build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);

        com.portal.procucev.rfq.dto.RFQResponse apiResponse = com.portal.procucev.rfq.dto.RFQResponse.builder()
                .status("SUCCESS")
                .rfqNumber("RFQ-NEW-1001")
                .build();
        when(rfqApiService.submitRFQ(request)).thenReturn(apiResponse);

        com.portal.procucev.rfq.entity.RFQEntity savedEntity = com.portal.procucev.rfq.entity.RFQEntity.builder()
                .rfqNumber("RFQ-NEW-1001")
                .buyerEmail(senderEmail)
                .build();
        when(rfqRepository.save(any())).thenReturn(savedEntity);

        // Buyer completes profile
        BuyerProfileCompletionRequest completionRequest = BuyerProfileCompletionRequest.builder()
                .email(senderEmail)
                .fullName("Jane Doe")
                .companyName("Industrial Corp")
                .phone("9876543210")
                .address1("Tech Park")
                .pincode("560001")
                .build();

        var response = profileController.completeProfile(completionRequest);

        // Assert Step 2 outcomes:
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals(StatusConstants.PROFILE_COMPLETED, demoUser.getVerificationStatus());
        assertEquals(1, response.getBody().getData().get("resumedRfqsCount"));

        // Verify that RFQ was created through the resumed pipeline!
        verify(rfqApiService).submitRFQ(request);
        verify(acknowledgementEmailService).sendConsolidatedAcknowledgement(anyList(), anyList(), any(Buyer.class), anyString());
    }
}
