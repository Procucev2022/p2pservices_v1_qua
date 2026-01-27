package com.portal.procucev.dao;

import com.portal.procucev.model.PaymentLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {
}