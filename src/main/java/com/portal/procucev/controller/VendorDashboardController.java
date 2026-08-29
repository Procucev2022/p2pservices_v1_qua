package com.portal.procucev.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.Dto.vendor.VendorDashboardDto;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.User;
import com.portal.procucev.service.VendorDashboardService;

/**
 * REST surface for the vendor workspace screens (3.1 - 3.6).
 * Mirrors the buyer dashboard controller conventions.
 */
@RestController
@RequestMapping("/rest/vendor/dashboard")
@CrossOrigin
public class VendorDashboardController {

    private static final Logger log = LoggerFactory.getLogger(VendorDashboardController.class);

    @Autowired
    private VendorDashboardService vendorDashboardService;

    @Autowired
    private UserDao userDao;

    @GetMapping("/summary")
    public ResponseEntity<MessageResponse> getSummary(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String username) {
        try {
            String resolvedOrgId = resolveOrgId(orgId);
            String resolvedUser = resolveUsername(username);
            String resolvedVendorId = (vendorId != null && !vendorId.isBlank()) ? vendorId : resolvedUser;

            VendorDashboardDto.ProfileSummaryDto summary =
                    vendorDashboardService.getProfileSummary(resolvedOrgId, resolvedVendorId, resolvedUser);
            return ResponseEntity.ok(MessageResponse.success("Vendor summary retrieved", Map.of("summary", summary)));
        } catch (Exception e) {
            log.error("Error retrieving vendor summary", e);
            return failure("Failed to retrieve vendor summary", e);
        }
    }

    @GetMapping("/opportunities")
    public ResponseEntity<MessageResponse> getOpportunities(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String username) {
        try {
            String resolvedOrgId = resolveOrgId(orgId);
            String resolvedUser = resolveUsername(username);
            String resolvedVendorId = (vendorId != null && !vendorId.isBlank()) ? vendorId : resolvedUser;

            List<VendorDashboardDto.OpportunityDto> opportunities =
                    vendorDashboardService.getOpportunities(resolvedOrgId, resolvedVendorId, resolvedUser);
            return ResponseEntity.ok(MessageResponse.success(
                    "Vendor opportunities retrieved", Map.of("opportunities", opportunities)));
        } catch (Exception e) {
            log.error("Error retrieving vendor opportunities", e);
            return failure("Failed to retrieve opportunities", e);
        }
    }

    @GetMapping("/catalogue")
    public ResponseEntity<MessageResponse> getCatalogue(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String vendorId) {
        try {
            String resolvedOrgId = resolveOrgId(orgId);
            List<VendorDashboardDto.CatalogueProductDto> products =
                    vendorDashboardService.getCatalogue(resolvedOrgId, vendorId);
            return ResponseEntity.ok(MessageResponse.success(
                    "Vendor catalogue retrieved", Map.of("products", products)));
        } catch (Exception e) {
            log.error("Error retrieving vendor catalogue", e);
            return failure("Failed to retrieve catalogue", e);
        }
    }

