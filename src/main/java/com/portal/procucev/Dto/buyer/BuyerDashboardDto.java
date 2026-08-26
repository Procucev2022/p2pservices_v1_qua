package com.portal.procucev.Dto.buyer;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BuyerDashboardDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse implements Serializable {
        private static final long serialVersionUID = 1L;
        private int totalActiveRFQs;
        private int totalPendingQuotes;
        private String totalSpend;
        private int totalCalls;
        private int connectedCalls;
        private int totalWhatsApp;
        private int readWhatsApp;
        private int totalSMS;
        private int totalEmails;
        private int totalFollowupsToday;
        private String activeSubscription;
        private int remainingFreeRFQs;
        private String sourcingPlanName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedEntityDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String itemName;
        private double quantity;
        private String unit;
        private String targetDate;
        private String technicalSpecs;
        private double confidence;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteComparisonDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String vendorId;
        private String vendorName;
        private String vendorCategory;
        private double unitPrice;
        private double totalPrice;
        private int leadTimeDays;
        private double aiMatchScore;
        private boolean isBestPrice;
        private boolean isPreferred;
        private int warrantyYears;
        private String complianceStatus;
        private String paymentTerms;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorFollowUpDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String vendorId;
        private String vendorName;
        private String phone;
        private String contactPerson;
        private String callStatus;
        private String callLastAttempt;
        private String callDuration;
        private String whatsappStatus;
        private String whatsappLastAttempt;
        private String smsStatus;
        private String emailStatus;
        private String overallStatus;
        private String lastInteraction;
        private int attemptsCount;
        private String bidStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RFQFollowUpBreakdownDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String rfqNumber;
        private int totalInvited;
        private int respondedCount;
        private int callTotal;
        private int callConnected;
        private String callAvgDuration;
        private int whatsappTotal;
        private int whatsappRead;
        private int smsTotal;
        private int smsDelivered;
        private int emailTotal;
        private List<VendorFollowUpDto> vendors;
        private String nextScheduledChaser;
        private boolean autoChasingEnabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RFQPipelineItemDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String rfqNumber;
        private String title;
        private String category;
        private String sourcingMode;
        private String status;
        private int quotesCount;
        private String targetDeliveryDate;
        private double budget;
        private String createdAt;
        private Double aiScore;
        private boolean chasingActive;
        private String chaserMethod;
        private List<ExtractedEntityDto> extractedEntities;
        private List<QuoteComparisonDto> quotes;
        private RFQFollowUpBreakdownDto followUpData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveFeedItemDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String type; // call, whatsapp, sms, email, system
        private String channel;
        private String title;
        private String message;
        private String timestamp;
        private String rfqNumber;
        private String recipient;
        private String duration;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChaserRequestDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String rfqNumber;
        private String vendorName;
        private String channel; // call, whatsapp, sms, email, all
        private String customMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChaserResponseDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean success;
        private String trackingId;
        private String message;
        private String dispatchedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoApprovalRequestDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String rfqNumber;
        private String vendorName;
        private double totalAmount;
        private double unitPrice;
        private int leadTime;
        private String approverNotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoApprovalResponseDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String poNumber;
        private String rfqNumber;
        private String vendorName;
        private double totalAmount;
        private String sha256Signature;
        private String issuedDate;
        private String status;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorEvaluationDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String vendorName;
        private String category;
        private String location;
        private double overallScore;
        private double commercialScore;
        private double technicalScore;
        private double qualityScore;
        private double esgScore;
        private String riskRating;
        private String status;
        private String evaluatedDate;
        private Map<String, String> keyHighlights;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionPlanDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String code;
        private String name;
        private String price;
        private String period;
        private String description;
        private boolean isCurrent;
        private List<String> features;
        private int remainingQuota;
        private int totalQuota;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRfqRequestDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String rfqNumber;
        private String title;
        private String category;
        private String division;
        private String sourcingStrategyMode;
        private double budget;
        private String deliveryDate;
        private String specialInstruction;
        private List<ExtractedEntityDto> entities;
        private List<String> targetedVendorNames;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRfqResponseDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String rfqNumber;
        private String sourcingStrategyMode;
        private String status;
        private String message;
    }
}
