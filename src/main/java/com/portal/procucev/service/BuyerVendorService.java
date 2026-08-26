package com.portal.procucev.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.portal.procucev.model.BuyerVendor;

import java.util.Optional;

public interface BuyerVendorService {

    Page<BuyerVendor> getVendors(String buyerOrgId, String status, String industry, String search, Pageable pageable);

    BuyerVendor createVendor(BuyerVendor vendor);

    Optional<BuyerVendor> getVendorById(String id, String buyerOrgId);

    BuyerVendor updateVendor(String id, BuyerVendor vendor, String buyerOrgId);

    boolean updateStatus(String id, String buyerOrgId, String status);

    java.util.Map<String, Object> bulkCreateVendors(java.util.List<BuyerVendor> vendors, String buyerOrgId, String createdBy);

    java.util.List<java.util.Map<String, Object>> getProcucevRecommendations(String category, int limit);
}
