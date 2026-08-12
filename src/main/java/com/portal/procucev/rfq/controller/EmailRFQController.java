package com.portal.procucev.rfq.controller;

import com.portal.procucev.rfq.dto.ApiResponse;
import com.portal.procucev.rfq.dto.ProcessingStats;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.service.EmailProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/rfq/email")
@RequiredArgsConstructor
public class EmailRFQController {

    private final EmailProcessorService emailProcessorService;
    private final RFQRepository rfqRepository;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<ProcessingStats>> processEmailsManually() {
        log.info("Manual trigger received for Email-to-RFQ processing.");
        ProcessingStats stats = emailProcessorService.processUnreadEmails();
        return ResponseEntity.ok(ApiResponse.success("Email processing execution completed.", stats));
    }

    @GetMapping("/{rfqNumber}")
    public ResponseEntity<ApiResponse<RFQEntity>> getRfqByNumber(@PathVariable String rfqNumber,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Querying persisted RFQ by RFQ Number: {}", rfqNumber);
        String buyerEmail = userDetails.getUsername();
        return rfqRepository.findByRfqNumberAndBuyerEmailIgnoreCase(rfqNumber, buyerEmail)
                .map(rfq -> ResponseEntity.ok(ApiResponse.success("RFQ found.", rfq)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(ApiResponse.error("RFQ not found for number: " + rfqNumber)));
    }
}
