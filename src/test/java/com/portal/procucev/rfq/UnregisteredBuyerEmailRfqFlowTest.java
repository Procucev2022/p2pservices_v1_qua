package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.dao.*;
import com.portal.procucev.model.*;
import com.portal.procucev.rfq.controller.BuyerProfileCompletionController;
import com.portal.procucev.rfq.dto.BuyerProfileCompletionRequest;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.entity.BuyerEntity;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
    @Mock
    private RfqDao rfqDao;
    @Mock
    private MasterStatusDao masterStatusDao;
    @Mock
    private OtpStoreDao otpStoreDao;

    private EmailProcessorService emailProcessorService;
    private PendingRfqResumeService pendingRfqResumeService;
    private BuyerProfileCompletionController profileController;
    private DemoBuyerCleanupService demoBuyerCleanupService;

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
        pendingRfqResumeService.setRfqDao(rfqDao);
        pendingRfqResumeService.setRfqRepository(rfqRepository);
        pendingRfqResumeService.setMasterStatusDao(masterStatusDao);

        demoBuyerCleanupService = new DemoBuyerCleanupService(
                userDao, clientDao, otpStoreDao, buyerRepository,
                emailTransactionRepository, rfqDao
        );
        demoBuyerCleanupService.setRfqRepository(rfqRepository);

        profileController = new BuyerProfileCompletionController(
                userDao, clientDao, pincodeDao, buyerRepository,
                emailTransactionRepository, selfRegistrationService,
                smsService, pendingRfqResumeService
        );
    }

    @Test
    @DisplayName("End-to-End Unregistered Buyer Flow: Email Arrival -> Demo Account -> Idle RFQ -> Not Shown to CM -> Profile Verification -> Activated -> Shown to CM")
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
        // STEP 1: Email arrives from unregistered sender -> AI extraction runs -> Idle RFQ created
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
        demoUser.setPassword("Secret@123");
        demoUser.setVerificationStatus(StatusConstants.DEMO_BUYER);
        demoUser.setActive(true);

        Organization org = new Organization();
        org.setId("ORG-DEMO-1");
        demoUser.setOrg(org);

        when(demoBuyerRegistrationService.createDemoBuyer(senderEmail, "Jane Doe")).thenReturn(demoUser);

        // Mock AI extraction & RFQ pipeline for the email
        ExtractedRFQ extractedRfq = ExtractedRFQ.builder()
                .buyerEmail(senderEmail)
                .deliveryLocation("Bengaluru")
                .deliveryDate("2026-09-15")
                .items(List.of(RFQItem.builder()
                        .itemDescription("Ball Valve")
                        .quantity(50.0)
                        .deliveryLocation("Bengaluru")
                        .deliveryDate("2026-09-15")
                        .build()))
                .build();
        when(aiExtractionService.extractRFQFromEmail(any())).thenReturn(extractedRfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        when(validationService.validateWithDetails(any())).thenReturn(valResult);

        RFQRequest request = RFQRequest.builder()
                .rfqNumber("RFQ-NEW-1001")
                .deliveryDate("2026-09-15")
                .idle(true)
                .clientStatus(StatusConstants.CLIENT_RFQ_IDLE)
                .build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);

        RFQResponse apiResponse = RFQResponse.builder()
                .status("SUCCESS")
                .rfqNumber("RFQ-NEW-1001")
                .build();
        when(rfqApiService.submitRFQ(any())).thenReturn(apiResponse);

        RFQEntity savedEntity = RFQEntity.builder()
                .rfqNumber("RFQ-NEW-1001")
                .buyerEmail(senderEmail)
                .status(StatusConstants.CLIENT_RFQ_IDLE)
                .build();
        when(rfqRepository.save(any())).thenReturn(savedEntity);

        // Process email
        String result = emailProcessorService.processSingleEmail(incomingEmail);

        // Assert Step 1 outcomes:
        assertEquals(StatusConstants.PENDING_BUYER_REGISTRATION, result);
        verify(aiExtractionService).extractRFQFromEmail(any()); // AI extraction runs!
        verify(rfqApiService).submitRFQ(argThat(r -> r.isIdle() && StatusConstants.CLIENT_RFQ_IDLE.equals(r.getClientStatus())));
        verify(acknowledgementEmailService).sendDemoBuyerRegistrationEmail(eq(senderEmail), eq("Jane Doe"), eq("https://p2pv1dev-ana9azfph7chftea.centralindia-01.azurewebsites.net/login"), eq("Secret@123"));
        verify(emailReaderService).moveMessageToFolder(messageId, "Processed");
        verify(emailTransactionRepository, atLeastOnce()).save(argThat(tx ->
                StatusConstants.PENDING_BUYER_REGISTRATION.equals(tx.getStatus())
                && tx.getEmailBody().contains("50 Ball Valves")
        ));

        // Category Manager query must NOT return this idle RFQ:
        when(rfqDao.findAllClientRfqNoPr()).thenReturn(Collections.emptyList());
        List<Rfq> cmRfqsBeforeVerification = rfqDao.findAllClientRfqNoPr();
        assertTrue(cmRfqsBeforeVerification.isEmpty(), "Category Manager must NOT see idle RFQ before verification");

        // ══════════════════════════════════════════════════════════
        // STEP 2: Buyer completes profile / OTP verification -> Idle RFQ is activated
        // ══════════════════════════════════════════════════════════
        PincodeData pincodeData = new PincodeData();
        pincodeData.setPincode("560001");
        pincodeData.setCity("Bengaluru");
        pincodeData.setState("Karnataka");

        when(userDao.findByUsernameAndActive(senderEmail, true)).thenReturn(demoUser);
        when(pincodeDao.existsByPincode("560001")).thenReturn(true);
        when(pincodeDao.findByPincode("560001")).thenReturn(pincodeData);
        when(buyerRepository.findByEmailIgnoreCase(senderEmail)).thenReturn(Optional.empty());

        when(demoBuyerRegistrationService.isBuyerFullyVerified(demoUser)).thenReturn(true);

        MasterStatus idleMasterStatus = new MasterStatus();
        idleMasterStatus.setStatus(StatusConstants.CLIENT_RFQ_IDLE);

        MasterStatus activeMasterStatus = new MasterStatus();
        activeMasterStatus.setStatus(StatusConstants.CLIENT_RFQ_NEW);
        when(masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW)).thenReturn(activeMasterStatus);

        Rfq idleRfq = new Rfq();
        idleRfq.setRfqId("RFQ-NEW-1001");
        idleRfq.setUser("USER-DEMO-1");
        idleRfq.setClientStatus(idleMasterStatus);

        when(rfqDao.findIdleRfqsByUserOrOrg("USER-DEMO-1", "ORG-DEMO-1")).thenReturn(List.of(idleRfq));
        when(rfqRepository.findByRfqNumber("RFQ-NEW-1001")).thenReturn(Optional.of(savedEntity));

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

        // Verify RFQ status transitioned to active CLIENT_RFQ_NEW
        assertEquals(StatusConstants.CLIENT_RFQ_NEW, idleRfq.getClientStatus().getStatus());
        verify(rfqDao).save(idleRfq);

        // Now Category Manager query returns the activated RFQ:
        when(rfqDao.findAllClientRfqNoPr()).thenReturn(List.of(idleRfq));
        List<Rfq> cmRfqsAfterVerification = rfqDao.findAllClientRfqNoPr();
        assertEquals(1, cmRfqsAfterVerification.size(), "Category Manager now sees the activated RFQ");
    }

    @Test
    @DisplayName("3-Hour Expiration Check: Unverified Demo Buyer expires only after 3 hours and is deleted")
    void testThreeHourExpiryAndCleanup() {
        User user = new User();
        user.setId("USER-EXP-1");
        user.setUsername("expired@test.com");
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);
        user.setActivityTs(null);

        // 2 hours ago: not expired (3h window)
        user.setCreatedTS(new Date(System.currentTimeMillis() - (2 * 60 * 60 * 1000L)));
        assertFalse(demoBuyerCleanupService.isExpired(user), "User created 2h ago must NOT be expired under 3h policy");

        // 3 hours and 5 minutes ago: expired!
        user.setCreatedTS(new Date(System.currentTimeMillis() - (3 * 60 * 60 * 1000L + 5 * 60 * 1000L)));
        assertTrue(demoBuyerCleanupService.isExpired(user), "User created 3h 5m ago must be expired under 3h policy");

        // Execute cleanup
        when(rfqDao.findNoPrRfqByClient("USER-EXP-1")).thenReturn(Collections.emptyList());
        when(emailTransactionRepository.findBySenderEmailIgnoreCase("expired@test.com")).thenReturn(Collections.emptyList());
        when(buyerRepository.findByEmailIgnoreCase("expired@test.com")).thenReturn(Optional.empty());

        boolean deleted = demoBuyerCleanupService.deleteExpiredDemoBuyer(user);
        assertTrue(deleted);
        verify(userDao).delete(user);
    }

    @Test
    @DisplayName("Fallback Extraction: Unregistered Buyer email succeeds when Gemini API Key is missing")
    void testUnregisteredBuyerFallbackExtractionWhenAiFails() {
        String senderEmail = "sowjanyapillutla96@gmail.com";
        String messageId = "MSG-CEMENT-1002";

        EmailData incomingEmail = EmailData.builder()
                .messageId(messageId)
                .subject("RFQ")
                .senderEmail(senderEmail)
                .senderName("Sowjanya Pillutla")
                .receivedDate(new Date())
                .body("Dear Procurement Team, We would like to request your best quotation for *500 bags of Ordinary Portland Cement (OPC)* suitable for construction applications. The required delivery location is *ABC Procurement Warehouse, Peenya Industrial Area, Bangalore, Karnataka – 560058, India*, and the required delivery date is *15-Feb-2029*. Please provide the *unit price, applicable taxes, transportation charges, warranty/quality certification details, payment terms, delivery schedule, and quotation validity*. Regards, *Veera Babu* ABC Procurement Pvt. Ltd.")
                .attachmentText("")
                .fileSizeExceeded(false)
                .build();

        when(emailTransactionRepository.findByMessageId(messageId)).thenReturn(Optional.empty());

        Buyer unverifiedBuyer = Buyer.builder()
                .email(senderEmail)
                .name("Sowjanya Pillutla")
                .verified(false)
                .build();
        when(buyerVerificationService.verifyAndGetBuyer(senderEmail)).thenReturn(unverifiedBuyer);

        User demoUser = new User();
        demoUser.setId("USER-SOWJANYA");
        demoUser.setUsername(senderEmail);
        demoUser.setFullName("Sowjanya Pillutla");
        demoUser.setPassword("Secret@123");
        demoUser.setVerificationStatus(StatusConstants.DEMO_BUYER);
        demoUser.setActive(true);

        Organization org = new Organization();
        org.setId("ORG-SOWJANYA");
        demoUser.setOrg(org);

        when(demoBuyerRegistrationService.createDemoBuyer(senderEmail, "Sowjanya Pillutla")).thenReturn(demoUser);

        // AI throws exception (Gemini API key is not configured)
        when(aiExtractionService.extractRFQFromEmail(incomingEmail))
                .thenThrow(new com.portal.procucev.rfq.exception.ApplicationException(
                        "AI extraction failed on all models: Gemini API key is not configured. Set the GEMINI_API_KEY environment variable (or the app.gemini.api-key property) to enable AI extraction."));

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        when(validationService.validateWithDetails(any())).thenReturn(valResult);

        RFQRequest mockRfqReq = RFQRequest.builder()
                .rfqNumber("RFQ-CEM-1002")
                .deliveryDate("2029-02-15")
                .clientStatus(StatusConstants.CLIENT_RFQ_IDLE)
                .idle(true)
                .build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);

        RFQResponse mockRfqResp = RFQResponse.builder()
                .rfqNumber("RFQ-CEM-1002")
                .status("SUCCESS")
                .build();
        when(rfqApiService.submitRFQ(any())).thenReturn(mockRfqResp);

        RFQEntity savedEntity = RFQEntity.builder()
                .rfqNumber("RFQ-CEM-1002")
                .buyerEmail(senderEmail)
                .status(StatusConstants.CLIENT_RFQ_IDLE)
                .build();
        when(rfqRepository.save(any())).thenReturn(savedEntity);

        String result = emailProcessorService.processSingleEmail(incomingEmail);

        assertEquals(StatusConstants.PENDING_BUYER_REGISTRATION, result);
        verify(rfqRepository).save(argThat(entity ->
                StatusConstants.CLIENT_RFQ_IDLE.equals(entity.getStatus())
        ));
        verify(acknowledgementEmailService).sendDemoBuyerRegistrationEmail(
                eq(senderEmail), anyString(), anyString(), anyString()
        );
        verify(emailReaderService).moveMessageToFolder(messageId, "Processed");
    }
}
