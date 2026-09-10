package com.portal.procucev.rfq.repository;

import com.portal.procucev.rfq.entity.EmailTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTransactionRepository extends JpaRepository<EmailTransaction, Long> {
    Optional<EmailTransaction> findByMessageId(String messageId);

    /** Find all pending emails for a given sender awaiting buyer registration. */
    List<EmailTransaction> findBySenderEmailIgnoreCaseAndStatus(String senderEmail, String status);

    /** Check if an email with the given messageId has already been successfully processed. */
    boolean existsByMessageIdAndStatus(String messageId, String status);
}
