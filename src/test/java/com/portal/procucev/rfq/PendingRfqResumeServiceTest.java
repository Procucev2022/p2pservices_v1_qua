package com.portal.procucev.rfq;

import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.service.DemoBuyerRegistrationService;
import com.portal.procucev.rfq.service.EmailProcessorService;
import com.portal.procucev.rfq.service.PendingRfqResumeService;
import com.portal.procucev.utils.StatusConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingRfqResumeServiceTest {

    @Mock
    private EmailTransactionRepository emailTransactionRepository;

    @Mock
    private EmailProcessorService emailProcessorService;

    @Mock
    private DemoBuyerRegistrationService demoBuyerRegistrationService;

    @Mock
    private UserDao userDao;

    @Mock
    private RfqDao rfqDao;

    @Mock
    private RFQRepository rfqRepository;

    @Mock
    private MasterStatusDao masterStatusDao;

    @InjectMocks
    private PendingRfqResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeService.setRfqDao(rfqDao);
        resumeService.setRfqRepository(rfqRepository);
        resumeService.setMasterStatusDao(masterStatusDao);
    }

    @Test
    @DisplayName("resumePendingRfqs returns 0 if buyer does not exist")
    void testResume_UserNotFound() {
        when(userDao.findByUsernameAndActive("unknown@test.com", true)).thenReturn(null);

        int count = resumeService.resumePendingRfqs("unknown@test.com");
        assertEquals(0, count);
        verifyNoInteractions(emailTransactionRepository);
    }

    @Test
    @DisplayName("resumePendingRfqs returns 0 if buyer is not fully verified")
    void testResume_UserNotVerified() {
        User user = new User();
        user.setUsername("demo@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);

        when(userDao.findByUsernameAndActive("demo@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(false);

        int count = resumeService.resumePendingRfqs("demo@test.com");
        assertEquals(0, count);
        verifyNoInteractions(emailTransactionRepository);
    }

    @Test
    @DisplayName("resumePendingRfqs returns 0 when no pending transactions found")
    void testResume_NoPendingTransactions() {
        User user = new User();
        user.setId("U1");
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(rfqDao.findIdleRfqsByUserOrOrg("U1", null)).thenReturn(Collections.emptyList());
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(Collections.emptyList());

        int count = resumeService.resumePendingRfqs("verified@test.com");
        assertEquals(0, count);
        verify(emailProcessorService, never()).processSingleEmail(any());
    }

    @Test
    @DisplayName("resumePendingRfqs activates existing idle RFQ and marks transaction RFQ_CREATED")
    void testResume_ActivatesExistingIdleRfq() {
        User user = new User();
        user.setId("U-VERIFIED");
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);
        Organization org = new Organization();
        org.setId("ORG-1");
        user.setOrg(org);

        Rfq idleRfq = new Rfq();
        idleRfq.setRfqId("RFQ-IDLE-999");
        MasterStatus idleStatus = new MasterStatus();
        idleStatus.setStatus(StatusConstants.CLIENT_RFQ_IDLE);
        idleRfq.setClientStatus(idleStatus);

        MasterStatus activeStatus = new MasterStatus();
        activeStatus.setStatus(StatusConstants.CLIENT_RFQ_NEW);

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(rfqDao.findIdleRfqsByUserOrOrg("U-VERIFIED", "ORG-1")).thenReturn(List.of(idleRfq));
        when(masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW)).thenReturn(activeStatus);

        RFQEntity entity = new RFQEntity();
        entity.setRfqNumber("RFQ-IDLE-999");
        entity.setStatus("PENDING");
        when(rfqRepository.findByRfqNumber("RFQ-IDLE-999")).thenReturn(Optional.of(entity));

        EmailTransaction tx = EmailTransaction.builder()
                .messageId("MSG-IDLE")
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(List.of(tx));

        int count = resumeService.resumePendingRfqs("verified@test.com");

        assertEquals(1, count);
        assertEquals(StatusConstants.CLIENT_RFQ_NEW, idleRfq.getClientStatus().getStatus());
        verify(rfqDao).save(idleRfq);
        verify(rfqRepository).save(argThat(e -> "SUCCESS".equals(e.getStatus())));
        verify(emailTransactionRepository).save(argThat(t -> "RFQ_CREATED".equals(t.getStatus())));
        verify(emailProcessorService, never()).processSingleEmail(any());
    }

    @Test
    @DisplayName("resumePendingRfqs successfully reprocesses pending emails when no idle RFQs exist")
    void testResume_SuccessfulReprocessing() {
        User user = new User();
        user.setId("U2");
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        EmailTransaction tx1 = EmailTransaction.builder()
                .messageId("MSG-101")
                .subject("Need valves")
                .senderEmail("verified@test.com")
                .emailBody("Please send quote for 10 gate valves")
                .attachmentText("Valve specifications")
                .attachmentPaths("non_existent_file.pdf")
                .receivedDate(LocalDateTime.now())
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(rfqDao.findIdleRfqsByUserOrOrg("U2", null)).thenReturn(Collections.emptyList());
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(List.of(tx1));
        when(emailTransactionRepository.existsByMessageIdAndStatus("MSG-101", "RFQ_CREATED")).thenReturn(false);
        when(emailProcessorService.processSingleEmail(any(EmailData.class))).thenReturn("RFQ_CREATED");

        int count = resumeService.resumePendingRfqs("verified@test.com");

        assertEquals(1, count);
        verify(emailProcessorService).processSingleEmail(argThat(email ->
                "MSG-101".equals(email.getMessageId())
                && "Need valves".equals(email.getSubject())
                && "verified@test.com".equals(email.getSenderEmail())
                && email.getBody().contains("10 gate valves")
        ));
    }

    @Test
    @DisplayName("resumePendingRfqs skips email if RFQ was already created (duplicate protection)")
    void testResume_SkipsAlreadyCreated() {
        User user = new User();
        user.setId("U3");
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        EmailTransaction tx = EmailTransaction.builder()
                .messageId("MSG-DUP")
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(rfqDao.findIdleRfqsByUserOrOrg("U3", null)).thenReturn(Collections.emptyList());
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(List.of(tx));
        when(emailTransactionRepository.existsByMessageIdAndStatus("MSG-DUP", "RFQ_CREATED")).thenReturn(true);

        int count = resumeService.resumePendingRfqs("verified@test.com");

        assertEquals(0, count);
        verify(emailProcessorService, never()).processSingleEmail(any());
        assertEquals("SKIPPED_ALREADY_PROCESSED", tx.getStatus());
    }

    @Test
    @DisplayName("resumePendingRfqs handles exception during single email processing")
    void testResume_HandlesExceptionGracefully() {
        User user = new User();
        user.setId("U4");
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        EmailTransaction tx = EmailTransaction.builder()
                .messageId("MSG-ERR")
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(rfqDao.findIdleRfqsByUserOrOrg("U4", null)).thenReturn(Collections.emptyList());
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(List.of(tx));
        when(emailTransactionRepository.existsByMessageIdAndStatus("MSG-ERR", "RFQ_CREATED")).thenReturn(false);
        when(emailProcessorService.processSingleEmail(any())).thenThrow(new RuntimeException("AI service failed"));

        int count = resumeService.resumePendingRfqs("verified@test.com");

        assertEquals(0, count);
        assertEquals("RESUME_FAILED", tx.getStatus());
        verify(emailTransactionRepository, atLeastOnce()).save(tx);
    }

    @Test
    @DisplayName("resumePendingRfqs with real existing attachment and non-success process result")
    void testResume_WithExistingAttachmentAndNonSuccess() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("rfq_test_attachment", ".txt");
        tempFile.deleteOnExit();

        User user = new User();
        user.setId("U5");
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        EmailTransaction tx = EmailTransaction.builder()
                .messageId("MSG-ATT")
                .attachmentPaths(tempFile.getAbsolutePath())
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(rfqDao.findIdleRfqsByUserOrOrg("U5", null)).thenReturn(Collections.emptyList());
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(List.of(tx));
        when(emailTransactionRepository.existsByMessageIdAndStatus("MSG-ATT", "RFQ_CREATED")).thenReturn(false);
        when(emailProcessorService.processSingleEmail(any(EmailData.class))).thenReturn("FAILED");

        int count = resumeService.resumePendingRfqs("verified@test.com");

        assertEquals(0, count);
        verify(emailProcessorService).processSingleEmail(argThat(data ->
                data.getAttachments() != null && !data.getAttachments().isEmpty()
        ));
    }

    @Test
    @DisplayName("resumePendingRfqs with null rfqDao and masterStatusDao")
    void testResume_WithNullRfqDao() {
        resumeService.setRfqDao(null);
        resumeService.setMasterStatusDao(null);

        User user = new User();
        user.setId("U6");
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(Collections.emptyList());

        int count = resumeService.resumePendingRfqs("verified@test.com");
        assertEquals(0, count);
    }

    @Test
    @DisplayName("resumePendingRfqs with idle RFQs and null rfqRepository")
    void testResume_IdleRfqsWithNullRfqRepository() {
        resumeService.setRfqRepository(null);

        User user = new User();
        user.setId("U7");
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        Rfq idleRfq = new Rfq();
        idleRfq.setRfqId("RFQ-NULL-REPO");
        MasterStatus activeStatus = new MasterStatus();
        activeStatus.setStatus(StatusConstants.CLIENT_RFQ_NEW);

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(rfqDao.findIdleRfqsByUserOrOrg("U7", null)).thenReturn(List.of(idleRfq));
        when(masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW)).thenReturn(activeStatus);
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(Collections.emptyList());

        int count = resumeService.resumePendingRfqs("verified@test.com");

        assertEquals(1, count);
        verify(rfqDao).save(idleRfq);
    }
}
