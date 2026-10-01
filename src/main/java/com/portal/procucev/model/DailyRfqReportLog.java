package com.portal.procucev.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_rfq_report_log")
public class DailyRfqReportLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    @Column(name = "web_portal_count", nullable = false)
    private int webPortalCount;

    @Column(name = "whatsapp_count", nullable = false)
    private int whatsappCount;

    @Column(name = "email_count", nullable = false)
    private int emailCount;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Lob
    @Column(name = "rfq_ids_json", columnDefinition = "TEXT")
    private String rfqIdsJson;

    @Column(name = "recipient", length = 255)
    private String recipient;

    @Column(name = "dispatched_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dispatchedAt;
}
