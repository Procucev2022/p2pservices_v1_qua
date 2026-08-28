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

import com.portal.procucev.Dto.buyer.BuyerDashboardDto;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.User;
import com.portal.procucev.service.BuyerDashboardService;

@RestController
@RequestMapping("/rest/buyer/dashboard")
@CrossOrigin
public class BuyerDashboardController {

    private static final Logger log = LoggerFactory.getLogger(BuyerDashboardController.class);

    @Autowired
    private BuyerDashboardService buyerDashboardService;

    @Autowired
    private UserDao userDao;

    @GetMapping("/summary")
    public ResponseEntity<MessageResponse> getSummary(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String buyerId,
            @RequestParam(required = false) String username) {
        try {
            String resolvedOrgId = (orgId != null && !orgId.isBlank()) ? orgId : getLoggedInBuyerOrgId();
            String resolvedUser = (username != null && !username.isBlank()) ? username : getLoggedInUsername();
            String resolvedBuyerId = (buyerId != null && !buyerId.isBlank()) ? buyerId : resolvedUser;

            BuyerDashboardDto.SummaryResponse summary = buyerDashboardService.getSummary(resolvedOrgId, resolvedBuyerId, resolvedUser);
            return ResponseEntity.ok(MessageResponse.success("Dashboard summary retrieved", Map.of("summary", summary)));
        } catch (Exception e) {
            log.error("Error retrieving dashboard summary", e);
            return ResponseEntity.internalServerError().body(
                    new MessageResponse("500", "Failed to retrieve summary", List.of(e.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    @GetMapping("/rfqs")
    public ResponseEntity<MessageResponse> getPipelineRfqs(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String buyerId,
            @RequestParam(required = false) String username) {
        try {
            String resolvedOrgId = (orgId != null && !orgId.isBlank()) ? orgId : getLoggedInBuyerOrgId();
            String resolvedUser = (username != null && !username.isBlank()) ? username : getLoggedInUsername();
            String resolvedBuyerId = (buyerId != null && !buyerId.isBlank()) ? buyerId : resolvedUser;

            List<BuyerDashboardDto.RFQPipelineItemDto> rfqs = buyerDashboardService.getPipelineRfqs(resolvedOrgId, resolvedBuyerId, resolvedUser);
            return ResponseEntity.ok(MessageResponse.success("Pipeline RFQs retrieved", Map.of("rfqs", rfqs)));
        } catch (Exception e) {
            log.error("Error retrieving pipeline RFQs", e);
            return ResponseEntity.internalServerError().body(
                    new MessageResponse("500", "Failed to retrieve pipeline RFQs", List.of(e.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    @GetMapping("/live-feed")
    public ResponseEntity<MessageResponse> getLiveFeed(
            @RequestParam(required = false, defaultValue = "all") String channel,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String buyerId,
            @RequestParam(required = false) String username) {
        try {
            String resolvedOrgId = (orgId != null && !orgId.isBlank()) ? orgId : getLoggedInBuyerOrgId();
            String resolvedUser = (username != null && !username.isBlank()) ? username : getLoggedInUsername();
            String resolvedBuyerId = (buyerId != null && !buyerId.isBlank()) ? buyerId : resolvedUser;

            List<BuyerDashboardDto.LiveFeedItemDto> feed = buyerDashboardService.getLiveFeed(resolvedOrgId, resolvedBuyerId, resolvedUser, channel);
            return ResponseEntity.ok(MessageResponse.success("Live feed retrieved", Map.of("feed", feed)));
        } catch (Exception e) {
            log.error("Error retrieving live feed", e);
            return ResponseEntity.internalServerError().body(
                    new MessageResponse("500", "Failed to retrieve live feed", List.of(e.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    @PostMapping("/chaser")
    public ResponseEntity<MessageResponse> triggerChaser(@RequestBody BuyerDashboardDto.ChaserRequestDto request) {
        try {
            String username = getLoggedInUsername();
            BuyerDashboardDto.ChaserResponseDto response = buyerDashboardService.triggerChaser(request, username);
            return ResponseEntity.ok(MessageResponse.success("Chaser triggered successfully", Map.of("chaser", response)));
        } catch (Exception e) {
            log.error("Error triggering chaser", e);
            return ResponseEntity.internalServerError().body(
                    new MessageResponse("500", "Failed to trigger chaser", List.of(e.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    @PostMapping("/po/approve")
    public ResponseEntity<MessageResponse> approvePurchaseOrder(@RequestBody BuyerDashboardDto.PoApprovalRequestDto request) {
        try {
            String username = getLoggedInUsername();
            BuyerDashboardDto.PoApprovalResponseDto response = buyerDashboardService.approvePurchaseOrder(request, username);
            return ResponseEntity.ok(MessageResponse.success("PO approved successfully", Map.of("po", response)));
        } catch (Exception e) {
            log.error("Error approving PO", e);
            return ResponseEntity.internalServerError().body(
                    new MessageResponse("500", "Failed to approve PO", List.of(e.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    @GetMapping("/evaluations")
    public ResponseEntity<MessageResponse> getVendorEvaluations() {
        try {
            String orgId = getLoggedInBuyerOrgId();
            List<BuyerDashboardDto.VendorEvaluationDto> evaluations = buyerDashboardService.getVendorEvaluations(orgId);
            return ResponseEntity.ok(MessageResponse.success("Vendor evaluations retrieved", Map.of("evaluations", evaluations)));
        } catch (Exception e) {
            log.error("Error retrieving vendor evaluations", e);
            return ResponseEntity.internalServerError().body(
                    new MessageResponse("500", "Failed to retrieve evaluations", List.of(e.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<MessageResponse> getSubscriptionPlans() {
        try {
            String orgId = getLoggedInBuyerOrgId();
            List<BuyerDashboardDto.SubscriptionPlanDto> plans = buyerDashboardService.getSubscriptionPlans(orgId);
            return ResponseEntity.ok(MessageResponse.success("Subscription plans retrieved", Map.of("plans", plans)));
        } catch (Exception e) {
            log.error("Error retrieving subscription plans", e);
            return ResponseEntity.internalServerError().body(
                    new MessageResponse("500", "Failed to retrieve subscription plans", List.of(e.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    @PostMapping("/create-rfq")
    public ResponseEntity<MessageResponse> createRfq(@RequestBody BuyerDashboardDto.CreateRfqRequestDto request) {
        try {
            String username = getLoggedInUsername();
            BuyerDashboardDto.CreateRfqResponseDto response = buyerDashboardService.createRfq(request, username);
            return ResponseEntity.ok(MessageResponse.success("RFQ created successfully", Map.of("rfq", response)));
        } catch (Exception e) {
            log.error("Error creating RFQ", e);
            return ResponseEntity.internalServerError().body(
                    new MessageResponse("500", "Failed to create RFQ", List.of(e.getMessage()), new Date(), "Failure", "SYSTEM_ERROR")
            );
        }
    }

    private String getLoggedInUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return auth.getName();
        }
        return "buyer@procucev.com";
    }

    private String getLoggedInBuyerOrgId() {
        String username = getLoggedInUsername();
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
                log.debug("Could not resolve organization from DB: {}", e.getMessage());
            }
        }
        return "ORG-PROCUCEV-BUYER";
    }
}
