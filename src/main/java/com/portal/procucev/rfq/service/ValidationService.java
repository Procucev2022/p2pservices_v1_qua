package com.portal.procucev.rfq.service;

import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ValidationService {

    @Data
    @Builder
    public static class ValidationResult {
        private boolean valid;
        private boolean missingQuantity;
        private List<String> missingItems;
        private String failureReason;
    }

    public ValidationResult validateWithDetails(ExtractedRFQ extractedRFQ) {
        log.info("Validating extracted RFQ details...");

        if (extractedRFQ == null) {
            return ValidationResult.builder().valid(false).failureReason("AI Extraction returned null result.").build();
        }

        if (extractedRFQ.getBuyerEmail() == null || extractedRFQ.getBuyerEmail().isBlank()) {
            return ValidationResult.builder().valid(false).failureReason("Buyer email is missing.").build();
        }

        List<RFQItem> items = extractedRFQ.getItems();
        if (items == null || items.isEmpty()) {
            return ValidationResult.builder().valid(false).failureReason("No line items found in RFQ.").build();
        }

        List<String> missingQuantityItems = new java.util.ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            RFQItem item = items.get(i);
            if (item == null) {
                return ValidationResult.builder()
                        .valid(false)
                        .failureReason("Item #" + (i + 1) + " is null.")
                        .build();
            }

            if (item.getItemDescription() == null || item.getItemDescription().isBlank()) {
                return ValidationResult.builder()
                        .valid(false)
                        .failureReason("Item #" + (i + 1) + " has no description.")
                        .build();
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                missingQuantityItems.add(item.getItemDescription().trim());
            }
        }

        if (!missingQuantityItems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("RFQ creation failed because quantity is missing for:\n");
            for (int k = 0; k < missingQuantityItems.size(); k++) {
                sb.append((k + 1)).append(". ").append(missingQuantityItems.get(k)).append("\n");
            }
            sb.append("Quantity is a mandatory field for every RFQ item.");

            String reason = sb.toString();
            log.warn("RFQ Validation failed due to missing quantity: {}", missingQuantityItems);

            return ValidationResult.builder()
                    .valid(false)
                    .missingQuantity(true)
                    .missingItems(missingQuantityItems)
                    .failureReason(reason)
                    .build();
        }

        log.info("Extracted RFQ passed all validation checks successfully.");
        return ValidationResult.builder().valid(true).failureReason(null).build();
    }
}
