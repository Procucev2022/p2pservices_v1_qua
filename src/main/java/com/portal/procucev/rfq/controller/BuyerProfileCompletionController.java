package com.portal.procucev.rfq.controller;

import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.dto.ApiResponse;
import com.portal.procucev.rfq.dto.BuyerOtpRequest;
import com.portal.procucev.rfq.dto.BuyerProfileCompletionRequest;
import com.portal.procucev.rfq.entity.BuyerEntity;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.repository.BuyerRepository;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.service.AcknowledgementEmailService;
import com.portal.procucev.rfq.service.DemoBuyerCleanupService;
import com.portal.procucev.rfq.service.DemoBuyerRegistrationService;
import com.portal.procucev.rfq.service.PendingRfqResumeService;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.service.SmsService;
import com.portal.procucev.utils.PhoneNumberUtils;
import com.portal.procucev.utils.StatusConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rfq/email/buyer")
@RequiredArgsConstructor
public class BuyerProfileCompletionController {

    private final UserDao userDao;
    private final ClientDao clientDao;
    private final PincodeDao pincodeDao;
    private final BuyerRepository buyerRepository;
    private final EmailTransactionRepository emailTransactionRepository;
    private final SelfRegistrationService selfRegistrationService;
    private final SmsService smsService;
    private final PendingRfqResumeService pendingRfqResumeService;

    @Autowired(required = false)
    private DemoBuyerRegistrationService demoBuyerRegistrationService;

    @Autowired(required = false)
    private DemoBuyerCleanupService demoBuyerCleanupService;

    private boolean isDemoPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return true;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.endsWith("9999999991") || digits.endsWith("0000000000") || digits.equals("9999999991");
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBuyerStatus(@RequestParam String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userDao.findByUsernameAndActive(normalizedEmail, true);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Buyer not found for email: " + normalizedEmail));
        }

