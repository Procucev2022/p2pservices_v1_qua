package com.portal.procucev.rfq;

import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.service.DemoBuyerRegistrationService;
import com.portal.procucev.rfq.service.EmailProcessorService;
import com.portal.procucev.rfq.service.PendingRfqResumeService;
import com.portal.procucev.utils.StatusConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

    @InjectMocks
    private PendingRfqResumeService resumeService;

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
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(Collections.emptyList());

        int count = resumeService.resumePendingRfqs("verified@test.com");
        assertEquals(0, count);
        verify(emailProcessorService, never()).processSingleEmail(any());
    }

    @Test
    @DisplayName("resumePendingRfqs successfully reprocesses pending emails")
    void testResume_SuccessfulReprocessing() {
        User user = new User();
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        EmailTransaction tx1 = EmailTransaction.builder()
                .messageId("MSG-101")
                .subject("Need valves")
                .senderEmail("verified@test.com")
                .emailBody("Please send quote for 10 gate valves")
                .attachmentText("Valve specifications")
                .receivedDate(LocalDateTime.now())
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
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
        user.setUsername("verified@test.com");
        user.setActive(true);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);

        EmailTransaction tx = EmailTransaction.builder()
                .messageId("MSG-DUP")
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();

        when(userDao.findByUsernameAndActive("verified@test.com", true)).thenReturn(user);
        when(demoBuyerRegistrationService.isBuyerFullyVerified(user)).thenReturn(true);
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("verified@test.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(List.of(tx));
        when(emailTransactionRepository.existsByMessageIdAndStatus("MSG-DUP", "RFQ_CREATED")).thenReturn(true);

        int count = resumeService.resumePendingRfqs("verified@test.com");

        assertEquals(0, count);
        verify(emailProcessorService, never()).processSingleEmail(any());
        assertEquals("SKIPPED_ALREADY_PROCESSED", tx.getStatus());
    }
}
