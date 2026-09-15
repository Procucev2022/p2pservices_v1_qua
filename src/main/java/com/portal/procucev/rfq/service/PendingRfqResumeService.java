package com.portal.procucev.rfq.service;

import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.utils.StatusConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingRfqResumeService {

    private final EmailTransactionRepository emailTransactionRepository;
    private final EmailProcessorService emailProcessorService;
    private final DemoBuyerRegistrationService demoBuyerRegistrationService;
    private final UserDao userDao;

    @Autowired(required = false)
    private RfqDao rfqDao;

    @Autowired(required = false)
    private RFQRepository rfqRepository;

    @Autowired(required = false)
    private MasterStatusDao masterStatusDao;

    public void setRfqDao(RfqDao rfqDao) {
        this.rfqDao = rfqDao;
    }

    public void setRfqRepository(RFQRepository rfqRepository) {
        this.rfqRepository = rfqRepository;
    }

    public void setMasterStatusDao(MasterStatusDao masterStatusDao) {
        this.masterStatusDao = masterStatusDao;
    }

    /**
     * Resumes all pending RFQs for a buyer who has just completed verification.
     *
     * <p>Called after the buyer's {@code verificationStatus} transitions to
     * {@code PROFILE_COMPLETED}.</p>
     *
     * @param buyerEmail the email address of the now-verified buyer
     * @return the number of emails successfully reprocessed
     */
    @Transactional
    public int resumePendingRfqs(String buyerEmail) {
        String normalizedEmail = buyerEmail.trim().toLowerCase();
        log.info("Attempting to resume pending RFQs for verified buyer: {}", normalizedEmail);

        // ── Safety check: buyer must actually be fully verified ──
        User user = userDao.findByUsernameAndActive(normalizedEmail, true);
        if (user == null) {
            log.warn("Cannot resume pending RFQs: no active user found for {}", normalizedEmail);
            return 0;
        }

        if (!demoBuyerRegistrationService.isBuyerFullyVerified(user)) {
            log.warn("Cannot resume pending RFQs: buyer {} is not fully verified (status={})",
                    normalizedEmail, user.getVerificationStatus());
            return 0;
        }

        int activatedIdleCount = 0;

        // ── 1. Activate any existing IDLE RFQs created for this buyer ──
        if (rfqDao != null && masterStatusDao != null) {
            String orgId = user.getOrg() != null ? user.getOrg().getId() : null;
            List<Rfq> idleRfqs = rfqDao.findIdleRfqsByUserOrOrg(user.getId(), orgId);
            if (idleRfqs != null && !idleRfqs.isEmpty()) {
                MasterStatus activeClientStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW);
                for (Rfq idleRfq : idleRfqs) {
                    idleRfq.setClientStatus(activeClientStatus);
                    rfqDao.save(idleRfq);
                    if (rfqRepository != null) {
                        rfqRepository.findByRfqNumber(idleRfq.getRfqId()).ifPresent(entity -> {
                            entity.setStatus("SUCCESS");
                            rfqRepository.save(entity);
                        });
                    }
                    activatedIdleCount++;
                    log.info("Activated idle RFQ {} for newly verified buyer {}", idleRfq.getRfqId(), normalizedEmail);
                }
            }
        }

        // ── 2. Find all pending email transactions ──
        List<EmailTransaction> pendingTransactions = emailTransactionRepository
                .findBySenderEmailIgnoreCaseAndStatus(normalizedEmail, StatusConstants.PENDING_BUYER_REGISTRATION);

        if (pendingTransactions.isEmpty()) {
            log.info("No pending email transactions found for {}", normalizedEmail);
            return activatedIdleCount;
        }

        log.info("Found {} pending email transaction(s) for {}. Resuming processing...",
                pendingTransactions.size(), normalizedEmail);

        int successCount = activatedIdleCount;

        for (EmailTransaction transaction : pendingTransactions) {
            try {
                // ── Duplicate protection: skip if already created ──
                if (emailTransactionRepository.existsByMessageIdAndStatus(transaction.getMessageId(), "RFQ_CREATED")) {
                    log.info("Email {} already has RFQ_CREATED status. Skipping to prevent duplicate.",
                            transaction.getMessageId());
                    transaction.setStatus("SKIPPED_ALREADY_PROCESSED");
                    emailTransactionRepository.save(transaction);
                    continue;
                }

                // If an idle RFQ was already activated for this buyer, mark the transaction as RFQ_CREATED
                if (activatedIdleCount > 0) {
                    transaction.setStatus("RFQ_CREATED");
                    emailTransactionRepository.save(transaction);
                    continue;
                }

                // ── Reconstruct EmailData from stored transaction ──
                EmailData emailData = reconstructEmailData(transaction);

                // ── Mark as being reprocessed ──
                transaction.setStatus("RESUMED_PROCESSING");
                emailTransactionRepository.save(transaction);

                // ── Delegate to the normal email processing pipeline ──
                String result = emailProcessorService.processSingleEmail(emailData);

                log.info("Resumed email {} processed with result: {}", transaction.getMessageId(), result);
                if ("RFQ_CREATED".equalsIgnoreCase(result) || result.contains("RFQ_CREATED")) {
                    successCount++;
                }

            } catch (Exception e) {
                log.error("Failed to resume processing for email {}: {}",
                        transaction.getMessageId(), e.getMessage(), e);
                transaction.setStatus("RESUME_FAILED");
                transaction.setErrorMessage("Resume failed: " + e.getMessage());
                emailTransactionRepository.save(transaction);
            }
        }

        log.info("Completed pending RFQ resume for {}: {} successful", normalizedEmail, successCount);
        return successCount;
    }

    /**
     * Reconstructs an {@link EmailData} object from the stored {@link EmailTransaction} fields.
     * This allows the normal email processing pipeline to treat it as if it just arrived.
     */
    private EmailData reconstructEmailData(EmailTransaction transaction) {
        List<File> attachmentFiles = new ArrayList<>();
        if (transaction.getAttachmentPaths() != null && !transaction.getAttachmentPaths().isBlank()) {
            String[] paths = transaction.getAttachmentPaths().split(",");
            for (String path : paths) {
                File f = new File(path.trim());
                if (f.exists()) {
                    attachmentFiles.add(f);
                } else {
                    log.warn("Stored attachment file not found: {}", path.trim());
                }
            }
        }

        Date receivedDate = null;
        if (transaction.getReceivedDate() != null) {
            receivedDate = Date.from(transaction.getReceivedDate().atZone(ZoneId.systemDefault()).toInstant());
        }

        return EmailData.builder()
                .messageId(transaction.getMessageId())
                .subject(transaction.getSubject())
                .senderEmail(transaction.getSenderEmail())
                .receivedDate(receivedDate)
                .body(transaction.getEmailBody())
                .attachmentText(transaction.getAttachmentText())
                .attachments(attachmentFiles.isEmpty() ? Collections.emptyList() : attachmentFiles)
                .fileSizeExceeded(false)
                .build();
    }
}
