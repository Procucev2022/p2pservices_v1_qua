package com.portal.procucev.Dto.vendor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transport objects for the vendor workspace screens (3.1 - 3.6):
 * opportunity feed, quotation submission, Mode 3 qualification,
 * item catalogue, subscriptions, and profile.
 */
public class VendorDashboardDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileSummaryDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String vendorName;
        private String vendorCode;
        private String primaryCategory;
        private double rating;
        private boolean verified;
        private int activeBids;
        private int awardedPos;
        private String subscription;
        /** Id of the active plan, used to match against the plan cards. */
        private String subscriptionPlanId;
        /** RFQ download bundle size from the active plan; 0 means direct invitations only. */
        private int rfqQuota;
        private int rfqDownloadsUsed;
        /** Products publishable on the active plan; 0 when catalogue listing is excluded. */
        private int maxCatalogueProducts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String description;
        private double quantity;
        private String unit;
        private double unitPrice;
        private int leadTimeDays;
        private String uploadedDocument;
        private String complianceDoc;
        private String marketBandStatus;
        private String paymentTerms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpportunityDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String rfqNumber;
        private String title;
        private String buyer;
        private String buyerCompany;
        private String buyerContact;
        private String majorCategory;
        private String minorCategory;
        private String deadline;
        private int daysRemaining;
        /** direct_invitation or network_marketplace */
        private String type;
        private String estimatedValue;
        private String deliveryLocation;
        private String status;
        private List<LineItemDto> lineItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CatalogueProductDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String sku;
        private String name;
        private String category;
        private String specs;
        private int moq;
        private String unit;
        private double unitPrice;
        private int leadTimeDays;
        private boolean published;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionPlanDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private String subtext;
        private String price;
        private String billing;
        private String description;
        private String badge;
        /** RFQ downloads allowed per 90-day period; 0 means direct invitations only. */
        private int quota;
        private List<String> features;
        private List<String> limitations;
        /** False when the plan is not yet launched and cannot be purchased. */
        private boolean available;
        /** Tax or convenience-charge qualifier taken from the plan name. */
        private String priceNote;
        /** Products publishable on this plan; 0 when catalogue listing is excluded. */
        private int maxCatalogueProducts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfqDownloadRequestDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String rfqNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfqDownloadResponseDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String rfqNumber;
        private boolean dispatched;
        private String dispatchedTo;
        private int downloadsUsed;
        private int quota;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotationLineItemDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String description;
        private double quantity;
        private double unitPrice;
        private double lineTotal;
        private int leadTimeDays;
        private String paymentTerms;
        private String uploadedDocument;
        private String complianceDoc;
        private String marketBandStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotationRequestDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String rfqNumber;
        private double totalAmount;
        private int maxLeadTimeDays;
        private String remarks;
        private List<QuotationLineItemDto> lineItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotationResponseDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String quotationId;
        private String rfqNumber;
        private String status;
        private double totalAmount;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualificationAnswerDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String refId;
        private String moduleId;
        private String criteria;
        private double score;
        private double weightedScore;
        private String attachmentName;
        private boolean attachmentVerified;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleScoreDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private double score;
        private double maxScore;
        private int weight;
        private double weightedScore;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualificationRequestDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String vendorCode;
        private String vendorName;
        private double overallScore;
        private String status;
        private Map<String, ModuleScoreDto> moduleScores;
        private List<QualificationAnswerDto> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualificationResponseDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String vendorName;
        private String category;
        private String submissionDate;
        private String status;
        private double overallScore;
        private Map<String, ModuleScoreDto> moduleScores;
        private String systemAction;
        private List<QualificationAnswerDto> questionBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscribeRequestDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String planId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscribeResponseDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String planId;
        private String planName;
        private int quota;
        private String status;
        private String message;
    }
}
