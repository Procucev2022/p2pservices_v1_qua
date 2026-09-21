package com.portal.procucev.rfq.service;

import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.entity.BuyerEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.repository.BuyerRepository;
import com.portal.procucev.utils.StatusConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerVerificationService {

    private final BuyerRepository buyerRepository;
    private final UserDao userDao;

    @Transactional
    public Buyer verifyAndGetBuyer(String email) {
        log.info("Executing Internal Buyer Verification in p2pservices_v1 for email: {}", email);

        if (email == null || email.isBlank()) {
            log.warn("Buyer verification failed: email is null or blank.");
            return Buyer.builder()
                    .email(email)
                    .verified(false)
                    .build();
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 1. Check main portal User database table FIRST (Primary Source of Truth for Portal Users)
        List<User> portalUsers = userDao.findActiveUsersByUsernameAndRoleNames(
                normalizedEmail,
                List.of(StatusConstants.ClientInitiator, StatusConstants.Clientrole)
        );
        User portalUser = portalUsers.isEmpty() ? null : portalUsers.get(0);

        if (portalUser == null) {
            log.warn("Buyer verification failed: Email '{}' is NOT registered in the portal users database.", normalizedEmail);
            log.warn("RFQ creation skipped because sender is not a registered portal buyer.");

            // Clean up any stale cached record in rfq_buyers if user was removed from portal
            try {
                buyerRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(buyerRepository::delete);
            } catch (Exception ignored) {}

            return Buyer.builder()
                    .email(normalizedEmail)
                    .name(extractNameFromEmail(normalizedEmail))
                    .verified(false)
                    .build();
        }

        log.info("Found registered portal user for email: {} (User ID: {})", normalizedEmail, portalUser.getId());
        if (portalUser.getOrg() == null || portalUser.getOrg().getId() == null) {
            log.warn("Buyer verification failed: portal user {} has no organization.", normalizedEmail);
            return Buyer.builder()
                    .email(normalizedEmail)
                    .name(extractNameFromEmail(normalizedEmail))
                    .verified(false)
                    .build();
        }
        String orgId = String.valueOf(portalUser.getOrg().getId());
        String userId = String.valueOf(portalUser.getId());
        String compName = portalUser.getOrg().getCompanyName() != null
                ? portalUser.getOrg().getCompanyName() : "Portal Buyer";

        String personName = ((portalUser.getFirstName() != null ? portalUser.getFirstName() : "") + " " +
                (portalUser.getLastName() != null ? portalUser.getLastName() : "")).trim();
        if (personName.isEmpty()) {
            personName = portalUser.getFullName() != null ? portalUser.getFullName() : normalizedEmail;
        }

        String buyerCity = portalUser.getOrg().getCity();
        String buyerState = portalUser.getOrg().getState();
        String buyerPincode = portalUser.getOrg().getZipCode();

        String a1 = portalUser.getOrg().getAddress1() != null ? portalUser.getOrg().getAddress1().trim() : "";
        String a2 = portalUser.getOrg().getAddress2() != null ? portalUser.getOrg().getAddress2().trim() : "";
        String buyerAddress = (a1 + " " + a2).trim();
        if (buyerAddress.isEmpty()) {
            java.util.List<String> locParts = new java.util.ArrayList<>();
            if (buyerCity != null && !buyerCity.isBlank()) locParts.add(buyerCity.trim());
            if (buyerState != null && !buyerState.isBlank()) locParts.add(buyerState.trim());
            if (buyerPincode != null && !buyerPincode.isBlank()) locParts.add(buyerPincode.trim());
            buyerAddress = locParts.isEmpty() ? null : String.join(", ", locParts);
        }

        // Cache/Sync to rfq_buyers table
        BuyerEntity buyerEntity = buyerRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (buyerEntity == null) {
            buyerEntity = BuyerEntity.builder()
                    .email(normalizedEmail)
                    .contactPerson(personName)
                    .companyName(compName)
                    .phone(portalUser.getPhone())
                    .verified(true)
                    .orgId(orgId)
                    .userId(userId)
                    .city(buyerCity)
                    .state(buyerState)
                    .pincode(buyerPincode)
                    .address(buyerAddress)
                    .build();
            buyerEntity = buyerRepository.save(buyerEntity);
        } else {
            buyerEntity.setVerified(true);
            buyerEntity.setOrgId(orgId);
            buyerEntity.setUserId(userId);
            buyerEntity.setCompanyName(compName);
            buyerEntity.setContactPerson(personName);
            buyerEntity.setPhone(portalUser.getPhone());
            buyerEntity.setCity(buyerCity);
            buyerEntity.setState(buyerState);
            buyerEntity.setPincode(buyerPincode);
            buyerEntity.setAddress(buyerAddress);
            buyerEntity = buyerRepository.save(buyerEntity);
        }

        return Buyer.builder()
                .id(buyerEntity.getId())
                .email(normalizedEmail)
                .name(personName)
                .orgId(orgId)
                .userId(userId)
                .companyName(compName)
                .contactPerson(personName)
                .phone(portalUser.getPhone())
                .city(buyerCity)
                .state(buyerState)
                .pincode(buyerPincode)
                .address(buyerAddress)
                .verified(true)
                .build();
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return "Valued Buyer";
        String prefix = email.substring(0, email.indexOf("@"));
        String[] parts = prefix.split("[._-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        String res = sb.toString().trim();
        return res.isEmpty() ? "Valued Buyer" : res;
    }
}
