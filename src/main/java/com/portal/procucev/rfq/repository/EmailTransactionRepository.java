package com.portal.procucev.rfq.repository;

import com.portal.procucev.rfq.entity.EmailTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTransactionRepository extends JpaRepository<EmailTransaction, Long> {
    Optional<EmailTransaction> findByMessageId(String messageId);
}
