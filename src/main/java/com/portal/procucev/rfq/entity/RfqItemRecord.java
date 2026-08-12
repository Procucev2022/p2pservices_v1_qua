package com.portal.procucev.rfq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rfq_item_records", indexes = {
    @Index(name = "idx_rfq_item_lookup", columnList = "buyer_email, item_description, delivery_date")
})
public class RfqItemRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_email", nullable = false)
    private String buyerEmail;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "delivery_date")
    private String deliveryDate;

    @Column(name = "rfq_number")
    private String rfqNumber;

    @Column(name = "category")
    private String category;

    @Column(name = "division")
    private String division;

    @Column(name = "category_confidence")
    private Double categoryConfidence;

    @Column(name = "classification_status")
    private String classificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
