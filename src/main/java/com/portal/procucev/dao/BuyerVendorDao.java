package com.portal.procucev.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.model.BuyerVendor;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

public interface BuyerVendorDao extends JpaRepository<BuyerVendor, String> {

    @Query("""
        SELECT v FROM BuyerVendor v
        WHERE v.buyerOrgId = :buyerOrgId
          AND (:status IS NULL OR v.status = :status)
          AND (:industry IS NULL OR v.typeOfIndustry = :industry)
          AND (
            :search IS NULL OR :search = '' OR
            LOWER(v.vendorName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(v.searchTerm) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        ORDER BY v.createdTS DESC
    """)
    Page<BuyerVendor> findByBuyerOrgFiltered(
        @Param("buyerOrgId") String buyerOrgId,
        @Param("status") String status,
        @Param("industry") String industry,
        @Param("search") String search,
        Pageable pageable
    );

    List<BuyerVendor> findByBuyerOrgId(String buyerOrgId);

    Optional<BuyerVendor> findByVendorCodeAndBuyerOrgId(String vendorCode, String buyerOrgId);

    Optional<BuyerVendor> findByIdAndBuyerOrgId(String id, String buyerOrgId);

    boolean existsByVendorCodeAndBuyerOrgId(String vendorCode, String buyerOrgId);

    @Transactional
    @Modifying
    @Query("UPDATE BuyerVendor v SET v.status = :status WHERE v.id = :id AND v.buyerOrgId = :buyerOrgId")
    int updateStatus(@Param("id") String id, @Param("buyerOrgId") String buyerOrgId, @Param("status") String status);

    @Transactional
    @Modifying
    void deleteByIdAndBuyerOrgId(String id, String buyerOrgId);

    @Transactional
    @Modifying
    void deleteByVendorCodeAndBuyerOrgId(String vendorCode, String buyerOrgId);

    @Transactional
    @Modifying
    @Query("DELETE FROM BuyerVendor v WHERE v.buyerOrgId = :buyerOrgId AND (v.vendorCode IN :vendorCodes OR v.id IN :vendorCodes)")
    int deleteByCodesOrIdsAndBuyerOrgId(@Param("vendorCodes") List<String> vendorCodes, @Param("buyerOrgId") String buyerOrgId);

    @Query("""
        SELECT v FROM BuyerVendor v
        WHERE v.status = 'Active'
          AND (:industry IS NULL OR :industry = '' OR LOWER(v.typeOfIndustry) LIKE LOWER(CONCAT('%', :industry, '%')) OR LOWER(v.typeOfBusiness) LIKE LOWER(CONCAT('%', :industry, '%')))
        ORDER BY v.createdTS DESC
    """)
    List<BuyerVendor> findProcucevNetworkVendors(@Param("industry") String industry, Pageable pageable);

    @Query("""
        SELECT v FROM BuyerVendor v
        WHERE v.status = 'Active'
        ORDER BY v.createdTS DESC
    """)
    List<BuyerVendor> findAllProcucevNetworkVendors(Pageable pageable);
}
