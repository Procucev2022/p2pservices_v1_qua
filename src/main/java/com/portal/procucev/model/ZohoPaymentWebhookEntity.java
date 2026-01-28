package com.portal.procucev.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "zoho_payment_webhooks")
@Data
public class ZohoPaymentWebhookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private String eventType;

    private String paymentLinkId;
    private String status;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String rawPayload;

    private Instant receivedAt = Instant.now();
}

