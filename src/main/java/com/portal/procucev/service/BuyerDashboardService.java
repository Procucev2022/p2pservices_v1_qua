package com.portal.procucev.service;

import java.util.List;

import com.portal.procucev.Dto.buyer.BuyerDashboardDto;

public interface BuyerDashboardService {

    BuyerDashboardDto.SummaryResponse getSummary(String buyerOrgId, String buyerId, String username);

    List<BuyerDashboardDto.RFQPipelineItemDto> getPipelineRfqs(String buyerOrgId, String buyerId, String username);

    List<BuyerDashboardDto.LiveFeedItemDto> getLiveFeed(String buyerOrgId, String buyerId, String username, String channel);

    BuyerDashboardDto.ChaserResponseDto triggerChaser(BuyerDashboardDto.ChaserRequestDto request, String username);

    BuyerDashboardDto.PoApprovalResponseDto approvePurchaseOrder(BuyerDashboardDto.PoApprovalRequestDto request, String username);

    List<BuyerDashboardDto.VendorEvaluationDto> getVendorEvaluations(String buyerOrgId);

    List<BuyerDashboardDto.SubscriptionPlanDto> getSubscriptionPlans(String buyerOrgId);
}
