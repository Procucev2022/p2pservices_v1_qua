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
@Table(name = "rfq_email_transactions")
public class EmailTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "status")
    private String status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Lob
    @Column(name = "extraction_json", columnDefinition = "TEXT")
    private String extractionJson;

    // ── Columns for pending-buyer email storage ──
    @Lob
    @Column(name = "email_body", columnDefinition = "LONGTEXT")
    private String emailBody;

    @Lob
    @Column(name = "attachment_text", columnDefinition = "LONGTEXT")
    private String attachmentText;

    @Column(name = "attachment_paths", length = 2000)
    private String attachmentPaths;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Backward-compatible constructor matching the original 9 fields before email body storage was added.
     */
    public EmailTransaction(Long id, String messageId, String subject, String senderEmail, String status,
                            String errorMessage, String extractionJson, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.messageId = messageId;
        this.subject = subject;
        this.senderEmail = senderEmail;
        this.status = status;
        this.errorMessage = errorMessage;
        this.extractionJson = extractionJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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