        List<EmailTransaction> pendingTransactions = emailTransactionRepository
                .findBySenderEmailIgnoreCaseAndStatus(normalizedEmail, StatusConstants.PENDING_BUYER_REGISTRATION);

        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getUsername());
        data.put("fullName", user.getFullName());
        data.put("phone", user.getPhone());
        boolean isDemo = (StatusConstants.DEMO_BUYER.equalsIgnoreCase(user.getVerificationStatus())
                || isDemoPhoneNumber(user.getPhone()))
                && !StatusConstants.PROFILE_COMPLETED.equalsIgnoreCase(user.getVerificationStatus());
        data.put("isDemoBuyer", isDemo);
        data.put("isDemoPhone", isDemoPhoneNumber(user.getPhone()));
        data.put("verificationStatus", user.getVerificationStatus());
        data.put("companyName", user.getOrg() != null ? user.getOrg().getCompanyName() : null);
        data.put("city", user.getOrg() != null ? user.getOrg().getCity() : null);
        data.put("state", user.getOrg() != null ? user.getOrg().getState() : null);
        data.put("pincode", user.getOrg() != null ? user.getOrg().getZipCode() : null);
        data.put("pendingRfqsCount", pendingTransactions.size());
        data.put("resetPassword", user.isResetPassword());

        return ResponseEntity.ok(ApiResponse.success("Buyer status retrieved successfully.", data));
    }

    @PostMapping("/send-email-otp")
    public ResponseEntity<ApiResponse<String>> sendEmailOtp(@RequestBody BuyerOtpRequest requestDto, HttpServletRequest request) {
        String email = requestDto.getEmail() != null ? requestDto.getEmail().trim().toLowerCase() : "";
        User user = userDao.findByUsernameAndActive(email, true);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found for email: " + email));
        }

        String phone = (requestDto.getPhone() != null && !requestDto.getPhone().isBlank())
                ? PhoneNumberUtils.normalize(requestDto.getPhone().trim())
                : (user.getPhone() != null ? user.getPhone() : StatusConstants.DEMO_PHONE_NUMBER);
        boolean sent = selfRegistrationService.generateEmailOtp(email, request, phone);
        if (sent) {
            return ResponseEntity.ok(ApiResponse.success("OTP sent to your email address.", null));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to send OTP to email."));
        }
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<ApiResponse<String>> verifyEmailOtp(@RequestBody BuyerOtpRequest requestDto) {
        String email = requestDto.getEmail() != null ? requestDto.getEmail().trim().toLowerCase() : "";
        String otp = requestDto.getOtp() != null ? requestDto.getOtp().trim() : "";

        User user = userDao.findByUsernameAndActive(email, true);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found for email: " + email));
        }

        String phone = (requestDto.getPhone() != null && !requestDto.getPhone().isBlank())
                ? PhoneNumberUtils.normalize(requestDto.getPhone().trim())
                : (user.getPhone() != null ? user.getPhone() : StatusConstants.DEMO_PHONE_NUMBER);

        Organization org = new Organization();
        org.setEmail(email);
        org.setOrganizationPhonenumber(phone);
        org.setEmailOtp(otp);

        boolean isValid = selfRegistrationService.isEmailOtpValid(org);
        if (isValid) {
            selfRegistrationService.removeEmailOtp(org);
            log.info("Email OTP verified successfully for {}", email);
            return ResponseEntity.ok(ApiResponse.success("Email OTP verified successfully.", null));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired email OTP."));
        }
    }

    @PostMapping("/send-phone-otp")
    public ResponseEntity<ApiResponse<String>> sendPhoneOtp(@RequestBody BuyerOtpRequest requestDto) {
        String email = requestDto.getEmail() != null ? requestDto.getEmail().trim().toLowerCase() : "";
        String phone = requestDto.getPhone() != null ? requestDto.getPhone().trim() : "";

        if (phone.isBlank() || isDemoPhoneNumber(phone) || phone.length() < 10) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please provide a valid 10-digit mobile number."));
        }

        User user = userDao.findByUsernameAndActive(email, true);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found for email: " + email));
        }

        Organization org = new Organization();
        org.setEmail(email);
        org.setOrganizationPhonenumber(PhoneNumberUtils.normalize(phone));

        try {
            smsService.sendOtpToMobile(org);
            log.info("Phone OTP dispatched for email {} to phone {}", email, phone);
            return ResponseEntity.ok(ApiResponse.success("OTP sent to mobile number.", null));
        } catch (Exception e) {
            log.error("Failed to send phone OTP to {}: {}", phone, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to send OTP to mobile: " + e.getMessage()));
        }
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/verify-phone-otp")
    public ResponseEntity<ApiResponse<String>> verifyPhoneOtp(@RequestBody BuyerOtpRequest requestDto) {
        String email = requestDto.getEmail() != null ? requestDto.getEmail().trim().toLowerCase() : "";
        String phone = requestDto.getPhone() != null ? PhoneNumberUtils.normalize(requestDto.getPhone().trim()) : "";
        String otp = requestDto.getOtp() != null ? requestDto.getOtp().trim() : "";

        User user = userDao.findByUsernameAndActive(email, true);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found for email: " + email));
        }

        Organization org = new Organization();
        org.setEmail(email);
        org.setOrganizationPhonenumber(phone);
        org.setMobileOtp(otp);

        boolean isValid = smsService.validateMobileOtp(org);
        if (isValid) {
            user.setPhone(phone);
            user.setVerificationStatus(StatusConstants.PHONE_VERIFIED);
            userDao.save(user);

            if (user.getOrg() != null) {
                user.getOrg().setOrganizationPhonenumber(phone);
                clientDao.save(user.getOrg());
            }

            log.info("Phone OTP verified and updated for user {}: phone={}", email, phone);
            return ResponseEntity.ok(ApiResponse.success("Phone verified successfully.", null));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired mobile OTP."));
        }
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/complete-profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeProfile(@RequestBody BuyerProfileCompletionRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        log.info("Profile completion request received for buyer: {}", email);

        User user = userDao.findByUsernameAndActive(email, true);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found for email: " + email));
        }

        // Validate pincode
        String pincode = request.getPincode() != null ? request.getPincode().trim() : "";
        if (pincode.isBlank() || !pincode.matches("^[1-9][0-9]{5}$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please provide a valid 6-digit Indian pincode."));
        }

        // Validate phone
        String phone = request.getPhone() != null ? PhoneNumberUtils.normalize(request.getPhone().trim()) : user.getPhone();
        if (phone == null || phone.isBlank() || isDemoPhoneNumber(phone)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("A valid mobile number is required to complete registration."));
        }

        // Resolve city / state from pincode if not provided
        PincodeData pincodeData = pincodeDao.findByPincode(pincode);
        String city = request.getCity() != null && !request.getCity().isBlank() ? request.getCity().trim()
                : (pincodeData != null && pincodeData.getCity() != null ? pincodeData.getCity() : "");
        String state = request.getState() != null && !request.getState().isBlank() ? request.getState().trim()
                : (pincodeData != null && pincodeData.getState() != null ? pincodeData.getState() : "");

        if (pincodeData == null && !city.isBlank() && !state.isBlank()) {
            try {
                pincodeDao.save(new PincodeData(pincode, city, state));
            } catch (Exception e) {
                log.warn("Could not save new pincode data: {}", e.getMessage());
            }
        }

        // Update Organization
        Organization org = user.getOrg();
        if (org != null) {
            if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
                org.setCompanyName(request.getCompanyName().trim());
            }
            org.setAddress1(request.getAddress1() != null ? request.getAddress1().trim() : org.getAddress1());
            org.setAddress2(request.getAddress2() != null ? request.getAddress2().trim() : org.getAddress2());
            org.setCity(city);
            org.setState(state);
            org.setZipCode(pincode);
            org.setOrganizationPhonenumber(phone);
            if (request.getGstin() != null && !request.getGstin().isBlank()) {
                org.setGstin(request.getGstin().trim());
            }
            clientDao.save(org);
        }

        // Update User
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        user.setPhone(phone);
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);
        userDao.save(user);

        // Sync or update rfq_buyers cache record
        try {
            String orgId = org != null && org.getId() != null ? String.valueOf(org.getId()) : null;
            String userId = String.valueOf(user.getId());
            String compName = org != null ? org.getCompanyName() : user.getFullName();
            String address = ((org != null && org.getAddress1() != null ? org.getAddress1() : "") + " "
                    + (org != null && org.getAddress2() != null ? org.getAddress2() : "")).trim();

            BuyerEntity buyerEntity = buyerRepository.findByEmailIgnoreCase(email).orElse(null);
            if (buyerEntity == null) {
                buyerEntity = BuyerEntity.builder()
                        .email(email)
                        .contactPerson(user.getFullName())
                        .companyName(compName)
                        .phone(phone)
                        .verified(true)
                        .orgId(orgId)
                        .userId(userId)
                        .city(city)
                        .state(state)
                        .pincode(pincode)
                        .address(address.isBlank() ? null : address)
                        .build();
            } else {
                buyerEntity.setVerified(true);
                buyerEntity.setContactPerson(user.getFullName());
                buyerEntity.setCompanyName(compName);
                buyerEntity.setPhone(phone);
                buyerEntity.setCity(city);
                buyerEntity.setState(state);
                buyerEntity.setPincode(pincode);
                buyerEntity.setAddress(address.isBlank() ? null : address);
            }
            buyerRepository.save(buyerEntity);
        } catch (Exception e) {
            log.warn("Could not sync buyer entity to rfq_buyers table: {}", e.getMessage());
        }

        log.info("Buyer profile completed for {}. Resuming any pending RFQs...", email);

        // Resume deferred RFQs automatically
        int resumedCount = 0;
        try {
            resumedCount = pendingRfqResumeService.resumePendingRfqs(email);
        } catch (Exception e) {
            log.error("Error while resuming pending RFQs for {}: {}", email, e.getMessage(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("email", email);
        result.put("verificationStatus", StatusConstants.PROFILE_COMPLETED);
        result.put("resumedRfqsCount", resumedCount);
        result.put("message", "Profile completed successfully. " + resumedCount + " pending RFQ(s) have been processed.");

        return ResponseEntity.ok(ApiResponse.success("Profile completed successfully.", result));
    }

    @Autowired(required = false)
    private AcknowledgementEmailService acknowledgementEmailService;

    @org.springframework.beans.factory.annotation.Value("${app.rfq.buyer-portal-url:https://p2pv1dev-ana9azfph7chftea.centralindia-01.azurewebsites.net/login}")
    private String buyerPortalUrl;

    @org.springframework.beans.factory.annotation.Value("${app.rfq.demo-phone:9999999991}")
    private String demoPhone;

    @PostMapping("/resend-credentials")
    public ResponseEntity<ApiResponse<String>> resendCredentials(
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "Welcome@123") String password,
            @RequestParam(required = false) String phone) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userDao.findByUsernameAndActive(normalizedEmail, true);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found for email: " + normalizedEmail));
        }
        user.setPassword(password);

        String phoneToSet = (phone != null && !phone.isBlank())
                ? PhoneNumberUtils.normalize(phone.trim())
                : PhoneNumberUtils.normalize(demoPhone);

        if (StatusConstants.DEMO_BUYER.equals(user.getVerificationStatus())
                || user.getPhone() == null
                || isDemoPhoneNumber(user.getPhone())) {
            user.setPhone(phoneToSet);
            if (user.getOrg() != null) {
                user.getOrg().setOrganizationPhonenumber(phoneToSet);
                clientDao.save(user.getOrg());
            }
        }
        userDao.save(user);

        if (acknowledgementEmailService != null) {
            String buyerName = user.getFullName() != null ? user.getFullName() : "Valued Customer";
            acknowledgementEmailService.sendDemoBuyerRegistrationEmail(normalizedEmail, buyerName, buyerPortalUrl, password);
        }

        return ResponseEntity.ok(ApiResponse.success("Credentials updated to '" + password + "' (phone: " + user.getPhone() + ") and registration email dispatched to " + normalizedEmail, null));
    }

    @PostMapping("/update-credentials")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCredentials(
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "Welcome@123") String password,
            @RequestParam(required = false, defaultValue = "9999999991") String phone) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userDao.findByUsernameAndActive(normalizedEmail, true);
        if (user == null && demoBuyerRegistrationService != null) {
            user = demoBuyerRegistrationService.createDemoBuyer(normalizedEmail, "New Buyer");
        }
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found for email: " + normalizedEmail));
        }
        String normalizedPhone = PhoneNumberUtils.normalize(phone);
        user.setPassword(password);
        user.setPhone(normalizedPhone);
        user.setResetPassword(true);
        user.setCreatedTS(new java.util.Date());
        user.setActivityTs(null);
        if (isDemoPhoneNumber(normalizedPhone)) {
            user.setVerificationStatus(StatusConstants.DEMO_BUYER);
        }
        if (user.getOrg() != null) {
            user.getOrg().setOrganizationPhonenumber(normalizedPhone);
            clientDao.save(user.getOrg());
        }
        userDao.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("email", normalizedEmail);
        data.put("phone", user.getPhone());
        data.put("password", password);
        data.put("verificationStatus", user.getVerificationStatus());
        data.put("resetPassword", user.isResetPassword());
        return ResponseEntity.ok(ApiResponse.success("Credentials updated successfully.", data));
    }

    @PostMapping("/simulate-expiry")
    public ResponseEntity<ApiResponse<String>> simulateExpiry(
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "65") int minutesAgo) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userDao.findByUsernameAndActive(normalizedEmail, true);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found: " + normalizedEmail));
        }
        long pastTime = System.currentTimeMillis() - (minutesAgo * 60 * 1000L);
        user.setCreatedTS(new java.util.Date(pastTime));
        user.setActivityTs(null);
        userDao.save(user);
        return ResponseEntity.ok(ApiResponse.success("Account creation time set to " + minutesAgo + " minutes ago with activityTs = null.", null));
    }

    @PostMapping("/cleanup-expired")
    public ResponseEntity<ApiResponse<String>> triggerCleanupExpired() {
        if (demoBuyerCleanupService != null) {
            demoBuyerCleanupService.cleanupExpiredDemoBuyers();
        }
        return ResponseEntity.ok(ApiResponse.success("Cleanup completed.", null));
    }
}

