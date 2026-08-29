package com.portal.procucev.utils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Presentation copy for subscription plans.
 *
 * The subscription_plan table stores commercial facts (price, quota, launched
 * status) and capability flags, but has no columns for the customer-facing
 * name, positioning line, description, or feature wording. That copy lives here
 * and is merged onto the matching database row at read time, so pricing stays
 * authoritative in the database while the wording matches the approved design.
 *
 * Entries are matched to database rows by {@link Entry#code} against the
 * leading word of plan_name.
 */
@Component
public class SubscriptionPlanCatalogue {

    /** Customer-facing copy for a single plan tier. */
    public static class Entry {
        private final String code;
        private final String name;
        private final String subtext;
        private final String description;
        private final String fallbackPrice;
        private final String fallbackBilling;
        private final List<String> features;
        private final List<String> limitations;

        Entry(String code, String name, String subtext, String description,
              String fallbackPrice, String fallbackBilling,
              List<String> features, List<String> limitations) {
            this.code = code;
            this.name = name;
            this.subtext = subtext;
            this.description = description;
            this.fallbackPrice = fallbackPrice;
            this.fallbackBilling = fallbackBilling;
            this.features = features;
            this.limitations = limitations;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSubtext() { return subtext; }
        public String getDescription() { return description; }
        /** Shown only when no database row supplies a price. */
        public String getFallbackPrice() { return fallbackPrice; }
        public String getFallbackBilling() { return fallbackBilling; }
        public List<String> getFeatures() { return features; }
        public List<String> getLimitations() { return limitations; }
    }

    private final Map<String, Entry> vendorPlans = new LinkedHashMap<>();
    private final Map<String, Entry> buyerPlans = new LinkedHashMap<>();

    public SubscriptionPlanCatalogue() {
        buildVendorPlans();
        buildBuyerPlans();
    }

    private void buildVendorPlans() {
        put(vendorPlans, new Entry(
                "PREMIUM",
                "Premium Model",
                "Client Uploaded Vendor",
                "Automatically granted to vendors uploaded by buyers. Access all direct RFQ "
                        + "invitations issued by your clients.",
                "Free",
                "included with Buyer Roster upload",
                Arrays.asList(
                        "See all RFQs from buyers who uploaded your vendor profile",
                        "Unlimited technical BOQ downloads for direct invitation RFQs",
                        "Email-based quotation tracking and status updates",
                        "Direct communication channel with inviting enterprise buyers",
                        "Verified supplier badge & compliance tracking"),
                Arrays.asList(
                        "Marketplace RFQs outside client roster require Connect or Select model upgrade")));

        put(vendorPlans, new Entry(
                "CONNECT",
                "Connect Model",
                "Marketplace Expansion",
                "Expand your market reach. Access and download open RFQs across the entire "
                        + "Procucev Network Marketplace.",
                null,
                null,
                Arrays.asList(
                        "All Premium Client-Uploaded features included",
                        "Access to full Open Network Marketplace RFQs",
                        "Instant email dispatch of technical BOQ spreadsheets & specifications",
                        "Automated category & location proximity matching alerts",
                        "Quarterly download quota tracking and log"),
                Arrays.asList(
                        "Catalogue creation restricted (upgrade to Select Model to publish products)")));

        put(vendorPlans, new Entry(
                "SELECT",
                "Select Model",
                "Item Catalogue & High Volume",
                "Complete tier for high-volume suppliers. Build your Item Catalogue and capture "
                        + "maximum marketplace demand.",
                null,
                null,
                Arrays.asList(
                        "All Premium & Connect Model features included",
                        "Vendor Item Catalogue with SKUs, MOQs & specifications",
                        "Bidirectional cross-highlighting of catalogue matches against RFQs",
                        "Priority category positioning in Buyer Mode 2 & Mode 3 matching",
                        "Bulk CSV/Excel catalogue import and export tools"),
                Arrays.asList()));

        // ELECT is the top seller tier in the live data with no counterpart in the
        // design set, so its copy is written from its own capability flags.
        put(vendorPlans, new Entry(
                "ELECT",
                "Elect Model",
                "Enterprise Supplier Desk",
                "Highest supplier tier. Adds live negotiation, quantity-level listings, and a "
                        + "dedicated account manager on top of the full catalogue suite.",
                null,
                null,
                Arrays.asList(
                        "Everything in the Select Model",
                        "Real-time negotiation with buyers on open RFQs",
                        "Product listings with both price and available quantity",
                        "Dedicated account manager for onboarding and disputes"),
                Arrays.asList()));
    }

    private void buildBuyerPlans() {
        put(buyerPlans, new Entry(
                "VERSION 1",
                "Version 1 Sourcing",
                "Roster-Based Chasing",
                "Streamline procurement across your pre-approved roster with automated "
                        + "working-hour follow-up pipelines.",
                "$199",
                "per user / month",
                Arrays.asList(
                        "Direct Sourcing from uploaded Excel/Manual buyer rosters",
                        "SMS outreach sent exactly 5 mins after email dispatch",
                        "Automatic Call chasing placed after 6 working hours",
                        "WhatsApp chaser interactive prompts after 12 working hours",
                        "Skips Sundays and operates strictly 8 AM - 7 PM IST Mon-Sat",
                        "OCR Quote extraction parsed directly from incoming vendor emails",
                        "Automatic halt of chasing sequence upon quote ingestion"),
                Arrays.asList()));

        put(buyerPlans, new Entry(
                "VERSION 2",
                "Version 2 Sourcing",
                "Hybrid Sourced Network",
                "Expand your pool to Procucev Base Network suppliers. Evaluate vendors "
                        + "immediately post-quote.",
                "$499",
                "per user / month",
                Arrays.asList(
                        "All features in Version 1 included",
                        "RFQ broadcast matches Procucev Pool network partners",
                        "Intelligent RFQ Category matching & Location proximity filter",
                        "Automatic classification of Buyer Upload vs. Network pool",
                        "Vendor evaluation triggers unlocked strictly after quote receipt",
                        "Interactive evaluation surveys to verify quality metrics post-bid",
                        "Real-time proximity-based targeted pool preview in Wizard"),
                Arrays.asList()));

        put(buyerPlans, new Entry(
                "VERSION 3",
                "Version 3 Sourcing",
                "Autonomous Sourcing Desk",
                "Fully autonomous category manager desk. Full 360-degree audits and matrices "
                        + "active immediately.",
                "$999",
                "per user / month",
                Arrays.asList(
                        "All features in Version 1 & 2 included",
                        "Immediate 360-degree Vendor Audits active for all pool partners",
                        "Detailed remarks & documents OCR checked against each criteria",
                        "Interactive Comparative Quote Evaluation Matrices",
                        "Automatic PO generation & contract digital signature creation",
                        "Immutable Compliance Audit Log (SHA-256 integrity checkers)",
                        "Autonomous category agent operational monitoring Kanban desk"),
                Arrays.asList()));
    }

    private void put(Map<String, Entry> target, Entry entry) {
        target.put(entry.getCode(), entry);
    }

    /** Seller tiers in display order. */
    public List<Entry> getVendorPlans() {
        return List.copyOf(vendorPlans.values());
    }

    /** Buyer tiers in display order. */
    public List<Entry> getBuyerPlans() {
        return List.copyOf(buyerPlans.values());
    }

    /** Copy for a seller plan name, or null when the tier is not in the catalogue. */
    public Entry findVendorEntry(String planName) {
        return find(vendorPlans, planName);
    }

    /** Copy for a buyer plan name, or null when the tier is not in the catalogue. */
    public Entry findBuyerEntry(String planName) {
        return find(buyerPlans, planName);
    }

    private Entry find(Map<String, Entry> source, String planName) {
        if (planName == null) {
            return null;
        }
        String normalised = planName.trim().toUpperCase();
        for (Map.Entry<String, Entry> candidate : source.entrySet()) {
            if (normalised.startsWith(candidate.getKey())) {
                return candidate.getValue();
            }
        }
        return null;
    }
}
