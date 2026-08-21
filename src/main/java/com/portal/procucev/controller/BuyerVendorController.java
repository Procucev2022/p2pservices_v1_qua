package com.portal.procucev.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.BuyerVendorAiProfile;
import com.portal.procucev.model.User;
import com.portal.procucev.service.BuyerVendorService;
import com.portal.procucev.service.VendorAiProcessingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/rest/buyer/vendors")
@CrossOrigin
public class BuyerVendorController {

    private static final Logger log = LoggerFactory.getLogger(BuyerVendorController.class);

    @Autowired
    private BuyerVendorService buyerVendorService;

    @Autowired
    private VendorAiProcessingService vendorAiProcessingService;

    @Autowired
    private UserDao userDao;

    /**
     * GET /rest/buyer/vendors?search=&status=&industry=&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<MessageResponse> getVendors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String industry,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            Pageable pageable = PageRequest.of(page, size);
            Page<BuyerVendor> vendorPage = buyerVendorService.getVendors(buyerOrgId, status, industry, search, pageable);

            MessageResponse response = MessageResponse.success(
                "Vendors retrieved successfully",
                Map.of(
                    "vendors", vendorPage.getContent(),
                    "totalRecords", vendorPage.getTotalElements(),
                    "totalPages", vendorPage.getTotalPages(),
                    "currentPage", vendorPage.getNumber()
                )
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error fetching vendors", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    /**
     * POST /rest/buyer/vendors
     */
    @PostMapping
    public ResponseEntity<MessageResponse> createVendor(@Valid @RequestBody BuyerVendor vendor) {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            vendor.setBuyerOrgId(buyerOrgId);
            vendor.setCreatedBy(getLoggedInUsername());

            BuyerVendor saved = buyerVendorService.createVendor(vendor);

            MessageResponse response = MessageResponse.success(
                "Vendor created successfully",
                Map.of("vendor", saved)
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.error("Validation error: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(
                MessageResponse.error("Validation failed", List.of(ex.getMessage()))
            );
        } catch (Exception ex) {
            log.error("Error creating vendor", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    /**
     * POST /rest/buyer/vendors/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<MessageResponse> createVendorsBulk(@RequestBody List<BuyerVendor> vendors) {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            String username = getLoggedInUsername();

            Map<String, Object> result = buyerVendorService.bulkCreateVendors(vendors, buyerOrgId, username);

            MessageResponse response = MessageResponse.success(
                "Bulk vendor upload processed successfully",
                result
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error bulk creating vendors", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    /**
     * POST /rest/buyer/vendors/ai-process
     * Triggers AI Categorization, Credential Mapping & Qualification for the provided vendors.
     */
    @PostMapping("/ai-process")
    public ResponseEntity<MessageResponse> processVendorsAi(@RequestBody List<BuyerVendor> vendors) {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            String username = getLoggedInUsername();

            List<BuyerVendorAiProfile> processedProfiles = vendorAiProcessingService.processVendorsWithAi(vendors, buyerOrgId, username);

            MessageResponse response = MessageResponse.success(
                "AI Vendor Processing completed successfully",
                Map.of(
                    "processedCount", processedProfiles.size(),
                    "profiles", processedProfiles
                )
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error in AI Vendor Processing", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    /**
     * GET /rest/buyer/vendors/ai-analysis
     * Fetches all AI analyzed vendor profiles for the buyer organization.
     */
    @GetMapping("/ai-analysis")
    public ResponseEntity<MessageResponse> getAiAnalyzedVendors() {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            List<BuyerVendorAiProfile> list = vendorAiProcessingService.getAnalyzedVendors(buyerOrgId);

            MessageResponse response = MessageResponse.success(
                "AI Vendor Analysis retrieved successfully",
                Map.of("profiles", list)
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error fetching AI analyzed vendors", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    /**
     * GET /rest/buyer/vendors/ai-profile/:vendorCode
     * Fetches the detailed AI profile for a specific vendor.
     */
    @GetMapping("/ai-profile/{vendorCode}")
    public ResponseEntity<MessageResponse> getVendorAiProfile(@PathVariable String vendorCode) {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            Optional<BuyerVendorAiProfile> profileOpt = vendorAiProcessingService.getVendorAiProfile(vendorCode, buyerOrgId);

            if (profileOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    MessageResponse.error("Vendor AI profile not found", List.of("AI_PROFILE_NOT_FOUND"))
                );
            }

            MessageResponse response = MessageResponse.success(
                "Vendor AI profile retrieved successfully",
                Map.of("profile", profileOpt.get())
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error fetching vendor AI profile", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    /**
     * GET /rest/buyer/vendors/:id
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getVendorById(@PathVariable String id) {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            Optional<BuyerVendor> vendor = buyerVendorService.getVendorById(id, buyerOrgId);

            if (vendor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    MessageResponse.error("Vendor not found", List.of("VENDOR_NOT_FOUND"))
                );
            }

            MessageResponse response = MessageResponse.success(
                "Vendor retrieved successfully",
                Map.of("vendor", vendor.get())
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error fetching vendor", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    /**
     * PUT /rest/buyer/vendors/:id
     */
    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> updateVendor(@PathVariable String id, @Valid @RequestBody BuyerVendor vendor) {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            BuyerVendor updated = buyerVendorService.updateVendor(id, vendor, buyerOrgId);

            MessageResponse response = MessageResponse.success(
                "Vendor updated successfully",
                Map.of("vendor", updated)
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.error("Validation error: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(
                MessageResponse.error("Validation failed", List.of(ex.getMessage()))
            );
        } catch (Exception ex) {
            log.error("Error updating vendor", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    /**
     * PATCH /rest/buyer/vendors/:id/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<MessageResponse> updateVendorStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            String buyerOrgId = getLoggedInBuyerOrgId();
            String newStatus = body.get("status");

            if (newStatus == null || (!newStatus.equals("Active") && !newStatus.equals("Inactive") && !newStatus.equals("Pending"))) {
                return ResponseEntity.badRequest().body(
                    MessageResponse.error("Invalid status value", List.of("Status must be Active, Inactive, or Pending"))
                );
            }

            boolean updated = buyerVendorService.updateStatus(id, buyerOrgId, newStatus);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    MessageResponse.error("Vendor not found", List.of("VENDOR_NOT_FOUND"))
                );
            }

            MessageResponse response = MessageResponse.success(
                "Vendor status updated to " + newStatus,
                Map.of("id", id, "status", newStatus)
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error updating vendor status", ex);
            return ResponseEntity.internalServerError().body(
                new MessageResponse("500", "Internal server error", List.of(ex.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    private String getLoggedInBuyerOrgId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userDao.findByLatestUserName(username);
        if (user != null && user.getOrg() != null) {
            return user.getOrg().getId();
        }
        List<User> users = userDao.findByUsername(username);
        if (users != null && !users.isEmpty()) {
            for (User u : users) {
                if (u.isActive() && u.getOrg() != null) {
                    return u.getOrg().getId();
                }
            }
            for (User u : users) {
                if (u.getOrg() != null) {
                    return u.getOrg().getId();
                }
            }
        }
        throw new IllegalStateException("Logged-in user has no organization");
    }

    private String getLoggedInUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
