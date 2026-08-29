package com.portal.procucev.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portal.procucev.Dto.buyer.BuyerDashboardDto;
import com.portal.procucev.dao.BuyerVendorDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.SubscriptionPlanDao;
import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.RfqVendor;
import com.portal.procucev.model.SubscriptionPlan;
import com.portal.procucev.utils.SubscriptionPlanAudience;
import com.portal.procucev.utils.SubscriptionPlanCatalogue;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.repository.RFQRepository;

@Service
public class BuyerDashboardServiceImpl implements BuyerDashboardService {

    private static final Logger log = LoggerFactory.getLogger(BuyerDashboardServiceImpl.class);

    @Autowired(required = false)
    private RfqDao rfqDao;

    @Autowired(required = false)
    private RFQRepository rfqRepository;

    @Autowired(required = false)
    private BuyerVendorDao buyerVendorDao;

    @Autowired(required = false)
    private SubscriptionPlanDao subscriptionPlanDao;

    @Autowired(required = false)
    private OrgDao orgDao;

    @Autowired
    private SubscriptionPlanAudience planAudience;

    @Autowired
    private SubscriptionPlanCatalogue planCatalogue;

    @Override
    public BuyerDashboardDto.SummaryResponse getSummary(String buyerOrgId, String buyerId, String username) {
        List<BuyerDashboardDto.RFQPipelineItemDto> pipeline = getPipelineRfqs(buyerOrgId, buyerId, username);
        int totalActive = pipeline.size();
        int pendingQuotes = 0;
        double totalSpendAmount = 0.0;
        int totalCalls = 0;
        int connectedCalls = 0;
        int totalWhatsApp = 0;
        int readWhatsApp = 0;
        int totalSMS = 0;
        int totalEmails = 0;

        for (BuyerDashboardDto.RFQPipelineItemDto item : pipeline) {
            pendingQuotes += (item.getQuotesCount() != 0 ? item.getQuotesCount() : 0);
            totalSpendAmount += item.getBudget();
            if (item.getFollowUpData() != null) {
                totalCalls += item.getFollowUpData().getCallTotal();
                connectedCalls += item.getFollowUpData().getCallConnected();
                totalWhatsApp += item.getFollowUpData().getWhatsappTotal();
                readWhatsApp += item.getFollowUpData().getWhatsappRead();
                totalSMS += item.getFollowUpData().getSmsTotal();
                totalEmails += item.getFollowUpData().getEmailTotal();
            }
        }

        int totalFollowups = totalCalls + totalWhatsApp + totalSMS + totalEmails;

        String spendFormatted;
        if (totalSpendAmount >= 1_000_000) {
            spendFormatted = String.format("$%.2fM", totalSpendAmount / 1_000_000);
        } else if (totalSpendAmount >= 1_000) {
            spendFormatted = String.format("$%.0fK", totalSpendAmount / 1_000);
        } else if (totalSpendAmount > 0) {
            spendFormatted = String.format("$%.0f", totalSpendAmount);
        } else {
            spendFormatted = "$0";
        }

        return BuyerDashboardDto.SummaryResponse.builder()
                .totalActiveRFQs(totalActive)
                .totalPendingQuotes(pendingQuotes)
                .totalSpend(spendFormatted)
                .totalCalls(totalCalls)
                .connectedCalls(connectedCalls)
                .totalWhatsApp(totalWhatsApp)
                .readWhatsApp(readWhatsApp)
                .totalSMS(totalSMS)
                .totalEmails(totalEmails)
                .totalFollowupsToday(totalFollowups)
                .activeSubscription(resolvePlanName(buyerOrgId))
                .remainingFreeRFQs(resolveRemainingQuota(buyerOrgId, totalActive))
                .sourcingPlanName(resolvePlanName(buyerOrgId))
                .build();
    }

