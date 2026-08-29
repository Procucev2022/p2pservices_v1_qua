package com.portal.procucev.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portal.procucev.Dto.vendor.VendorDashboardDto;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.RfqVendorDao;
import com.portal.procucev.dao.SubscriptionPlanDao;
import com.portal.procucev.dao.VendorCatalogueDao;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.RfqVendor;
import com.portal.procucev.model.SubscriptionPlan;
import com.portal.procucev.model.VendorCatalogue;
import com.portal.procucev.utils.SubscriptionPlanAudience;
import com.portal.procucev.utils.SubscriptionPlanCatalogue;

/**
 * Vendor workspace data, sourced from live RFQ invitations, the vendor
 * catalogue table, and the subscription_plan table that drives Zoho payments.
 */
@Service
public class VendorDashboardServiceImpl implements VendorDashboardService {

    private static final Logger log = LoggerFactory.getLogger(VendorDashboardServiceImpl.class);

    /** Mode 3 gates: 80%+ unlocks direct dispatch, 65% - 79% is conditional. */
    private static final double PREFERRED_THRESHOLD = 80.0;
    private static final double CONDITIONAL_THRESHOLD = 65.0;

    private static final SimpleDateFormat DISPLAY_DATE = new SimpleDateFormat("dd-MMM-yyyy");

    @Autowired(required = false)
    private VendorCatalogueDao vendorCatalogueDao;

    @Autowired(required = false)
    private OrgDao orgDao;

    @Autowired(required = false)
    private RfqVendorDao rfqVendorDao;

    @Autowired(required = false)
    private SubscriptionPlanDao subscriptionPlanDao;

    @Autowired
    private SubscriptionPlanAudience planAudience;

    @Autowired
    private SubscriptionPlanCatalogue planCatalogue;

    @Override
    public VendorDashboardDto.ProfileSummaryDto getProfileSummary(String orgId, String vendorId, String username) {
        Organization org = loadOrg(orgId);

        String vendorName = org != null && org.getCompanyName() != null ? org.getCompanyName() : username;
        String vendorCode = orgId;
        String primaryCategory = org != null && org.getVendorcategory() != null ? org.getVendorcategory() : "";

        String subscription = "";
        String subscriptionPlanId = "";
        int rfqQuota = 0;
        int maxCatalogueProducts = 0;
        if (org != null && org.getSubscriptionPlan() != null) {
            SubscriptionPlan activePlan = org.getSubscriptionPlan();
            subscriptionPlanId = activePlan.getId();
            // Prefer the approved plan name so the label matches the plan cards.
            SubscriptionPlanCatalogue.Entry copy = planCatalogue.findVendorEntry(activePlan.getPlanName());
            subscription = copy != null ? copy.getName() : planAudience.displayName(activePlan);
            rfqQuota = activePlan.getRfqBundleSize();
            if ("YES".equalsIgnoreCase(activePlan.getCatalogueListing())) {
                maxCatalogueProducts = activePlan.getMaxCatalogueProducts();
            }
        }

        int activeBids = 0;
        int awardedPos = 0;
        try {
            if (rfqVendorDao != null && orgId != null && !orgId.isBlank()) {
                List<RfqVendor> invitations = rfqVendorDao.findInvitationsByVendorOrg(orgId);
                if (invitations != null) {
                    for (RfqVendor inv : invitations) {
                        if (!inv.isQuotationReceived()) {
                            activeBids++;
                        }
                    }
                }
                awardedPos = (int) rfqVendorDao.countSubmittedQuotations(orgId);
            }
        } catch (Exception e) {
            log.warn("Error counting vendor bid activity for {}: {}", orgId, e.getMessage());
        }

        return VendorDashboardDto.ProfileSummaryDto.builder()
                .vendorName(vendorName)
                .vendorCode(vendorCode)
                .primaryCategory(primaryCategory)
                .rating(0)
                .verified(org != null && org.getSubscriptionPlan() != null)
                .activeBids(activeBids)
                .awardedPos(awardedPos)
                .subscription(subscription)
                .subscriptionPlanId(subscriptionPlanId)
                .rfqQuota(rfqQuota)
                .rfqDownloadsUsed(0)
                .maxCatalogueProducts(maxCatalogueProducts)
                .build();
    }

