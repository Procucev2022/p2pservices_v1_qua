package com.portal.procucev.rfq.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.FailedRfqRequest;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.RFQItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcknowledgementEmailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${rfq.acknowledgement.from:veerababu.v@procucev.com}")
    private String mailFrom = "veerababu.v@procucev.com";

    @org.springframework.beans.factory.annotation.Value("${rfq.acknowledgement.cc:support@procucev.com}")
    private String mailCc = "support@procucev.com";

    public void sendSuccessAcknowledgement(List<RFQEntity> rfqEntities, Buyer buyer) {
        if (rfqEntities == null || rfqEntities.isEmpty()) {
            log.error("Cannot send success acknowledgement: RFQEntity list is empty.");
            return;
        }

        if (rfqEntities.size() == 1) {
            sendSuccessAcknowledgement(rfqEntities.get(0), buyer);
            return;
        }

        String buyerEmail = (buyer != null && buyer.getEmail() != null && !buyer.getEmail().isBlank())
                ? buyer.getEmail() : rfqEntities.get(0).getBuyerEmail();

        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send success acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        log.info("Sending consolidated acknowledgement for {} RFQs to recipient: {}", rfqEntities.size(), buyerEmail);

        try {
            String buyerName = (buyer != null && buyer.getName() != null && !buyer.getName().isBlank())
                    ? buyer.getName() : "Valued Customer";

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailFrom);
            mailMessage.setTo(buyerEmail);
            if (mailCc != null && !mailCc.isBlank()) {
                mailMessage.setCc(mailCc.trim());
            }
            mailMessage.setSubject("RFQs Successfully Created (" + rfqEntities.size() + " RFQs)");

            String body = buildConsolidatedSuccessEmailBody(rfqEntities, buyerName);
            mailMessage.setText(body);

            mailSender.send(mailMessage);
            log.info("Consolidated success acknowledgement sent successfully for {} RFQs", rfqEntities.size());
        } catch (Exception e) {
            log.error("Failed to send consolidated success acknowledgement email to {}: {}", buyerEmail, e.getMessage());
        }
    }

    public void sendSuccessAcknowledgement(RFQEntity rfqEntity, Buyer buyer) {
        if (rfqEntity == null) {
            log.error("Cannot send success acknowledgement: RFQEntity is null.");
            return;
        }

        String rfqNumber = rfqEntity.getRfqNumber();
        String buyerEmail = (buyer != null && buyer.getEmail() != null && !buyer.getEmail().isBlank())
                ? buyer.getEmail() : rfqEntity.getBuyerEmail();

        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send success acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        log.info("Final resolved acknowledgement recipient: {}", buyerEmail);

        try {
            String buyerName = (buyer != null && buyer.getName() != null && !buyer.getName().isBlank())
                    ? buyer.getName() : "Valued Customer";

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailFrom);
            mailMessage.setTo(buyerEmail);
            if (mailCc != null && !mailCc.isBlank()) {
                mailMessage.setCc(mailCc.trim());
            }
            mailMessage.setSubject("RFQ Successfully Created – " + rfqNumber);

            String body = buildSuccessEmailBody(rfqEntity, buyerName);
            mailMessage.setText(body);

            mailSender.send(mailMessage);
            log.info("Success acknowledgement sent successfully for RFQ Number: {}", rfqNumber);
        } catch (Exception e) {
            log.error("Failed to send success acknowledgement email for RFQ Number: {} (Email: {}): {}",
                    rfqNumber, buyerEmail, e.getMessage());
        }
    }

    public void sendFailureAcknowledgement(FailedRfqRequest request, Buyer buyer) {
        if (request == null) {
            log.error("Cannot send failure acknowledgement: FailedRfqRequest is null.");
            return;
        }

        String buyerEmail = (buyer != null && buyer.getEmail() != null && !buyer.getEmail().isBlank())
                ? buyer.getEmail() : request.getBuyerEmail();

        if (buyerEmail == null || buyerEmail.isBlank()) {
            log.error("Cannot send failure acknowledgement: Buyer email is missing.");
            return;
        }

        log.info("Sending failure acknowledgement to: {}", buyerEmail);

        try {
            String buyerName = (buyer != null && buyer.getName() != null && !buyer.getName().isBlank())
                    ? buyer.getName() : "Valued Buyer";

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailFrom);
            mailMessage.setTo(buyerEmail);
            mailMessage.setSubject("Action Required: Your Request for Quotation Could Not Be Processed");

            String body = buildFailureEmailBody(request, buyerName);
            mailMessage.setText(body);

            mailSender.send(mailMessage);
            log.info("Failure acknowledgement sent successfully to: {}", buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send failure acknowledgement email to {}: {}", buyerEmail, e.getMessage());
        }
    }

    public void sendMissingQuantityAcknowledgement(String buyerEmail, String buyerName, List<String> missingItems) {
        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send missing quantity acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        log.info("Sending missing quantity failure acknowledgement to: {}", buyerEmail);

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailFrom);
            mailMessage.setTo(buyerEmail);
            mailMessage.setSubject("RFQ Creation Failed - Missing Mandatory Quantity");

            String resolvedName = (buyerName != null && !buyerName.isBlank()) ? buyerName : "Valued Customer";

            StringBuilder sb = new StringBuilder();
            sb.append("Dear ").append(resolvedName).append(",\n\n");
            sb.append("We could not create your Request for Quotation because some mandatory information is missing.\n\n");
            sb.append("RFQ Status:\nFAILED\n\n");
            sb.append("Reason:\nQuantity is a mandatory field for every RFQ item.\n\n");
            sb.append("Missing Quantity:\n");

            if (missingItems != null && !missingItems.isEmpty()) {
                for (String item : missingItems) {
                    sb.append("- ").append(item).append("\n");
                }
            } else {
                sb.append("- Item Quantity missing\n");
            }

            sb.append("\nPlease provide the required quantity for each item and resend the RFQ email.\n\n");
            sb.append("No RFQ has been created in the procurement system.\n\n");
            sb.append("Best regards,\n");
            sb.append("Procurement Operations Team\n");
            sb.append("Procucev Platform\n");

            mailMessage.setText(sb.toString());
            mailSender.send(mailMessage);
            log.info("Missing quantity failure acknowledgement email sent successfully to: {}", buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send missing quantity acknowledgement email to {}: {}", buyerEmail, e.getMessage());
        }
    }

    public void sendUnregisteredBuyerAcknowledgement(String buyerEmail) {
        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send unregistered buyer acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        log.info("Sending registration invitation acknowledgement to: {}", buyerEmail);

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailFrom);
            mailMessage.setTo(buyerEmail);
            mailMessage.setSubject("Action Required: Please Register on Procucev Platform");

            StringBuilder sb = new StringBuilder();
            sb.append("Dear Valued Customer,\n\n");
            sb.append("Thank you for reaching out to Procucev Procurement Platform.\n\n");
            sb.append("We received your Request for Quotation (RFQ) email, but you are currently not registered as an active buyer on our platform.\n\n");
            sb.append("--------------------------------------------------\n");
            sb.append("ACTION REQUIRED TO PROCESS YOUR RFQ\n");
            sb.append("--------------------------------------------------\n");
            sb.append("To automatically process your RFQ emails and broadcast them to our supplier network, please register your account on our portal:\n\n");
            sb.append("Portal Registration URL:\nhttps://procucev.com/\n\n");
            sb.append("Once registered, your future RFQ emails will be automatically extracted and processed into official RFQs.\n\n");
            sb.append("Best regards,\n");
            sb.append("Customer Support Team\n");
            sb.append("Procucev Platform\n");

            mailMessage.setText(sb.toString());
            mailSender.send(mailMessage);
            log.info("Unregistered buyer registration invitation sent successfully to: {}", buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send unregistered buyer registration invitation to {}: {}", buyerEmail, e.getMessage());
        }
    }

    public void sendDuplicateEmailAcknowledgement(String buyerEmail, String rawSubject) {
        if (buyerEmail == null || buyerEmail.isBlank() || !buyerEmail.contains("@")) {
            log.error("Cannot send duplicate email acknowledgement: Invalid recipient email '{}'", buyerEmail);
            return;
        }

        log.info("Sending duplicate email acknowledgement to: {}", buyerEmail);

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailFrom);
            mailMessage.setTo(buyerEmail);
            if (mailCc != null && !mailCc.isBlank()) {
                mailMessage.setCc(mailCc.trim());
            }
            mailMessage.setSubject("Duplicate Request Received: " + (rawSubject != null && !rawSubject.isBlank() ? rawSubject : "RFQ Request"));

            StringBuilder sb = new StringBuilder();
            sb.append("Dear Valued Customer,\n\n");
            sb.append("We received your email request, but our system detected that this request has already been received and processed.\n\n");
            sb.append("To prevent duplicate RFQ creation, no new RFQ was generated for this duplicate submission.\n\n");
            sb.append("If you have any questions or need further assistance, please contact customer support.\n\n");
            sb.append("Best regards,\n");
            sb.append("Customer Support Team\n");
            sb.append("Procucev Platform\n");

            mailMessage.setText(sb.toString());
            mailSender.send(mailMessage);
            log.info("Duplicate email acknowledgement sent successfully to: {}", buyerEmail);
        } catch (Exception e) {
            log.error("Failed to send duplicate email acknowledgement to {}: {}", buyerEmail, e.getMessage());
        }
    }

    private String buildSuccessEmailBody(RFQEntity rfqEntity, String buyerName) {
        List<RFQItem> items = parseItemsJson(rfqEntity.getItemsJson());
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(buyerName).append(",\n\n");
        sb.append("Thank you for submitting your Request for Quotation (RFQ).\n");
        sb.append("We are pleased to inform you that your request has been successfully created in our procurement system.\n\n");
        sb.append("--------------------------------------------------\n");
        sb.append("RFQ DETAILS\n");
        sb.append("--------------------------------------------------\n");
        sb.append("RFQ Number:\n").append(rfqEntity.getRfqNumber()).append("\n\n");
        sb.append("Items:\n").append(items.size()).append("\n\n");
        sb.append("Delivery Date:\n").append(rfqEntity.getDeliveryDate() != null ? rfqEntity.getDeliveryDate() : "N/A").append("\n\n");
        sb.append("Delivery Location:\n").append(rfqEntity.getDeliveryLocation() != null ? rfqEntity.getDeliveryLocation() : "N/A").append("\n\n");
        sb.append("Status:\nSuccessfully Created\n\n");
        if (items.isEmpty()) {
            sb.append("- Line items details recorded in system.\n");
        } else {
            for (int i = 0; i < items.size(); i++) {
                RFQItem item = items.get(i);
                sb.append(i + 1).append(". ")
                        .append(item.getItemDescription() != null ? item.getItemDescription() : "Item")
                        .append(" | Qty: ").append(item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                        .append(" ").append(item.getUom() != null ? item.getUom() : "Nos");
                if (item.getCategory() != null) {
                    sb.append(" | Category: ").append(item.getCategory());
                }
                sb.append("\n");
            }
        }

        sb.append("\n--------------------------------------------------\n");
        sb.append("NEXT STEPS\n");
        sb.append("--------------------------------------------------\n");
        sb.append("1. You can track the status of this RFQ in your buyer portal using RFQ Number: ")
                .append(rfqEntity.getRfqNumber()).append("\n\n");
        sb.append("2. You will receive follow-up updates on this RFQ shortly.\n\n");

        sb.append("If you have any questions or need to make changes, please reply to this email.\n\n");
        sb.append("Best regards,\n");
        sb.append("Procurement Operations Team\n");
        sb.append("Procucev Platform\n");

        return sb.toString();
    }

    private String buildConsolidatedSuccessEmailBody(List<RFQEntity> rfqEntities, String buyerName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(buyerName).append(",\n\n");
        sb.append("Thank you for submitting your Request for Quotation.\n\n");
        sb.append("Your email has been successfully processed and the following RFQs have been created:\n\n");

        for (int r = 0; r < rfqEntities.size(); r++) {
            RFQEntity rfqEntity = rfqEntities.get(r);
            List<RFQItem> items = parseItemsJson(rfqEntity.getItemsJson());

            sb.append(r + 1).append(". ").append(rfqEntity.getRfqNumber()).append("\n");
            if (items != null && !items.isEmpty()) {
                RFQItem first = items.get(0);
                if (first.getCategory() != null && !first.getCategory().isBlank()) {
                    sb.append("   Category: ").append(first.getCategory()).append("\n");
                }
                sb.append("   Item: ").append(first.getItemDescription() != null ? first.getItemDescription() : "Item").append("\n");
                sb.append("   Quantity: ").append(first.getQuantity() != null ? first.getQuantity().intValue() : 1)
                        .append(" ").append(first.getUom() != null ? first.getUom() : "Nos").append("\n");
            }
            sb.append("   Delivery Location: ").append(rfqEntity.getDeliveryLocation() != null ? rfqEntity.getDeliveryLocation() : "N/A").append("\n");
            sb.append("   Delivery Date: ").append(rfqEntity.getDeliveryDate() != null ? rfqEntity.getDeliveryDate() : "N/A").append("\n\n");
        }

        sb.append("If you have any questions or need to make changes, please reply to this email.\n\n");
        sb.append("Best regards,\n");
        sb.append("Procurement Operations Team\n");
        sb.append("Procucev Platform\n");

        return sb.toString();
    }

    private String buildFailureEmailBody(FailedRfqRequest request, String buyerName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(buyerName).append(",\n\n");
        sb.append("Thank you for reaching out to Procucev Procurement Platform.\n");
        sb.append("We received your email, but unfortunately, we were unable to automatically generate a Request for Quotation (RFQ) from your request.\n\n");
        sb.append("--------------------------------------------------\n");
        sb.append("REASON FOR FAILURE\n");
        sb.append("--------------------------------------------------\n");
        sb.append(request.getReasonForFailure() != null ? request.getReasonForFailure() : "Unable to validate RFQ details.").append("\n\n");
        sb.append("--------------------------------------------------\n");
        sb.append("DETAILS EXTRACTED FROM YOUR EMAIL\n");
        sb.append("--------------------------------------------------\n");
        sb.append("Email Subject: ").append(request.getRawSubject() != null ? request.getRawSubject() : "N/A").append("\n");
        sb.append("Description: ").append(request.getDescription()).append("\n");
        sb.append("Quantity: ").append(request.getQuantity()).append(" ").append(request.getUom()).append("\n");
        sb.append("Delivery Location: ").append(request.getDeliveryLocation()).append("\n");
        sb.append("Delivery Date: ").append(request.getDeliveryDate()).append("\n\n");
        sb.append("Please verify your email information or contact support.\n\n");
        sb.append("Best regards,\n");
        sb.append("Customer Support Team\n");
        sb.append("Procucev Platform\n");

        return sb.toString();
    }

    private List<RFQItem> parseItemsJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<RFQItem>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