    @PostMapping("/catalogue/save")
    public ResponseEntity<MessageResponse> saveCatalogueProduct(
            @RequestBody VendorDashboardDto.CatalogueProductDto product) {
        try {
            String username = resolveUsername(null);
            String orgId = resolveOrgId(null);
            VendorDashboardDto.CatalogueProductDto saved =
                    vendorDashboardService.saveCatalogueProduct(product, orgId, username);
            return ResponseEntity.ok(MessageResponse.success(
                    "Catalogue product saved", Map.of("product", saved)));
        } catch (Exception e) {
            log.error("Error saving catalogue product", e);
            return failure("Failed to save catalogue product", e);
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<MessageResponse> getSubscriptionPlans() {
        try {
            String orgId = resolveOrgId(null);
            List<VendorDashboardDto.SubscriptionPlanDto> plans =
                    vendorDashboardService.getSubscriptionPlans(orgId);
            return ResponseEntity.ok(MessageResponse.success(
                    "Vendor subscription plans retrieved", Map.of("plans", plans)));
        } catch (Exception e) {
            log.error("Error retrieving vendor subscription plans", e);
            return failure("Failed to retrieve subscription plans", e);
        }
    }

    @PostMapping("/subscribe")
    public ResponseEntity<MessageResponse> updateSubscription(
            @RequestBody VendorDashboardDto.SubscribeRequestDto request) {
        try {
            String username = resolveUsername(null);
            String orgId = resolveOrgId(null);
            VendorDashboardDto.SubscribeResponseDto response =
                    vendorDashboardService.updateSubscription(request, orgId, username);
            return ResponseEntity.ok(MessageResponse.success(
                    "Subscription updated", Map.of("subscription", response)));
        } catch (Exception e) {
            log.error("Error updating vendor subscription", e);
            return failure("Failed to update subscription", e);
        }
    }

    @PostMapping("/rfq/download")
    public ResponseEntity<MessageResponse> downloadRfqDocuments(
            @RequestBody VendorDashboardDto.RfqDownloadRequestDto request) {
        try {
            String username = resolveUsername(null);
            String orgId = resolveOrgId(null);
            VendorDashboardDto.RfqDownloadResponseDto response =
                    vendorDashboardService.downloadRfqDocuments(request, orgId, username);
            return ResponseEntity.ok(MessageResponse.success(
                    "RFQ documents dispatched", Map.of("download", response)));
        } catch (Exception e) {
            log.error("Error dispatching RFQ documents", e);
            return failure("Failed to dispatch RFQ documents", e);
        }
    }

    @PostMapping("/quotation")
    public ResponseEntity<MessageResponse> submitQuotation(
            @RequestBody VendorDashboardDto.QuotationRequestDto request) {
        try {
            String username = resolveUsername(null);
            String orgId = resolveOrgId(null);
            VendorDashboardDto.QuotationResponseDto response =
                    vendorDashboardService.submitQuotation(request, orgId, username);
            return ResponseEntity.ok(MessageResponse.success(
                    "Quotation submitted successfully", Map.of("quotation", response)));
        } catch (Exception e) {
            log.error("Error submitting quotation", e);
            return failure("Failed to submit quotation", e);
        }
    }

    @PostMapping("/qualification")
    public ResponseEntity<MessageResponse> submitQualification(
            @RequestBody VendorDashboardDto.QualificationRequestDto request) {
        try {
            String username = resolveUsername(null);
            String orgId = resolveOrgId(null);
            VendorDashboardDto.QualificationResponseDto response =
                    vendorDashboardService.submitQualification(request, orgId, username);
            return ResponseEntity.ok(MessageResponse.success(
                    "Qualification submitted successfully", Map.of("evaluation", response)));
        } catch (Exception e) {
            log.error("Error submitting vendor qualification", e);
            return failure("Failed to submit qualification", e);
        }
    }

    private ResponseEntity<MessageResponse> failure(String message, Exception e) {
        return ResponseEntity.internalServerError().body(
                new MessageResponse("500", message, List.of(String.valueOf(e.getMessage())),
                        new Date(), "Failure", "SYSTEM_ERROR"));
    }

    private String resolveUsername(String provided) {
        if (provided != null && !provided.isBlank()) {
            return provided;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return auth.getName();
        }
        return "vendor@procucev.com";
    }

    private String resolveOrgId(String provided) {
        if (provided != null && !provided.isBlank()) {
            return provided;
        }
        String username = resolveUsername(null);
        if (userDao != null) {
            try {
                User user = userDao.findByLatestUserName(username);
                if (user != null && user.getOrg() != null && user.getOrg().getId() != null) {
                    return user.getOrg().getId();
                }
                List<User> users = userDao.findByUsername(username);
                if (users != null && !users.isEmpty()) {
                    for (User u : users) {
                        if (u.isActive() && u.getOrg() != null && u.getOrg().getId() != null) {
                            return u.getOrg().getId();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not resolve vendor organization from DB: {}", e.getMessage());
            }
        }
        return "ORG-PROCUCEV-VENDOR";
    }
}