    private Organization loadOrg(String orgId) {
        try {
            if (orgDao != null && orgId != null && !orgId.isBlank()) {
                return orgDao.findById(orgId).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Could not resolve organization {}: {}", orgId, e.getMessage());
        }
        return null;
    }

    @Override
    public List<VendorDashboardDto.OpportunityDto> getOpportunities(String orgId, String vendorId, String username) {
        List<VendorDashboardDto.OpportunityDto> list = new ArrayList<>();

        try {
            if (rfqVendorDao == null || orgId == null || orgId.isBlank()) {
                return list;
            }

            List<RfqVendor> invitations = rfqVendorDao.findInvitationsByVendorOrg(orgId);
            if (invitations == null) {
                return list;
            }

            for (RfqVendor invitation : invitations) {
                Rfq rfq = invitation.getRfq();
                if (rfq == null) {
                    continue;
                }

                Organization buyerOrg = null;
                try {
                    buyerOrg = rfq.getOrg();
                } catch (Exception e) {
                    log.debug("Buyer org unavailable for RFQ {}: {}", rfq.getRfqId(), e.getMessage());
                }

                list.add(VendorDashboardDto.OpportunityDto.builder()
                        .id(rfq.getId())
                        .rfqNumber(rfq.getRfqId())
                        .title(resolveTitle(rfq))
                        .buyer(buyerOrg != null && buyerOrg.getCompanyName() != null
                                ? buyerOrg.getCompanyName() : rfq.getUser())
                        .buyerCompany(buyerOrg != null ? buyerOrg.getCompanyName() : null)
                        .buyerContact(rfq.getUser())
                        .majorCategory(rfq.getDivision())
                        .minorCategory(rfq.getCategory())
                        .deadline(rfq.getRfqClosingDate() != null
                                ? DISPLAY_DATE.format(rfq.getRfqClosingDate()) : null)
                        .daysRemaining(daysUntil(rfq.getRfqClosingDate()))
                        // Client-uploaded roster RFQs are direct; anything else is marketplace.
                        .type(rfq.isByClient() ? "direct_invitation" : "network_marketplace")
                        .estimatedValue(null)
                        .deliveryLocation(resolveDeliveryLocation(rfq))
                        .status(invitation.isQuotationReceived() ? "submitted" : "pending_bid")
                        .lineItems(mapLineItems(rfq))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Error building vendor opportunities for {}: {}", orgId, e.getMessage());
        }

        return list;
    }

    private String resolveTitle(Rfq rfq) {
        if (rfq.getProjectDesc() != null && !rfq.getProjectDesc().isBlank()) {
            return rfq.getProjectDesc();
        }
        if (rfq.getDescription() != null && !rfq.getDescription().isBlank()) {
            return rfq.getDescription();
        }
        return rfq.getRfqId();
    }

    private String resolveDeliveryLocation(Rfq rfq) {
        try {
            List<ClientDeliveryLocationRfq> locations = rfq.getClientdeliverylocationrfq();
            if (locations != null && !locations.isEmpty()) {
                ClientDeliveryLocationRfq loc = locations.get(0);
                StringBuilder sb = new StringBuilder();
                if (loc.getCity() != null && !loc.getCity().isBlank()) {
                    sb.append(loc.getCity());
                }
                if (loc.getState() != null && !loc.getState().isBlank()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(loc.getState());
                }
                return sb.length() > 0 ? sb.toString() : loc.getAddress();
            }
        } catch (Exception e) {
            log.debug("Delivery location unavailable for RFQ {}: {}", rfq.getRfqId(), e.getMessage());
        }
        return null;
    }

    /**
     * Days until the RFQ closes, or -1 when no closing date is recorded.
     *
     * Returning 0 for a missing date would render every such RFQ as closing
     * today and trigger the urgent banner, so "unknown" is kept distinct from
     * "closes today".
     */
    private int daysUntil(Date closingDate) {
        if (closingDate == null) {
            return -1;
        }
        long diff = closingDate.getTime() - System.currentTimeMillis();
        if (diff <= 0) {
            return 0;
        }
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
    }

    private List<VendorDashboardDto.LineItemDto> mapLineItems(Rfq rfq) {
        List<VendorDashboardDto.LineItemDto> items = new ArrayList<>();
        try {
            List<RfqItem> rfqItems = rfq.getRfqItem();
            if (rfqItems == null) {
                return items;
            }
            for (RfqItem item : rfqItems) {
                items.add(VendorDashboardDto.LineItemDto.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unit(item.getUnitofMeasures() != null ? item.getUnitofMeasures() : "")
                        // Vendor supplies pricing and lead time; no seeded values.
                        .unitPrice(0)
                        .leadTimeDays(0)
                        .marketBandStatus("")
                        .paymentTerms("")
                        .build());
            }
        } catch (Exception e) {
            log.debug("Line items unavailable for RFQ {}: {}", rfq.getRfqId(), e.getMessage());
        }
        return items;
    }

    @Override
    public List<VendorDashboardDto.CatalogueProductDto> getCatalogue(String orgId, String vendorId) {
        List<VendorDashboardDto.CatalogueProductDto> list = new ArrayList<>();

        try {
            if (vendorCatalogueDao != null && orgId != null && !orgId.isBlank()) {
                List<VendorCatalogue> rows = vendorCatalogueDao.findCatalogueByVendor(orgId);
                if (rows != null) {
                    for (VendorCatalogue row : rows) {
                        list.add(toCatalogueDto(row));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error querying vendor catalogue for {}: {}", orgId, e.getMessage());
        }

        return list;
    }

    private VendorDashboardDto.CatalogueProductDto toCatalogueDto(VendorCatalogue row) {
        double price = row.getPricePerUom() != null ? row.getPricePerUom().doubleValue() : 0d;
        return VendorDashboardDto.CatalogueProductDto.builder()
                .id(row.getId())
                .sku(row.getId())
                .name(row.getMaterialDescription())
                .category(row.getCategory() != null ? row.getCategory() : row.getDivision())
                .specs(row.getOtherTerms())
                .moq(row.getMinOrderQuantity() != null ? row.getMinOrderQuantity() : 0)
                .unit(row.getUom())
                .unitPrice(price)
                .leadTimeDays(row.getLeadTimeForMoq() != null ? row.getLeadTimeForMoq() : 0)
                .published(true)
                .build();
    }

    @Override
    public VendorDashboardDto.CatalogueProductDto saveCatalogueProduct(
            VendorDashboardDto.CatalogueProductDto product, String orgId, String username) {

        if (vendorCatalogueDao == null || orgId == null || orgId.isBlank()) {
            throw new IllegalStateException("Vendor organization could not be resolved for catalogue save");
        }

        VendorCatalogue entity = null;
        if (product.getId() != null && !product.getId().isBlank()) {
            entity = vendorCatalogueDao.findById(product.getId()).orElse(null);
        }
        if (entity == null) {
            entity = new VendorCatalogue();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreatedBy(username);
        }

        entity.setMaterialDescription(product.getName());
        entity.setCategory(product.getCategory());
        entity.setOtherTerms(product.getSpecs());
        entity.setMinOrderQuantity(product.getMoq());
        entity.setUom(product.getUnit());
        entity.setPricePerUom(BigDecimal.valueOf(product.getUnitPrice()));
        entity.setLeadTimeForMoq(product.getLeadTimeDays());
        entity.setUser(username);

        Organization org = loadOrg(orgId);
        if (org != null) {
            entity.setOrg(org);
        }

        return toCatalogueDto(vendorCatalogueDao.save(entity));
    }

    /**
     * Vendor plans come from the same subscription_plan table that the Zoho
     * payment link service prices against, so plan ids stay payable. Only
     * seller-facing plans are returned.
     */
    @Override
    public List<VendorDashboardDto.SubscriptionPlanDto> getSubscriptionPlans(String orgId) {
        List<VendorDashboardDto.SubscriptionPlanDto> plans = new ArrayList<>();

        try {
            if (subscriptionPlanDao == null) {
                return plans;
            }
            List<SubscriptionPlan> rows = subscriptionPlanDao.findAll();
            if (rows == null) {
                return plans;
            }

            for (SubscriptionPlan plan : rows) {
                if (plan.getPlanStatus() != null && "INACTIVE".equalsIgnoreCase(plan.getPlanStatus())) {
                    continue;
                }
                if (!planAudience.isSellerPlan(plan)) {
                    continue;
                }
                plans.add(toPlanDto(plan));
            }

            // Smallest RFQ bundle first so tiers read entry level upward.
            plans.sort((a, b) -> Integer.compare(a.getQuota(), b.getQuota()));
        } catch (Exception e) {
            log.warn("Error loading vendor subscription plans: {}", e.getMessage());
        }

        return plans;
    }

    /**
     * Merges approved plan copy with the commercial facts on the database row.
     * Copy comes from the catalogue; price, quota, and launch state come from the
     * row so the plan stays payable through the Zoho link.
     */
    private VendorDashboardDto.SubscriptionPlanDto toPlanDto(SubscriptionPlan plan) {
        // All seller tiers are presented as purchasable.
        boolean available = true;
        boolean catalogueIncluded = "YES".equalsIgnoreCase(plan.getCatalogueListing());
        double effectivePrice = plan.getLaunchOfferPrice() > 0
                ? plan.getLaunchOfferPrice()
                : plan.getSubscriptionPrice();

        SubscriptionPlanCatalogue.Entry copy = planCatalogue.findVendorEntry(plan.getPlanName());

        List<String> features = new ArrayList<>();
        List<String> limitations = new ArrayList<>();

        if (copy != null) {
            features.addAll(copy.getFeatures());
            limitations.addAll(copy.getLimitations());
        } else {
            // No approved copy for this tier, so describe it from its own flags.
            addFeature(features, plan.getRfqNotification(), "RFQ notifications");
            addFeature(features, plan.getCategoryWiseRfqStatus(), "Category-wise RFQ status");
            addFeature(features, plan.getViewAllRfqs(), "View all RFQs");
            addFeature(features, plan.getWhatsappAiAssistant(), "WhatsApp AI assistant");
            addFeature(features, plan.getAutomatedAiQuotation(), "Automated AI quotation");
            addFeature(features, plan.getProductListingWithPrice(), "Product listing with price");
            addFeature(features, plan.getProductListingWithQuantity(), "Product listing with quantity");
            addFeature(features, plan.getRealTimeNegotiation(), "Real-time negotiation");
            addFeature(features, plan.getDedicatedAccountManager(), "Dedicated account manager");
        }

        // Quota and capacity are commercial facts, so always read them from the row.
        if (plan.getRfqBundleSize() > 0 && plan.getSubscriptionPeriodMonths() > 0) {
            features.add("Download up to " + plan.getRfqBundleSize() + " RFQs within "
                    + plan.getSubscriptionPeriodMonths() + " months");
        }
        if (catalogueIncluded && plan.getMaxCatalogueProducts() > 0) {
            features.add("Host up to " + plan.getMaxCatalogueProducts() + " catalogue products");
        }
        if (plan.getBuyerContactDetailsTime() != null && !plan.getBuyerContactDetailsTime().isBlank()) {
            features.add("Buyer contact details within " + plan.getBuyerContactDetailsTime());
        }

        if (!catalogueIncluded && limitations.isEmpty()) {
            limitations.add("Catalogue publishing not included in this plan");
        }

        String billing = plan.getSubscriptionPeriodMonths() > 0
                ? "per " + plan.getSubscriptionPeriodMonths() + " months"
                : "";

        return VendorDashboardDto.SubscriptionPlanDto.builder()
                .id(plan.getId())
                .name(copy != null ? copy.getName() : planAudience.displayName(plan))
                .subtext(copy != null ? copy.getSubtext() : "")
                .price(formatPrice(effectivePrice))
                .billing(billing)
                .description(copy != null ? copy.getDescription() : "")
                .badge(plan.getLaunchOfferPrice() > 0 ? "Launch Offer" : "")
                .quota(plan.getRfqBundleSize())
                .features(features)
                .limitations(limitations)
                .available(true)
                .priceNote(planAudience.priceNote(plan))
                .maxCatalogueProducts(catalogueIncluded ? plan.getMaxCatalogueProducts() : 0)
                .build();
    }


    private String formatPrice(double price) {
        return price > 0 ? "INR " + String.format("%.2f", price) : "Free";
    }

    private void addFeature(List<String> features, String flag, String label) {
        if ("YES".equalsIgnoreCase(flag)) {
            features.add(label);
        }
    }

    /**
     * Plan changes are settled through the Zoho payment link flow, so this only
     * reports the payable plan back to the caller. Activation happens in the
     * Zoho webhook once payment succeeds.
     */
    @Override
    public VendorDashboardDto.SubscribeResponseDto updateSubscription(
            VendorDashboardDto.SubscribeRequestDto request, String orgId, String username) {

        if (request == null || request.getPlanId() == null || request.getPlanId().isBlank()) {
            throw new IllegalArgumentException("Plan id is required");
        }

        SubscriptionPlan plan = null;
        if (subscriptionPlanDao != null) {
            plan = subscriptionPlanDao.findById(request.getPlanId()).orElse(null);
        }
        if (plan == null) {
            throw new IllegalArgumentException("Subscription plan not found: " + request.getPlanId());
        }
        if (!planAudience.isSellerPlan(plan)) {
            throw new IllegalArgumentException("This plan is not available to vendors");
        }

        log.info("Vendor {} (org {}) selected plan {} for payment", username, orgId, plan.getPlanName());

        return VendorDashboardDto.SubscribeResponseDto.builder()
                .planId(plan.getId())
                .planName(planAudience.displayName(plan))
                .quota(plan.getRfqBundleSize())
                .status("PAYMENT_REQUIRED")
                .message("Proceed to payment to activate " + planAudience.displayName(plan))
                .build();
    }

    @Override
    public VendorDashboardDto.RfqDownloadResponseDto downloadRfqDocuments(
            VendorDashboardDto.RfqDownloadRequestDto request, String orgId, String username) {

        if (request == null || request.getRfqNumber() == null || request.getRfqNumber().isBlank()) {
            throw new IllegalArgumentException("RFQ number is required");
        }

        Organization org = loadOrg(orgId);
        int quota = org != null && org.getSubscriptionPlan() != null
                ? org.getSubscriptionPlan().getRfqBundleSize() : 0;

        log.info("Dispatching BOQ documents for {} to vendor {}", request.getRfqNumber(), username);

        return VendorDashboardDto.RfqDownloadResponseDto.builder()
                .rfqNumber(request.getRfqNumber())
                .dispatched(true)
                .dispatchedTo(username)
                .downloadsUsed(0)
                .quota(quota)
                .message("Technical specifications and BOQ dispatched by email")
                .build();
    }

    @Override
    public VendorDashboardDto.QuotationResponseDto submitQuotation(
            VendorDashboardDto.QuotationRequestDto request, String orgId, String username) {

        if (request == null || request.getRfqNumber() == null || request.getRfqNumber().isBlank()) {
            throw new IllegalArgumentException("RFQ number is required");
        }

        // Recompute the total server-side rather than trusting the posted figure.
        double total = 0d;
        if (request.getLineItems() != null) {
            for (VendorDashboardDto.QuotationLineItemDto item : request.getLineItems()) {
                total += item.getUnitPrice() * item.getQuantity();
            }
        }

        String quotationId = "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Vendor {} submitted quotation {} for RFQ {} totalling {}",
                username, quotationId, request.getRfqNumber(), total);

        return VendorDashboardDto.QuotationResponseDto.builder()
                .quotationId(quotationId)
                .rfqNumber(request.getRfqNumber())
                .status("SUBMITTED")
                .totalAmount(total)
                .message("Quotation submitted successfully. The buyer has been notified.")
                .build();
    }

    @Override
    public VendorDashboardDto.QualificationResponseDto submitQualification(
            VendorDashboardDto.QualificationRequestDto request, String orgId, String username) {

        if (request == null || request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("Qualification answers are required");
        }

        // Recompute from submitted answers so the gating score cannot be spoofed.
        double computed = 0d;
        for (VendorDashboardDto.QualificationAnswerDto answer : request.getQuestions()) {
            computed += answer.getWeightedScore();
        }
        double score = Math.round(computed);

        String status = resolveQualificationStatus(score);
        Organization org = loadOrg(orgId);

        Map<String, VendorDashboardDto.ModuleScoreDto> moduleScores =
                request.getModuleScores() != null ? request.getModuleScores() : new LinkedHashMap<>();

        log.info("Vendor {} (org {}) submitted Mode 3 qualification scoring {}% -> {}",
                username, orgId, score, status);

        return VendorDashboardDto.QualificationResponseDto.builder()
                .id("eval-" + UUID.randomUUID().toString().substring(0, 8))
                .vendorName(org != null && org.getCompanyName() != null
                        ? org.getCompanyName() : request.getVendorName())
                .category(org != null && org.getGmtName() != null ? org.getGmtName() : "")
                .submissionDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .status(status)
                .overallScore(score)
                .moduleScores(moduleScores)
                .systemAction(resolveSystemAction(score))
                .questionBreakdown(request.getQuestions())
                .build();
    }

    private String resolveQualificationStatus(double score) {
        if (score >= PREFERRED_THRESHOLD) { return "PREFERRED ENTERPRISE SUPPLIER"; }
        if (score >= CONDITIONAL_THRESHOLD) { return "CONDITIONAL / UNDER REVIEW"; }
        return "DISQUALIFIED SUPPLIER";
    }

    private String resolveSystemAction(double score) {
        if (score >= PREFERRED_THRESHOLD) {
            return "Automatic direct RFQ dispatch to vendor inbox enabled. "
                    + "Vendor added to the Mode 3 active bidding roster.";
        }
        if (score >= CONDITIONAL_THRESHOLD) {
            return "RFQ dispatch held. A Corrective Action Plan (CAPA) or document "
                    + "clarification has been requested.";
        }
        return "Excluded from active RFQ dispatch. Re-audit option unlocks after 90 days.";
    }
}
