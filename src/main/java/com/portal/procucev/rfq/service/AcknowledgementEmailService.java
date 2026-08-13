package com.portal.procucev.rfq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.FailedRfqRequest;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcknowledgementEmailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${rfq.acknowledgement.from:rfq@procucev.com}")
    private String mailFrom = "rfq@procucev.com";

    @org.springframework.beans.factory.annotation.Value("${rfq.acknowledgement.cc:support@procucev.com}")
    private String mailCc = "support@procucev.com";

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
        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send unregistered buyer acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        try {
            SimpleMailMessage mailMessage = createBaseMailMessage(buyerEmail);
            mailMessage.setSubject(getCase2Subject());
            mailMessage.setText(getCase2Body());

            mailSender.send(mailMessage);
            log.info("CASE 2 Unregistered buyer acknowledgement email sent to {}", buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send CASE 2 unregistered buyer email to {}: {}", buyerEmail, e.getMessage());
        }
    }

    // CASE 3: DETAILS MISSING / PROCESSING FAILURE FOR REGISTERED BUYER
    public void sendFailureAcknowledgement(FailedRfqRequest request, Buyer buyer) {
        String buyerEmail = (buyer != null && buyer.getEmail() != null && !buyer.getEmail().isBlank())
                ? buyer.getEmail() : (request != null ? request.getBuyerEmail() : "");
        String buyerName = resolveBuyerName(buyer);

        sendCase3DetailsMissingAcknowledgement(buyerEmail, buyerName);
    }

    public void sendMissingQuantityAcknowledgement(String buyerEmail, String buyerName, List<String> missingItems) {
        sendCase3DetailsMissingAcknowledgement(buyerEmail, buyerName);
    }

    public void sendConsolidatedAcknowledgement(List<RFQEntity> createdRfqs, List<String> failedItems, Buyer buyer, String rawSubject) {
        if (createdRfqs != null && !createdRfqs.isEmpty()) {
            sendSuccessAcknowledgement(createdRfqs, buyer);
        } else {
            String buyerEmail = (buyer != null && buyer.getEmail() != null && !buyer.getEmail().isBlank())
                    ? buyer.getEmail() : "";
            String buyerName = resolveBuyerName(buyer);
            sendCase3DetailsMissingAcknowledgement(buyerEmail, buyerName);
        }
    }

    public void sendCase3DetailsMissingAcknowledgement(String buyerEmail, String buyerName) {
        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send CASE 3 details missing acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        try {
            String resolvedName = (buyerName != null && !buyerName.isBlank()) ? buyerName : "Valued Customer";

            SimpleMailMessage mailMessage = createBaseMailMessage(buyerEmail);
            mailMessage.setSubject(getCase3Subject());
            mailMessage.setText(getCase3Body(resolvedName));

            mailSender.send(mailMessage);
            log.info("CASE 3 Details missing acknowledgement email sent to {}", buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send CASE 3 details missing acknowledgement email to {}: {}", buyerEmail, e.getMessage());
        }
    }

    public void sendDuplicateEmailAcknowledgement(String buyerEmail, String rawSubject) {
        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send duplicate email acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        try {
            SimpleMailMessage mailMessage = createBaseMailMessage(buyerEmail);
            mailMessage.setSubject("Duplicate Request Received: " + (rawSubject != null && !rawSubject.isBlank() ? rawSubject : "RFQ Request"));
            mailMessage.setText("Dear Valued Customer,\n\n"
                    + "We received your email request, but our system detected that this request has already been received and processed.\n\n"
                    + "To prevent duplicate RFQ creation, no new RFQ was generated for this duplicate submission.\n\n"
                    + "Best regards,\nTeam Procucev");

            mailSender.send(mailMessage);
            log.info("Duplicate email acknowledgement sent successfully to: {}", buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send duplicate email acknowledgement to {}: {}", buyerEmail, e.getMessage());
        }
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
        return "Hi there,\n\n"
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
        return "Hi " + buyerName + ",\n\n"
                + "We received your requirement, but need a bit more info to match you with the right suppliers fast:\n\n"
                + "• Quantity required\n\n"
                + "Just reply to this email with the details, or it's even quicker on call/WhatsApp.\n\n"
                + "📞 Call: +91-7996170801\n"
                + "✉️ Email: RFQ@procucev.com\n\n"
                + "The sooner we get these, the sooner your RFQ goes live to suppliers!\n\n"
                + "Team Procucev";
    }
}
