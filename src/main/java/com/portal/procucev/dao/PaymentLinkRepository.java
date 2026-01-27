package com.portal.procucev.dao;

import com.portal.procucev.model.PaymentLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {

    Optional<PaymentLink> findByZohoPaymentLinkId(String zohoPaymentLinkId);

    List<PaymentLink> findByStatusIn(List<String> statuses);

}