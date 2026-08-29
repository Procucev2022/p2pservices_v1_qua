package com.portal.procucev.service;

import java.util.List;

import com.portal.procucev.Dto.vendor.VendorDashboardDto;

/**
 * Backing operations for the vendor workspace screens.
 */
public interface VendorDashboardService {

    VendorDashboardDto.ProfileSummaryDto getProfileSummary(String orgId, String vendorId, String username);

    List<VendorDashboardDto.OpportunityDto> getOpportunities(String orgId, String vendorId, String username);

    List<VendorDashboardDto.CatalogueProductDto> getCatalogue(String orgId, String vendorId);

    VendorDashboardDto.CatalogueProductDto saveCatalogueProduct(
            VendorDashboardDto.CatalogueProductDto product, String orgId, String username);

    List<VendorDashboardDto.SubscriptionPlanDto> getSubscriptionPlans(String orgId);

    VendorDashboardDto.SubscribeResponseDto updateSubscription(
            VendorDashboardDto.SubscribeRequestDto request, String orgId, String username);

    VendorDashboardDto.RfqDownloadResponseDto downloadRfqDocuments(
            VendorDashboardDto.RfqDownloadRequestDto request, String orgId, String username);

    VendorDashboardDto.QuotationResponseDto submitQuotation(
            VendorDashboardDto.QuotationRequestDto request, String orgId, String username);

    VendorDashboardDto.QualificationResponseDto submitQualification(
            VendorDashboardDto.QualificationRequestDto request, String orgId, String username);
}
