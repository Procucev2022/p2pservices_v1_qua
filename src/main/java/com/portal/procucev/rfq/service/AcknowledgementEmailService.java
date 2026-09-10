package com.portal.procucev.rfq.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.FailedRfqRequest;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.RFQItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcknowledgementEmailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Value("${rfq.acknowledgement.from:rfq@procucev.com}")
    private String mailFrom = "rfq@procucev.com";

    @Value("${rfq.acknowledgement.cc:support@procucev.com}")
    private String mailCc = "support@procucev.com";

    @Value("${rfq.acknowledgement.failure.to:govardhan.kilari@procucev.com}")
    private String failureTo = "govardhan.kilari@procucev.com";

    // CASE 1: RFQ SUCCESSFULLY CREATED
    public void sendSuccessAcknowledgement(List<RFQEntity> rfqEntities, Buyer buyer) {
        if (rfqEntities == null || rfqEntities.isEmpty()) {
            log.error("Cannot send success acknowledgement: RFQEntity list is empty.");
            return;
        }

        String buyerEmail = (buyer != null && buyer.getEmail() != null && !buyer.getEmail().isBlank())
                ? buyer.getEmail() : rfqEntities.get(0).getBuyerEmail();

        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send success acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        try {
            String buyerName = resolveBuyerName(buyer);
            String rfqNumbers = rfqEntities.stream()
                    .map(e -> com.portal.procucev.rfq.util.CommonUtil.shortenRfqNumber(e.getRfqNumber()))
                    .collect(Collectors.joining(", "));

            SimpleMailMessage mailMessage = createBaseMailMessage(buyerEmail);
            mailMessage.setSubject(getCase1Subject(rfqNumbers));
            mailMessage.setText(getCase1Body(buyerName, rfqNumbers));

            mailSender.send(mailMessage);
            log.info("CASE 1 Success acknowledgement email sent to {} for RFQs: {}", buyerEmail, rfqNumbers);
        } catch (Exception e) {
            log.error("Failed to send CASE 1 success acknowledgement email to {}: {}", buyerEmail, e.getMessage());
        }
    }

    public void sendSuccessAcknowledgement(RFQEntity rfqEntity, Buyer buyer) {
        if (rfqEntity == null) {
            log.error("Cannot send success acknowledgement: RFQEntity is null.");
            return;
        }
        sendSuccessAcknowledgement(List.of(rfqEntity), buyer);
    }

    // CASE 2: BUYER NOT REGISTERED
    public void sendUnregisteredBuyerAcknowledgement(String buyerEmail) {
        String recipient = resolveFailureRecipient();
        try {
            SimpleMailMessage mailMessage = createBaseMailMessage(recipient);
            mailMessage.setSubject(getCase2Subject());
            mailMessage.setText(getCase2Body(buyerEmail));

            mailSender.send(mailMessage);
            log.info("CASE 2 Unregistered buyer failure email sent to {} for unregistered sender '{}'", recipient, buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send CASE 2 unregistered buyer failure email to {}: {}", recipient, e.getMessage());
        }
    }

    // CASE: DEMO BUYER REGISTRATION (UNREGISTERED SENDER)
    public void sendDemoBuyerRegistrationEmail(String buyerEmail, String buyerName, String portalUrl) {
        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send demo buyer registration email: Invalid recipient email '{}'", buyerEmail);
            return;
        }
        try {
            String name = (buyerName != null && !buyerName.isBlank()) ? buyerName.trim() : "Valued Customer";
            String link = (portalUrl != null && !portalUrl.isBlank()) ? portalUrl.trim() : "https://qua.procucev.com/buyer";

            SimpleMailMessage mailMessage = createBaseMailMessage(buyerEmail.trim());
            mailMessage.setSubject("Welcome to Procucev – Complete Your Registration to Process Your RFQ");
            mailMessage.setText("Dear " + name + ",\n\n"
                    + "Thank you for reaching out to Procucev with your requirement.\n\n"
                    + "We noticed that you are not yet a registered buyer on our platform. To ensure security and match your requirement with verified suppliers, a temporary demo account has been created for you.\n\n"
                    + "Your RFQ request has been received and safely saved. To release and process your RFQ, please complete your profile verification:\n\n"
                    + "👉 Complete Registration & Profile: " + link + "\n\n"
                    + "Steps to complete:\n"
                    + "1. Log in or open the registration verification link above.\n"
                    + "2. Verify your email and mobile phone number with OTP.\n"
                    + "3. Confirm your company details and delivery pincode.\n\n"
                    + "Once your profile is completed, your RFQ will be automatically processed and sent to top-rated suppliers immediately!\n\n"
                    + "If you need any assistance, feel free to reply to this email or contact us at +91-7996170801.\n\n"
                    + "Best regards,\nTeam Procucev");

            mailSender.send(mailMessage);
            log.info("Demo buyer registration email sent successfully to {} ({})", buyerEmail, name);
        } catch (Exception e) {
            log.error("Failed to send demo buyer registration email to {}: {}", buyerEmail, e.getMessage());
        }
    }

    // CASE 3: DETAILS MISSING / PROCESSING FAILURE FOR REGISTERED BUYER
    public void sendFailureAcknowledgement(FailedRfqRequest request, Buyer buyer) {
        String buyerEmail = (buyer != null && buyer.getEmail() != null && !buyer.getEmail().isBlank())
                ? buyer.getEmail() : (request != null ? request.getBuyerEmail() : "");
        String buyerName = resolveBuyerName(buyer);

        String reason = request != null ? request.getReasonForFailure() : null;
        if (reason == null || reason.isBlank()) {
            reason = "We need a little more information to process your requirement.";
        }
        sendProcessingFailureAcknowledgement(buyerEmail, buyerName, reason);
    }

    public void sendMissingQuantityAcknowledgement(String buyerEmail, String buyerName, List<String> missingItems) {
        sendCase3DetailsMissingAcknowledgement(buyerEmail, buyerName);
    }

    public void sendConsolidatedAcknowledgement(List<RFQEntity> createdRfqs, List<String> failedItems, Buyer buyer, String rawSubject) {
        String buyerEmail = (buyer != null && buyer.getEmail() != null) ? buyer.getEmail().trim() : "";
        String buyerName = resolveBuyerName(buyer);
        boolean hasCreated = createdRfqs != null && !createdRfqs.isEmpty();
        boolean hasFailed = failedItems != null && !failedItems.isEmpty();

        if (hasCreated && !hasFailed) {
            sendSuccessAcknowledgement(createdRfqs, buyer);
        } else if (hasCreated) {
            sendSuccessAcknowledgement(createdRfqs, buyer);
            sendPartialSuccessAcknowledgement(buyerEmail, buyerName, createdRfqs, failedItems);
        } else if (describesMissingDetail(failedItems)) {
            sendCase3DetailsMissingAcknowledgement(buyerEmail, buyerName, failedItems);
        } else {
            sendProcessingFailureAcknowledgement(buyerEmail, buyerName, summariseFailures(failedItems));
        }
    }

    /**
     * True when at least one failure entry names a detail the buyer can supply, which is what the
     * CASE 3 template asks for. Entries describing a system or persistence failure return false.
     */
    private boolean describesMissingDetail(List<String> failedItems) {
        if (failedItems == null || failedItems.isEmpty()) {
            return false;
        }
        for (String entry : failedItems) {
            if (entry == null) {
                continue;
            }
            String lower = entry.toLowerCase();
            if (lower.contains("quantity") || lower.contains("location") || lower.contains("description")) {
                return true;
            }
        }
        return false;
    }

    private String summariseFailures(List<String> failedItems) {
        if (failedItems == null || failedItems.isEmpty()) {
            return "We could not create the RFQ for this requirement.";
        }
        return String.join("\n", failedItems);
    }

    public void sendCase3DetailsMissingAcknowledgement(String buyerEmail, String buyerName) {
        sendCase3DetailsMissingAcknowledgement(buyerEmail, buyerName, null);
    }

    public void sendCase3DetailsMissingAcknowledgement(String buyerEmail, String buyerName, List<String> failedItems) {
        String recipient = resolveFailureRecipient();
        try {
            String resolvedName = (buyerName != null && !buyerName.isBlank()) ? buyerName : "Valued Customer";

            SimpleMailMessage mailMessage = createBaseMailMessage(recipient);
            mailMessage.setSubject(getCase3Subject());
            mailMessage.setText(getCase3Body(resolvedName, failedItems));

            mailSender.send(mailMessage);
            log.info("CASE 3 Details missing acknowledgement email sent to {} for buyer '{}'", recipient, buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send CASE 3 details missing acknowledgement email to {}: {}", recipient, e.getMessage());
        }
    }

    public void sendProcessingFailureAcknowledgement(String buyerEmail, String buyerName, String reason) {
        String recipient = resolveFailureRecipient();
        try {
            SimpleMailMessage mailMessage = createBaseMailMessage(recipient);
            mailMessage.setSubject("⚠️ We Could Not Process Your RFQ");
            mailMessage.setText("Hi " + (buyerName == null || buyerName.isBlank() ? "Valued Customer" : buyerName) + ",\n\n"
                    + "We received your requirement, but could not process it.\n\nReason: " + reason + "\n\n"
                    + "Please reply with the missing details and we will try again.\n\nTeam Procucev");
            mailSender.send(mailMessage);
            log.info("Processing failure acknowledgement sent to {} for buyer '{}'", recipient, buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send processing failure acknowledgement to {}: {}", recipient, e.getMessage());
        }
    }

    public void sendFileSizeExceededAcknowledgement(String buyerEmail, String buyerName, String attachmentName, long maxBytes) {
        String recipient = resolveFailureRecipient();
        try {
            long maxMb = maxBytes > 0 ? maxBytes / (1024 * 1024) : 25;
            String resolvedName = (buyerName != null && !buyerName.isBlank()) ? buyerName : "Valued Customer";
            String buyerHeader = (buyerEmail != null && !buyerEmail.isBlank()) ? "Buyer Email: " + buyerEmail + "\n\n" : "";

            SimpleMailMessage mailMessage = createBaseMailMessage(recipient);
            mailMessage.setSubject("⚠️ File Size Exceeded: Could Not Process Your RFQ");
            mailMessage.setText(buyerHeader + "Hi " + resolvedName + ",\n\n"
                    + "We received your email, but we were unable to process it because an attachment"
                    + (attachmentName != null && !attachmentName.isBlank() ? " ('" + attachmentName + "')" : "")
                    + " exceeds the maximum allowed file size of " + maxMb + "MB.\n\n"
                    + "Please reduce the file size (maximum " + maxMb + "MB per attachment) or send attachments within the limit, and reply to submit your RFQ again.\n\n"
                    + "Best regards,\nTeam Procucev");

            mailSender.send(mailMessage);
            log.info("File size exceeded acknowledgement sent to {} for buyer '{}'", recipient, buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send file size exceeded acknowledgement to {}: {}", recipient, e.getMessage());
        }
    }

    private void sendPartialSuccessAcknowledgement(String buyerEmail, String buyerName,
                                                   List<RFQEntity> createdRfqs, List<String> failedItems) {
        String recipient = resolveFailureRecipient();
        try {
            String created = createdRfqs.stream()
                    .map(e -> com.portal.procucev.rfq.util.CommonUtil.formatRfqDisplayNumber(e.getRfqNumber()))
                    .collect(Collectors.joining(", "));
            String failed = failedItems == null ? "" : String.join("\n", failedItems);
            SimpleMailMessage mailMessage = createBaseMailMessage(recipient);
            mailMessage.setSubject("⚠️ Some RFQs Were Created, Some Need Attention");
            mailMessage.setText("Hi " + buyerName + ",\n\n"
                    + (buyerEmail != null && !buyerEmail.isBlank() ? "Buyer: " + buyerEmail + "\n\n" : "")
                    + "We created these RFQs: " + created + "\n\n"
                    + "The following groups could not be created:\n" + failed
                    + "\n\nPlease review the failed groups and reply with any corrections.\n\nTeam Procucev");
            mailSender.send(mailMessage);
            log.info("Partial success/failure alert sent to {} for buyer '{}'", recipient, buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send partial success acknowledgement to {}: {}", recipient, e.getMessage());
        }
    }

    public void sendDuplicateEmailAcknowledgement(String buyerEmail, String rawSubject) {
        String recipient = resolveFailureRecipient();
        try {
            String buyerHeader = (buyerEmail != null && !buyerEmail.isBlank()) ? "Buyer Email: " + buyerEmail + "\n\n" : "";
            SimpleMailMessage mailMessage = createBaseMailMessage(recipient);
            mailMessage.setSubject("Duplicate Request Received: " + (rawSubject != null && !rawSubject.isBlank() ? rawSubject : "RFQ Request"));
            mailMessage.setText(buyerHeader + "Dear Valued Customer,\n\n"
                    + "We received your email request, but our system detected that this request has already been received and processed.\n\n"
                    + "To prevent duplicate RFQ creation, no new RFQ was generated for this duplicate submission.\n\n"
                    + "Best regards,\nTeam Procucev");

            mailSender.send(mailMessage);
            log.info("Duplicate email acknowledgement sent successfully to: {} for buyer '{}'", recipient, buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send duplicate email acknowledgement to {}: {}", recipient, e.getMessage());
        }
    }

    public String resolveFailureRecipient() {
        if (failureTo != null && !failureTo.isBlank() && failureTo.contains("@")) {
            return failureTo.trim();
        }
        return "govardhan.kilari@procucev.com";
    }

    // Helper Methods & Template Strings
    private SimpleMailMessage createBaseMailMessage(String toAddress) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailFrom);
        mailMessage.setTo(toAddress);
        if (mailCc != null && !mailCc.isBlank()) {
            mailMessage.setCc(mailCc.trim());
        }
        return mailMessage;
    }

    private String resolveBuyerName(Buyer buyer) {
        if (buyer != null && buyer.getName() != null && !buyer.getName().isBlank()) {
            return buyer.getName().trim();
        }
        return "Valued Customer";
    }

    public String getCase1Subject(String rfqNumbers) {
        if (rfqNumbers != null && rfqNumbers.contains(",")) {
            return "🚀 Your RFQs are Live — Suppliers Notified!";
        }
        return "🚀 Your RFQ #" + rfqNumbers + " is Live — Suppliers Notified!";
    }

    public String getCase1Body(String buyerName, String rfqNumbers) {
        if (rfqNumbers != null && rfqNumbers.contains(",")) {
            String[] nums = rfqNumbers.split(",");
            StringBuilder sb = new StringBuilder();
            sb.append("Hi ").append(buyerName).append(",\n\n");
            sb.append("Great news! Your requirements have been converted into RFQs and sent to verified suppliers on Procucev right now.\n\n");
            sb.append("RFQs created:\n");
            for (String num : nums) {
                String cleanNum = com.portal.procucev.rfq.util.CommonUtil.shortenRfqNumber(num.trim());
                sb.append("✉️ ").append(cleanNum).append("\n");
            }
            sb.append("\n📩 Quotes typically start coming in within 24–48 hours.\n\n");
            sb.append("Need it faster or have a follow-up requirement?\n\n");
            sb.append("📞 Call: +91-7996170801\n");
            sb.append("✉️ Email: RFQ@procucev.com / support@procucev.com\n\n");
            sb.append("Just drop us your requirement anytime — we'll take it from there!\n\n");
            sb.append("Team Procucev");
            return sb.toString();
        }
        return "Hi " + buyerName + ",\n\n"
                + "Great news! Your requirement has been converted into RFQ #" + rfqNumbers + " and sent to verified suppliers on Procucev right now.\n\n"
                + "📩 Quotes typically start coming in within 24–48 hours.\n\n"
                + "Need it faster or have a follow-up requirement?\n\n"
                + "📞 Call: +91-7996170801\n"
                + "✉️ Email: RFQ@procucev.com / support@procucev.com\n\n"
                + "Just drop us your requirement anytime — we'll take it from there!\n\n"
                + "Team Procucev";
    }

    public String getCase2Subject() {
        return "🚀 Almost There! Register to Get Your RFQ Live";
    }

    public String getCase2Body() {
        return getCase2Body(null);
    }

    public String getCase2Body(String buyerEmail) {
        String buyerInfo = (buyerEmail != null && !buyerEmail.isBlank()) ? "Sender Email: " + buyerEmail + "\n\n" : "";
        return buyerInfo + "Hi there,\n\n"
                + "Thanks for reaching out! We couldn't process your requirement yet as your email isn't registered with us — but it takes less than 2 minutes to fix that.\n\n"
                + "Register now:\n\n"
                + "🌐 Portal: procucev.com/get-my-quote/\n"
                + "💬 WhatsApp: +91-70901 70801 (just say \"Hi\")\n\n"
                + "Once registered, we'll instantly push your requirement to our supplier network and get you quotes fast.\n\n"
                + "Questions? We're here to help:\n\n"
                + "📞 Call: +91-7996170801\n"
                + "✉️ Email: support@procucev.com\n\n"
                + "Team Procucev";
    }

    public String getCase3Subject() {
        return "⚡ One Quick Detail Needed to Process Your RFQ";
    }

    public String getCase3Body(String buyerName) {
        return getCase3Body(buyerName, null);
    }

    /** Cap on the number of individual line items named in the reply, to keep it readable. */
    private static final int MAX_LISTED_ITEMS = 15;

    public String getCase3Body(String buyerName, List<String> failedItems) {
        java.util.Set<String> missingBullets = new java.util.LinkedHashSet<>();
        // Item names whose quantity is missing, so a buyer with a long requirement sheet can see
        // exactly which rows to correct instead of a bare "Quantity required".
        java.util.List<String> quantityItems = new java.util.ArrayList<>();

        if (failedItems != null && !failedItems.isEmpty()) {
            for (String itemStr : failedItems) {
                if (itemStr == null) continue;
                String lower = itemStr.toLowerCase();
                if (lower.contains("quantity")) {
                    missingBullets.add("Quantity required");
                    String name = extractItemName(itemStr);
                    if (name != null && !quantityItems.contains(name)) {
                        quantityItems.add(name);
                    }
                }
                if (lower.contains("location")) {
                    missingBullets.add("Delivery location");
                }
            }
        }

        if (missingBullets.isEmpty()) {
            missingBullets.add("Quantity required");
        }

        StringBuilder bulletsSb = new StringBuilder();
        for (String bullet : missingBullets) {
            bulletsSb.append("• ").append(bullet).append("\n");
        }

        if (!quantityItems.isEmpty()) {
            bulletsSb.append("\nQuantity is missing for ")
                    .append(quantityItems.size())
                    .append(quantityItems.size() == 1 ? " item:\n" : " items:\n");
            int shown = Math.min(quantityItems.size(), MAX_LISTED_ITEMS);
            for (int i = 0; i < shown; i++) {
                bulletsSb.append("   ").append(i + 1).append(". ").append(quantityItems.get(i)).append("\n");
            }
            if (quantityItems.size() > shown) {
                bulletsSb.append("   ...and ").append(quantityItems.size() - shown).append(" more.\n");
            }
        }

        return "Hi " + buyerName + ",\n\n"
                + "We received your requirement, but need a bit more info to match you with the right suppliers fast:\n\n"
                + bulletsSb.toString() + "\n"
                + "Just reply to this email with the details, or it's even quicker on call/WhatsApp.\n\n"
                + "📞 Call: +91-7996170801\n"
                + "✉️ Email: RFQ@procucev.com\n\n"
                + "The sooner we get these, the sooner your RFQ goes live to suppliers!\n\n"
                + "Team Procucev";
    }

    /**
     * Pulls the item name out of a failure entry formatted as
     * {@code "Item: <name> | Reason: <reason>"}. Returns null for entries that are not item-scoped.
     */
    private String extractItemName(String failedItemEntry) {
        if (failedItemEntry == null) {
            return null;
        }
        if (!failedItemEntry.startsWith("Item: ")) {
            return null;
        }
        String remainder = failedItemEntry.substring("Item: ".length());
        int pipeIdx = remainder.indexOf(" | ");
        String name = pipeIdx >= 0 ? remainder.substring(0, pipeIdx) : remainder;
        name = name.trim();
        return name.isEmpty() ? null : name;
    }
}