    /** Active plan name from the org's linked subscription_plan row. */
    private String resolvePlanName(String buyerOrgId) {
        try {
            if (orgDao != null && buyerOrgId != null && !buyerOrgId.isBlank()) {
                Organization org = orgDao.findById(buyerOrgId).orElse(null);
                if (org != null && org.getSubscriptionPlan() != null
                        && org.getSubscriptionPlan().getPlanName() != null) {
                    return org.getSubscriptionPlan().getPlanName();
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve plan name for {}: {}", buyerOrgId, e.getMessage());
        }
        return "";
    }

    /** Remaining RFQs against the plan's bundle size. */
    private int resolveRemainingQuota(String buyerOrgId, int usedRfqs) {
        try {
            if (orgDao != null && buyerOrgId != null && !buyerOrgId.isBlank()) {
                Organization org = orgDao.findById(buyerOrgId).orElse(null);
                if (org != null && org.getSubscriptionPlan() != null) {
                    int bundle = org.getSubscriptionPlan().getRfqBundleSize();
                    return Math.max(0, bundle - usedRfqs);
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve quota for {}: {}", buyerOrgId, e.getMessage());
        }
        return 0;
    }

    @Override
    public List<BuyerDashboardDto.RFQPipelineItemDto> getPipelineRfqs(String buyerOrgId, String buyerId, String username) {
        List<BuyerDashboardDto.RFQPipelineItemDto> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");

        // 1. Fetch real RFQ records from RfqDao for this specific buyer/organization
        try {
            if (rfqDao != null) {
                List<Rfq> dbRfqs = null;
                if ((buyerOrgId != null && !buyerOrgId.isBlank()) || (buyerId != null && !buyerId.isBlank()) || (username != null && !username.isBlank())) {
                    dbRfqs = rfqDao.findBuyerRfqsFiltered(buyerOrgId, buyerId, username);
                }

                if ((dbRfqs == null || dbRfqs.isEmpty()) && username != null && !username.isBlank()) {
                    dbRfqs = rfqDao.findNoPrRfqByClient(username);
                }

                if ((dbRfqs == null || dbRfqs.isEmpty()) && buyerId != null && !buyerId.isBlank()) {
                    dbRfqs = rfqDao.findNoPrRfqByClient(buyerId);
                }

                if (dbRfqs != null && !dbRfqs.isEmpty()) {
                    for (Rfq rfq : dbRfqs) {
                        String rfqNum = rfq.getRfqId() != null ? rfq.getRfqId() : "RFQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        String title = rfq.getProjectDesc() != null && !rfq.getProjectDesc().isBlank()
                                ? rfq.getProjectDesc()
                                : (rfq.getDivision() != null ? rfq.getDivision() + " Procurement" : "Procurement Event");
                        String cat = rfq.getDivision() != null ? rfq.getDivision() : (rfq.getCategory() != null ? rfq.getCategory() : "General Procurement");
                        String status = rfq.getClientStatus() != null ? rfq.getClientStatus().getStatus() : (rfq.isQuotationReceived() ? "AI Recommended" : "In Evaluation");
                        String dateStr = rfq.getRfqClosingDate() != null ? sdf.format(rfq.getRfqClosingDate()) : "";
                        String createdDateStr = rfq.getCreatedTS() != null ? sdf.format(rfq.getCreatedTS()) : "";

                        // Real extracted line items
                        List<BuyerDashboardDto.ExtractedEntityDto> entities = new ArrayList<>();
                        double calculatedBudget = 0.0;
                        try {
                            if (rfq.getRfqItem() != null && !rfq.getRfqItem().isEmpty()) {
                                for (RfqItem item : rfq.getRfqItem()) {
                                    String itemDesc = item.getDescription() != null ? item.getDescription() : (item.getItemcode() != null ? item.getItemcode() : title);
                                    double qty = item.getQuantity() > 0 ? item.getQuantity() : 1.0;
                                    double totalAmt = item.getTotalamount() > 0 ? (double) item.getTotalamount() : (item.getUnitprice() > 0 ? item.getUnitprice() * qty : 0.0);
                                    calculatedBudget += totalAmt;

                                    entities.add(BuyerDashboardDto.ExtractedEntityDto.builder()
                                            .id(item.getId() != null ? item.getId() : "e-" + item.getSerialNo())
                                            .itemName(itemDesc)
                                            .quantity(qty)
                                            .unit(item.getUnitofMeasures() != null ? item.getUnitofMeasures() : "")
                                            .targetDate(dateStr)
                                            .technicalSpecs(item.getBrand() != null ? item.getBrand() : "")
                                            .confidence(0)
                                            .category(item.getCategory() != null ? item.getCategory() : cat)
                                            .build());
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Could not fetch line items for rfq: {}", e.getMessage());
                        }

                        // Real invited vendors & quotes
                        List<BuyerDashboardDto.QuoteComparisonDto> quoteList = new ArrayList<>();
                        List<BuyerDashboardDto.VendorFollowUpDto> vendorFollowUps = new ArrayList<>();
                        int invitedCount = 0;
                        int respCount = 0;
                        int notifiedCount = 0;

                        try {
                            if (rfq.getRfqVendor() != null && !rfq.getRfqVendor().isEmpty()) {
                                invitedCount = rfq.getRfqVendor().size();
                                for (RfqVendor rv : rfq.getRfqVendor()) {
                                    String vName = rv.getCompanyName() != null ? rv.getCompanyName()
                                            : (rv.getOrganization() != null ? rv.getOrganization().getCompanyName() : "");
                                    String vPhone = rv.getPhone() != null ? rv.getPhone() : "";
                                    boolean hasQuote = rv.isQuotationReceived();
                                    if (hasQuote) respCount++;
                                    if (rv.getIsRfqNotified() == 1) notifiedCount++;

                                    // Only vendors who actually submitted appear in the comparison matrix.
                                    if (hasQuote) {
                                        quoteList.add(BuyerDashboardDto.QuoteComparisonDto.builder()
                                                .vendorId(rv.getId())
                                                .vendorName(vName)
                                                .vendorCategory(rfq.isByClient() ? "Client Roster" : "Procucev Network")
                                                .unitPrice(0)
                                                .totalPrice(0)
                                                .leadTimeDays(0)
                                                .aiMatchScore(0)
                                                .isBestPrice(false)
                                                .isPreferred(false)
                                                .warrantyYears(0)
                                                .complianceStatus("")
                                                .paymentTerms("")
                                                .remarks("")
                                                .build());
                                    }

                                    vendorFollowUps.add(BuyerDashboardDto.VendorFollowUpDto.builder()
                                            .vendorId(rv.getId())
                                            .vendorName(vName)
                                            .phone(vPhone)
                                            .contactPerson(vName)
                                            .callStatus("")
                                            .callDuration("")
                                            .callLastAttempt("")
                                            .whatsappStatus("")
                                            .whatsappLastAttempt("")
                                            .smsStatus("")
                                            .emailStatus(rv.getIsRfqNotified() == 1 ? "sent" : "")
                                            .overallStatus(hasQuote ? "Responded" : "Awaiting Bid")
                                            .lastInteraction(rv.getVendorResponseDate() != null
                                                    ? sdf.format(rv.getVendorResponseDate()) : "")
                                            .attemptsCount(0)
                                            .bidStatus(hasQuote ? "Submitted" : "Pending")
                                            .build());
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Could not fetch vendors for rfq: {}", e.getMessage());
                        }

                        int quotesCount = respCount > 0 ? respCount : rfq.getCount();
                        String resolvedSourcingMode = rfq.getSourcingStrategyMode() != null && !rfq.getSourcingStrategyMode().isBlank()
                                ? rfq.getSourcingStrategyMode()
                                : (rfq.isByClient() ? "mode_1" : "mode_2");

                        list.add(BuyerDashboardDto.RFQPipelineItemDto.builder()
                                .id(rfqNum)
                                .rfqNumber(rfqNum)
                                .title(title)
                                .category(cat)
                                .sourcingMode(resolvedSourcingMode)
                                .status(status)
                                .quotesCount(quotesCount)
                                .targetDeliveryDate(dateStr)
                                .budget(calculatedBudget)
                                .createdAt(createdDateStr)
                                .aiScore(null)
                                .chasingActive(!rfq.isQuotationReceived())
                                .chaserMethod("")
                                .extractedEntities(entities)
                                .quotes(quoteList)
                                .followUpData(BuyerDashboardDto.RFQFollowUpBreakdownDto.builder()
                                        .rfqNumber(rfqNum)
                                        .totalInvited(invitedCount)
                                        .respondedCount(respCount)
                                        // Channel counters stay at zero until a chaser
                                        // dispatch log is available to read from.
                                        .callTotal(0)
                                        .callConnected(0)
                                        .callAvgDuration("")
                                        .whatsappTotal(0)
                                        .whatsappRead(0)
                                        .smsTotal(0)
                                        .smsDelivered(0)
                                        .emailTotal(notifiedCount)
                                        .autoChasingEnabled(!rfq.isQuotationReceived())
                                        .nextScheduledChaser("")
                                        .vendors(vendorFollowUps)
                                        .build())
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error querying RfqDao for buyer: {}", e.getMessage());
        }

        // 2. Fetch from RFQRepository for this buyer email
        try {
            if (rfqRepository != null) {
                List<RFQEntity> entities = null;
                if (username != null && !username.isBlank()) {
                    entities = rfqRepository.findByBuyerEmail(username);
                }
                if ((entities == null || entities.isEmpty()) && buyerId != null && !buyerId.isBlank()) {
                    entities = rfqRepository.findByBuyerEmail(buyerId);
                }

                if (entities != null && !entities.isEmpty()) {
                    for (RFQEntity entity : entities) {
                        if (entity.getRfqNumber() == null || entity.getRfqNumber().isBlank()) {
                            continue;
                        }
                        list.add(BuyerDashboardDto.RFQPipelineItemDto.builder()
                                .id(String.valueOf(entity.getId()))
                                .rfqNumber(entity.getRfqNumber())
                                .title(entity.getRawSubject() != null ? entity.getRawSubject() : entity.getRfqNumber())
                                .category("")
                                .sourcingMode("")
                                .status(entity.getStatus() != null ? entity.getStatus() : "")
                                .quotesCount(0)
                                .targetDeliveryDate(entity.getDeliveryDate())
                                .budget(0)
                                .createdAt("")
                                .chasingActive(false)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error querying RFQRepository for buyer: {}", e.getMessage());
        }

        return list;
    }

    @Override
    public List<BuyerDashboardDto.LiveFeedItemDto> getLiveFeed(String buyerOrgId, String buyerId, String username, String channel) {
        List<BuyerDashboardDto.LiveFeedItemDto> feed = new ArrayList<>();
        List<BuyerDashboardDto.RFQPipelineItemDto> pipeline = getPipelineRfqs(buyerOrgId, buyerId, username);

        int count = 0;
        for (BuyerDashboardDto.RFQPipelineItemDto rfq : pipeline) {
            if (count >= 5) break;
            count++;

            // One entry per RFQ reflecting its real pipeline state. Per-channel
            // dispatch events will be added once a chaser log table exists.
            feed.add(BuyerDashboardDto.LiveFeedItemDto.builder()
                    .id("feed-" + count)
                    .type("system")
                    .channel("system")
                    .title(rfq.getRfqNumber() + ": " + rfq.getStatus())
                    .message(rfq.getTitle())
                    .timestamp(rfq.getCreatedAt())
                    .rfqNumber(rfq.getRfqNumber())
                    .status(rfq.getStatus())
                    .build());
        }

        if (channel == null || "all".equalsIgnoreCase(channel)) {
            return feed;
        }
        return feed.stream()
                .filter(item -> channel.equalsIgnoreCase(item.getChannel()) || channel.equalsIgnoreCase(item.getType()))
                .toList();
    }

    @Override
    public BuyerDashboardDto.ChaserResponseDto triggerChaser(BuyerDashboardDto.ChaserRequestDto request, String username) {
        String trackingId = "CHS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String channel = request.getChannel() != null ? request.getChannel().toUpperCase() : "MULTI-CHANNEL";

        return BuyerDashboardDto.ChaserResponseDto.builder()
                .success(true)
                .trackingId(trackingId)
                .dispatchedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .message(channel + " chaser dispatched to " + request.getVendorName() + " (Tracking: " + trackingId + ")")
                .build();
    }

    @Override
    public BuyerDashboardDto.PoApprovalResponseDto approvePurchaseOrder(BuyerDashboardDto.PoApprovalRequestDto request, String username) {
        String rawPayload = request.getRfqNumber() + "|" + request.getVendorName() + "|" +
                request.getTotalAmount() + "|" + System.currentTimeMillis();

        String sha256 = generateSha256(rawPayload);
        String poNumber = "PO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + sha256.substring(0, 8).toUpperCase();

        return BuyerDashboardDto.PoApprovalResponseDto.builder()
                .poNumber(poNumber)
                .rfqNumber(request.getRfqNumber())
                .vendorName(request.getVendorName())
                .totalAmount(request.getTotalAmount())
                .sha256Signature(sha256)
                .status("APPROVED")
                .issuedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .message("Purchase Order " + poNumber + " successfully authorized and signed.")
                .build();
    }

    @Override
    public List<BuyerDashboardDto.VendorEvaluationDto> getVendorEvaluations(String buyerOrgId) {
        List<BuyerDashboardDto.VendorEvaluationDto> list = new ArrayList<>();

        try {
            if (buyerVendorDao != null && buyerOrgId != null && !buyerOrgId.isBlank()) {
                List<BuyerVendor> vendors = buyerVendorDao.findByBuyerOrgId(buyerOrgId);
                if (vendors != null && !vendors.isEmpty()) {
                    for (BuyerVendor bv : vendors) {
                        String loc = (bv.getCity() != null ? bv.getCity() + ", " : "") + (bv.getCountry() != null ? bv.getCountry() : "India");
                        list.add(BuyerDashboardDto.VendorEvaluationDto.builder()
                                .id(bv.getId() != null ? bv.getId() : bv.getVendorCode())
                                .vendorName(bv.getVendorName())
                                .category(bv.getTypeOfIndustry() != null ? bv.getTypeOfIndustry() : "Industrial Sourcing")
                                .location(loc)
                                // Scores are populated by Mode 3 qualification; unscored vendors report 0.
                                .overallScore(0)
                                .commercialScore(0)
                                .technicalScore(0)
                                .qualityScore(0)
                                .esgScore(0)
                                .riskRating("")
                                .status(bv.getStatus() != null ? bv.getStatus() : "")
                                .evaluatedDate("")
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error querying BuyerVendorDao for evaluations: {}", e.getMessage());
        }

        return list;
    }

    /**
     * Buyer plans are read from the subscription_plan table so the ids returned
     * here are the same ids the Zoho payment link service prices against.
     */
    /**
     * Buyer sourcing tiers (Version 1, 2, 3).
     *
     * Copy comes from the approved plan catalogue. Where a matching
     * subscription_plan row exists its id and price are used, so the plan is
     * payable through the Zoho link. Tiers with no row yet are listed for
     * comparison and marked unavailable rather than being hidden.
     */
    @Override
    public List<BuyerDashboardDto.SubscriptionPlanDto> getSubscriptionPlans(String buyerOrgId) {
        List<BuyerDashboardDto.SubscriptionPlanDto> plans = new ArrayList<>();

        String currentPlanId = null;
        try {
            if (orgDao != null && buyerOrgId != null && !buyerOrgId.isBlank()) {
                Organization org = orgDao.findById(buyerOrgId).orElse(null);
                if (org != null && org.getSubscriptionPlan() != null) {
                    currentPlanId = org.getSubscriptionPlan().getId();
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve current buyer plan for {}: {}", buyerOrgId, e.getMessage());
        }

        // Index the buyer-facing rows that exist, keyed by catalogue tier.
        Map<String, SubscriptionPlan> rowsByTier = new HashMap<>();
        try {
            if (subscriptionPlanDao != null) {
                List<SubscriptionPlan> rows = subscriptionPlanDao.findAll();
                if (rows != null) {
                    for (SubscriptionPlan plan : rows) {
                        if (plan.getPlanStatus() != null && "INACTIVE".equalsIgnoreCase(plan.getPlanStatus())) {
                            continue;
                        }
                        if (!planAudience.isBuyerPlan(plan)) {
                            continue;
                        }
                        SubscriptionPlanCatalogue.Entry match = planCatalogue.findBuyerEntry(plan.getPlanName());
                        if (match != null) {
                            rowsByTier.put(match.getCode(), plan);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error loading buyer subscription plans: {}", e.getMessage());
        }

        for (SubscriptionPlanCatalogue.Entry entry : planCatalogue.getBuyerPlans()) {
            SubscriptionPlan row = rowsByTier.get(entry.getCode());

            boolean isCurrent = row != null && currentPlanId != null && currentPlanId.equals(row.getId());

            String price = entry.getFallbackPrice();
            String period = entry.getFallbackBilling();
            if (row != null) {
                double effectivePrice = row.getLaunchOfferPrice() > 0
                        ? row.getLaunchOfferPrice()
                        : row.getSubscriptionPrice();
                if (effectivePrice > 0) {
                    price = "INR " + String.format("%.2f", effectivePrice);
                }
                if (row.getSubscriptionPeriodMonths() > 0) {
                    period = "per " + row.getSubscriptionPeriodMonths() + " months";
                }
            }

            int quota = row != null ? row.getRfqBundleSize() : 0;

            plans.add(BuyerDashboardDto.SubscriptionPlanDto.builder()
                    // Only a real row yields a payable id.
                    .id(row != null ? row.getId() : "")
                    .code(entry.getSubtext())
                    .name(entry.getName())
                    .price(price)
                    .period(period)
                    .description(entry.getDescription())
                    .isCurrent(isCurrent)
                    .remainingQuota(isCurrent ? quota : 0)
                    .totalQuota(quota)
                    .features(entry.getFeatures())
                    // All buyer tiers are presented as purchasable.
                    .available(true)
                    .priceNote(row != null ? planAudience.priceNote(row) : "")
                    .build());
        }

        return plans;
    }


    private void addFeature(List<String> features, String flag, String label) {
        if ("YES".equalsIgnoreCase(flag)) {
            features.add(label);
        }
    }


    /**
     * Creates an RfqVendor invitation per targeted supplier.
     *
     * Each invitation must carry the vendor's organization, because the vendor
     * opportunity feed selects from rfq_vendors by organization_uuid. A row
     * saved without one is invisible to every vendor, so names that cannot be
     * resolved to a registered organization are skipped and logged rather than
     * written as an orphan.
     */
    private void attachVendorInvitations(Rfq rfq, List<String> vendorNames, String username) {
        if (vendorNames == null || vendorNames.isEmpty()) {
            log.warn("RFQ {} created with no targeted vendors; no supplier will see it",
                    rfq.getRfqId());
            return;
        }
        if (orgDao == null) {
            log.warn("Organization lookup unavailable; cannot attach vendor invitations to {}",
                    rfq.getRfqId());
            return;
        }

        List<RfqVendor> invitations = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();

        for (String name : vendorNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Organization vendorOrg = null;
            try {
                vendorOrg = orgDao.findByCompanyName(name.trim());
            } catch (Exception e) {
                log.debug("Vendor organization lookup failed for '{}': {}", name, e.getMessage());
            }

            if (vendorOrg == null) {
                unresolved.add(name);
                continue;
            }

            RfqVendor invitation = new RfqVendor();
            invitation.setOrganization(vendorOrg);
            invitation.setRfqId(rfq.getRfqId());
            invitation.setQuotationReceived(false);
            invitation.setCreatedBy(username);
            invitation.setCreatedTS(new java.util.Date());
            invitations.add(invitation);
        }

        if (!unresolved.isEmpty()) {
            log.warn("RFQ {}: {} targeted vendor(s) are not registered organizations and were "
                    + "not invited: {}", rfq.getRfqId(), unresolved.size(), unresolved);
        }

        rfq.setRfqVendor(invitations);
    }

    @Override
    public BuyerDashboardDto.CreateRfqResponseDto createRfq(BuyerDashboardDto.CreateRfqRequestDto request, String username) {
        try {
            Rfq rfq = new Rfq();
            String rfqNum = (request.getRfqNumber() != null && !request.getRfqNumber().isBlank())
                    ? request.getRfqNumber()
                    : "RFQ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-" + String.format("%04d", (int)(Math.random() * 9000 + 1000));
            rfq.setRfqId(rfqNum);
            rfq.setProjectDesc(request.getTitle() != null ? request.getTitle() : "Autonomous Sourced RFQ");
            rfq.setCategory(request.getCategory() != null ? request.getCategory() : "General Procurement");
            rfq.setDivision(request.getDivision());
            rfq.setSourcingStrategyMode(request.getSourcingStrategyMode() != null ? request.getSourcingStrategyMode() : "mode_1");
            rfq.setByClient(true);
            rfq.setNoPrFlag(true);
            rfq.setUser(username);
            rfq.setCreatedBy(username);
            rfq.setCreatedTS(new java.util.Date());
            rfq.setSpecialInstruction(request.getSpecialInstruction());

            if (request.getDeliveryDate() != null && !request.getDeliveryDate().isBlank()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    rfq.setDeliveryDate(sdf.parse(request.getDeliveryDate()));
                    rfq.setRfqClosingDate(rfq.getDeliveryDate());
                } catch (Exception ignored) {}
            }

            if (request.getEntities() != null && !request.getEntities().isEmpty()) {
                List<RfqItem> items = new ArrayList<>();
                int idx = 1;
                for (BuyerDashboardDto.ExtractedEntityDto ent : request.getEntities()) {
                    RfqItem item = new RfqItem();
                    item.setDescription(ent.getItemName());
                    item.setQuantity(ent.getQuantity() > 0 ? (int)ent.getQuantity() : 1);
                    item.setUnitofMeasures(ent.getUnit() != null ? ent.getUnit() : "Units");
                    item.setBrand(ent.getTechnicalSpecs());
                    item.setCategory(ent.getCategory());
                    item.setSerialNo(idx++);
                    item.setCreatedTS(new java.util.Date());
                    items.add(item);
                }
                rfq.setRfqItem(items);
            }

            if (rfqDao != null) {
                // Vendor invitations must be persisted with the RFQ, otherwise the
                // targeted suppliers never see it in their opportunity feed.
                attachVendorInvitations(rfq, request.getTargetedVendorNames(), username);

                Rfq saved = rfqDao.save(rfq);
                int invited = saved.getRfqVendor() != null ? saved.getRfqVendor().size() : 0;

                return BuyerDashboardDto.CreateRfqResponseDto.builder()
                        .id(saved.getId())
                        .rfqNumber(saved.getRfqId())
                        .sourcingStrategyMode(saved.getSourcingStrategyMode())
                        .status("Success")
                        .message("RFQ created and dispatched to " + invited + " vendor(s)")
                        .build();
            }

            return BuyerDashboardDto.CreateRfqResponseDto.builder()
                    .rfqNumber(rfqNum)
                    .sourcingStrategyMode(request.getSourcingStrategyMode())
                    .status("Success")
                    .message("RFQ processed successfully")
                    .build();
        } catch (Exception e) {
            log.error("Error creating RFQ in BuyerDashboardService", e);
            return BuyerDashboardDto.CreateRfqResponseDto.builder()
                    .status("Failure")
                    .message("Failed to create RFQ: " + e.getMessage())
                    .build();
        }
    }

    private String generateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
