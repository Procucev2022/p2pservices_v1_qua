package com.portal.procucev.service;

import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.BuyerVendorAiProfile;

import java.util.List;
import java.util.Optional;

public interface VendorAiProcessingService {

    List<BuyerVendorAiProfile> processVendorsWithAi(List<BuyerVendor> vendors, String buyerOrgId, String username);

    List<BuyerVendorAiProfile> getAnalyzedVendors(String buyerOrgId);

    Optional<BuyerVendorAiProfile> getVendorAiProfile(String vendorCode, String buyerOrgId);
}
