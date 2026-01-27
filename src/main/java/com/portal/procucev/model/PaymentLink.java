package com.portal.procucev.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_links")
@Data
public class PaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private SubscriptionPlan plan;

    @ManyToOne
    private Organization organization;

    private String userId;

    private BigDecimal amount;

    @Column(name = "zoho_payment_link_id")
    private String zohoPaymentLinkId;

    private String paymentUrl;

    private String status;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String zohoRawWebhook;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String zohoJobRawResponse;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
