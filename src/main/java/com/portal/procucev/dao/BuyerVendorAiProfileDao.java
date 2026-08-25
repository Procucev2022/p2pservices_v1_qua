package com.portal.procucev.dao;

import com.portal.procucev.model.BuyerVendorAiProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuyerVendorAiProfileDao extends JpaRepository<BuyerVendorAiProfile, String> {

    List<BuyerVendorAiProfile> findByBuyerOrgIdOrderByCreatedTSDesc(String buyerOrgId);

    Optional<BuyerVendorAiProfile> findByVendorCodeAndBuyerOrgId(String vendorCode, String buyerOrgId);

    Optional<BuyerVendorAiProfile> findByVendorCode(String vendorCode);

    void deleteByVendorCodeAndBuyerOrgId(String vendorCode, String buyerOrgId);
}
