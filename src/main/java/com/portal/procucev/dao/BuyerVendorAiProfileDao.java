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

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    void deleteByVendorCodeAndBuyerOrgId(String vendorCode, String buyerOrgId);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM BuyerVendorAiProfile p WHERE p.buyerOrgId = :buyerOrgId AND p.vendorCode IN :vendorCodes")
    int deleteByVendorCodesAndBuyerOrgId(@org.springframework.data.repository.query.Param("vendorCodes") List<String> vendorCodes, @org.springframework.data.repository.query.Param("buyerOrgId") String buyerOrgId);
}
