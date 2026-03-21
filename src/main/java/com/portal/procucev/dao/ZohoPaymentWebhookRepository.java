package com.portal.procucev.dao;

import com.portal.procucev.model.ZohoPaymentWebhookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZohoPaymentWebhookRepository extends JpaRepository<ZohoPaymentWebhookEntity, Long> {
	ZohoPaymentWebhookEntity findByEventId(Long eventId);
}
