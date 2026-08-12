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
@Table(name = "rfq_records")
public class RFQEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_number", nullable = false, unique = true)
    private String rfqNumber;

    @Column(name = "buyer_email", nullable = false)
    private String buyerEmail;

    @Column(name = "status")
    private String status;

    @Column(name = "raw_subject")
    private String rawSubject;

    @Lob
    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "delivery_location")
    private String deliveryLocation;

    @Column(name = "delivery_date")
    private String deliveryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
