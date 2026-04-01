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

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean processed;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) this.receivedAt = Instant.now();
        this.processed = false;
    }
}

