package com.portal.procucev.dao;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.Dto.BuyerSummaryDto;
import com.portal.procucev.Dto.SellerSummaryDto;
import com.portal.procucev.Dto.VendorRFQDto;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.SubscriptionPlan;

import jakarta.transaction.Transactional;

public interface OrgDao  extends JpaRepository<Organization, String> {
	
	Organization findByCompanyName(String orgName);
	
	List<Organization> findByPanAndOrgType(String pan, OrgType orgTypeObject);

	List<Organization> findByCrnAndOrgType(String crn, OrgType orgTypeObject);

	@Query("select o.email from Organization o where o.id=:id")
	String findEmailById(@Param("id") String id);

	@Query("select o.otherEmails from Organization o where o.id=:id")
	String findOtherEmailById(@Param("id") String id);

	@Query("SELECT v.id,v.companyName,v.companyId,v.organizationPhonenumber,v.city,v.email from Organization v where v.orgType=:orgType Order By v.createdTS DESC")
	List<Object[]> getAllVendor(@Param("orgType") OrgType orgType);
	
//	@Query("""
//			SELECT new com.portal.procucev.Dto.VendorRFQDto(
//			       v.id,
//			       v.companyName,
//			       v.companyId,
//			       v.organizationPhonenumber,
//			       v.city,
//			       v.email
//			)
//			FROM Organization v
//			WHERE v.orgType=:orgType
//			ORDER BY v.createdTS DESC
//			""")
//			List<VendorRFQDto> getAllVendor(@Param("orgType") OrgType orgType);
	
	@Query("""
			SELECT new com.portal.procucev.Dto.VendorRFQDto(
			       v.id,
			       v.companyName,
			       v.companyId,
			       v.organizationPhonenumber,
			       v.city,
			       v.email
			)
			FROM Organization v
			WHERE v.orgType=:orgType
			ORDER BY v.createdTS DESC
			""")
			Page<VendorRFQDto> getAllVendor(@Param("orgType") OrgType orgType,Pageable pageable);
	
	//getAllVendors Search
	@Query("""
	        SELECT new com.portal.procucev.Dto.VendorRFQDto(
	               v.id,
	               v.companyName,
	               v.companyId,
	               v.organizationPhonenumber,
	               v.city,
	               v.email
	        )
	        FROM Organization v
	        WHERE v.orgType = :orgType
	          AND (
	            (:searchType IN ('email') AND LOWER(v.email) LIKE LOWER(CONCAT('%', :searchValue, '%')))
	            OR
	            (:searchType IN ('mobileNumber', 'organizationPhonenumber', 'mobile', 'phone') AND LOWER(v.organizationPhonenumber) LIKE LOWER(CONCAT('%', :searchValue, '%')))
	            OR
	            (:searchType IN ('sellerName', 'companyName', 'vendorName', 'name', 'company') AND LOWER(v.companyName) LIKE LOWER(CONCAT('%', :searchValue, '%')))
	            OR
	            (:searchType IN ('city') AND LOWER(v.city) LIKE LOWER(CONCAT('%', :searchValue, '%')))
	            OR
	            (:searchType IN ('vendorcategory', 'category') AND (LOWER(v.vendorcategory) LIKE LOWER(CONCAT('%', :searchValue, '%')) OR LOWER(v.subCategory) LIKE LOWER(CONCAT('%', :searchValue, '%'))))
	          )
	        ORDER BY v.createdTS DESC
	        """)
	List<VendorRFQDto> searchVendorByType(
	        @Param("orgType")     OrgType orgType,
	        @Param("searchType")  String searchType,
	        @Param("searchValue") String searchValue
	);

	@Query("""
	        SELECT new com.portal.procucev.Dto.VendorRFQDto(
	               v.id,
	               v.companyName,
	               v.companyId,
	               v.organizationPhonenumber,
	               v.city,
	               v.email
	        )
	        FROM Organization v
	        WHERE v.orgType = :orgType
	          AND LOWER(v.email) IN :emails
	        ORDER BY v.createdTS DESC
	        """)
	List<VendorRFQDto> searchVendorByEmails(
	        @Param("orgType") OrgType orgType,
	        @Param("emails") List<String> emails
	);

	@Query("""
	        SELECT DISTINCT v.city
	        FROM Organization v
	        WHERE v.orgType = :orgType
	          AND v.city IS NOT NULL AND TRIM(v.city) != ''
	          AND (LOWER(v.vendorcategory) LIKE LOWER(CONCAT('%', :category, '%')) OR LOWER(v.subCategory) LIKE LOWER(CONCAT('%', :category, '%')))
	        ORDER BY v.city ASC
	        """)
	List<String> findCitiesByVendorCategory(
	        @Param("orgType") OrgType orgType,
	        @Param("category") String category
	);

	@Query("""
	        SELECT new com.portal.procucev.Dto.VendorRFQDto(
	               v.id,
	               v.companyName,
	               v.companyId,
	               v.organizationPhonenumber,
	               v.city,
	               v.email
	        )
	        FROM Organization v
	        WHERE v.orgType = :orgType
	          AND (LOWER(v.vendorcategory) LIKE LOWER(CONCAT('%', :category, '%')) OR LOWER(v.subCategory) LIKE LOWER(CONCAT('%', :category, '%')))
	          AND (:city IS NULL OR :city = '' OR LOWER(v.city) = LOWER(:city) OR LOWER(v.city) LIKE LOWER(CONCAT('%', :city, '%')))
	        ORDER BY v.createdTS DESC
	        """)
	List<VendorRFQDto> searchVendorByCategoryAndCity(
	        @Param("orgType") OrgType orgType,
	        @Param("category") String category,
	        @Param("city") String city
	);

	@Query("""
	        SELECT new com.portal.procucev.Dto.VendorRFQDto(
	               v.id,
	               v.companyName,
	               v.companyId,
	               v.organizationPhonenumber,
	               v.city,
	               v.email
	        )
	        FROM Organization v
	        WHERE v.orgType = :orgType
	          AND (
	            LOWER(v.vendorcategory) LIKE LOWER(CONCAT('%', :category, '%'))
	            OR LOWER(v.subCategory) LIKE LOWER(CONCAT('%', :category, '%'))
	            OR LOWER(v.clientCategory) LIKE LOWER(CONCAT('%', :category, '%'))
	          )
	          AND (
	            :state IS NULL OR :state = ''
	            OR v.state IS NULL OR TRIM(v.state) = ''
	            OR LOWER(v.state) LIKE LOWER(CONCAT('%', :state, '%'))
	            OR LOWER(v.city) LIKE LOWER(CONCAT('%', :state, '%'))
	            OR (v.address1 IS NOT NULL AND LOWER(v.address1) LIKE LOWER(CONCAT('%', :state, '%')))
	            OR (v.address2 IS NOT NULL AND LOWER(v.address2) LIKE LOWER(CONCAT('%', :state, '%')))
	            OR (:city IS NOT NULL AND :city != '' AND LOWER(v.city) LIKE LOWER(CONCAT('%', :city, '%')))
	          )
	          AND (
	            :city IS NULL OR :city = ''
	            OR LOWER(v.city) LIKE LOWER(CONCAT('%', :city, '%'))
	            OR (v.address1 IS NOT NULL AND LOWER(v.address1) LIKE LOWER(CONCAT('%', :city, '%')))
	            OR (v.address2 IS NOT NULL AND LOWER(v.address2) LIKE LOWER(CONCAT('%', :city, '%')))
	            OR (v.state IS NOT NULL AND LOWER(v.state) LIKE LOWER(CONCAT('%', :city, '%')))
	          )
	        ORDER BY v.createdTS DESC
	        """)
	List<VendorRFQDto> searchVendorByCategoryStateAndCity(
	        @Param("orgType") OrgType orgType,
	        @Param("category") String category,
	        @Param("state") String state,
	        @Param("city") String city
	);

	@Query("SELECT v.id, v.companyName, v.companyId, v.organizationPhonenumber, v.city, v.email " +
	        "FROM Organization v " +
	        "WHERE v.vendorcategory LIKE CONCAT('%', :category, '%') OR v.subCategory LIKE CONCAT('%', :category, '%') " +
	        "ORDER BY v.createdTS DESC")
	List<VendorRFQDto> getAllVendorByCategory(@Param("category") String category);
	
	@Transactional
	@Modifying
	@Query("update Organization o set o.email = :email where o.id=:id")
	void updateEmailByOrg(@Param("id") String id,@Param("email") String email);
	
	@Transactional
	@Modifying
	@Query("update Organization o set o.otherEmails = :otherEmails where o.id=:id")
	void updateOtherEmail(@Param("otherEmails") String otherEmails, @Param("id") String id);

	List<Organization> findByOrgTypeAndSelfClient(OrgType orgTypeObject, boolean b, Sort by);

	@Transactional
	@Modifying
	@Query("update Organization o set o.clientStatus = :status where o.id=:id")
	void updateClientStatus(@Param("status") MasterStatus status, @Param("id") String id);

	List<Organization> findByCompanyNameAndOrgType(String companyName, OrgType orgTypeObject);

	@Query("select o.rfqCredits from Organization o where o.id=:id")
	int findRfqCreditsByOrg(@Param("id") String id);

	List<Organization> findByOrgType(OrgType orgTypeObject);

	@Query("select o.rfqCredits from Organization o where o.id=:id")
	Integer findRfqCreditsDataByOrg(@Param("id") String id);

	@Transactional
	@Modifying
	@Query("update Organization o set o.rfqCredits = :availableCredits where o.id=:id")
	void updateRfqCredits(@Param("id") String id, @Param("availableCredits") Integer availableCredits);

	@Modifying
    @Transactional
    @Query("UPDATE Organization o " +
           "SET o.rfqCredits = o.rfqCredits - 1, " +
           "    o.rfqUsedCount = o.rfqUsedCount + 1 " +
           "WHERE o.id = :id AND o.rfqCredits > 0")
	int updateRfqCreditsAndUsage(@Param("id") String id);
	
	@Query("SELECT new Organization(o.id,o.companyName,o.pan,o.address1,o.city,o.email,o.orgType,o.companyId) FROM Organization o WHERE o.orgType IN (:client, :vendor) AND LOWER(o.companyName) LIKE LOWER(CONCAT('%', :companyName, '%'))")
	List<Organization> findOrganizationsByTypeAndNameIgnoreCase(@Param("client") OrgType client,
			@Param("vendor") OrgType vendor, @Param("companyName") String companyName);

	@Modifying
    @Transactional
    @Query("UPDATE Organization o SET o.quoteSubmitted = o.quoteSubmitted + 1 WHERE o.id = :vendorId ")
	void updateQuoteCount(@Param("vendorId") String vendorId);

	@Modifying
	@Transactional
    @Query("UPDATE Organization o SET o.vendorClass = :vendorClass WHERE o.id = :id")
    void updateVendorClass(@Param("id") String id, @Param("vendorClass") String vendorClass);

	@Modifying
	@Transactional
    @Query("UPDATE Organization o SET o.subscriptionStart = :startDateUtil, o.subscriptionExpiry = :endDateUtil, o.subscriptionPlan =:subsPaln, o.rfqCredits = o.rfqCredits + 50 WHERE o.id = :id")
	void updateUpgradeVendorData(@Param("startDateUtil")  Date startDateUtil,@Param("endDateUtil")  Date endDateUtil,@Param("subsPaln") SubscriptionPlan subsPaln, @Param("id") String id);

	@Query("select o.city from Organization o where o=:org")
	String getCityByOrg(@Param("org") Organization org);

	Page<Organization> findByOrgType(OrgType orgTypeObject, Pageable pageable);


	    @Query("""
	        SELECT o
	        FROM Organization o
	        WHERE o.orgType = :orgType
	          AND (:sourceType IS NULL OR o.sourceType = :sourceType)
	          AND (
	                :search IS NULL OR :search = '' OR
	                LOWER(o.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR
	                LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) 
	                )
	    """)
	Page<Organization> findVendors(@Param("orgType") OrgType orgType, @Param("sourceType") String sourceType,  @Param("search") String search, Pageable pageable);

	    
	    @Query("""
	    		SELECT new com.portal.procucev.Dto.SellerSummaryDto(
	    		    o.companyName,
	    		    o.organizationPhonenumber,
	    		    o.email,
	    		    o.city,
	    		    o.vendorClass,
	    		    CASE WHEN o.subscriptionPlan IS NOT NULL THEN 'Yes' ELSE 'No' END,
	    		    o.subscriptionStart,
	    		    sp.planName,
	    		    o.subscriptionExpiry,
	    		    o.rfqCredits,
	    		    u.activityTs,
	    		    o.rfqUsedCount,
	    		    o.quoteSubmitted,
	    		    (o.rfqCredits - o.rfqUsedCount),
	    		    SIZE(o.divisionCategories),
	    		    o.sourceType
	    		)
	    		FROM Organization o
	    		LEFT JOIN o.subscriptionPlan sp
	    		LEFT JOIN User u ON u.org = o
	    		WHERE o.orgType = :orgType
	    		  AND o.createdTS BETWEEN :fromDate AND :toDate
	    		ORDER BY o.createdTS DESC
	    		""")
	    		List<SellerSummaryDto> getSellerSummary(
	    		        @Param("orgType") OrgType orgType,
	    		        @Param("fromDate") Date fromDate,
	    		        @Param("toDate") Date toDate
	    		);
	    
	    
	    @Query("""
	    	    SELECT new com.portal.procucev.Dto.BuyerSummaryDto(
	    	        u.fullName,
	    	        u.phone,
	    	        o.companyName,
	    	        u.username,
	    	        o.city,
	    	        COUNT(DISTINCT r.id),
	    	        CAST(DATEDIFF(CURRENT_DATE, MAX(r.createdTS)) AS long),
	    	        o.clientCategory,
	    	        o.clientCategory,
	    	        u.activityTs,
	    	        CAST(DATEDIFF(CURRENT_DATE, u.activityTs) AS long),
	    	        o.sourceType,
	    	        COUNT(DISTINCT CASE WHEN v.quoteSubmittedDate IS NOT NULL
	    	                            THEN v.id ELSE NULL END),
	    	        COUNT(DISTINCT v.id)
	    	    )
	    	    FROM Organization o
	    	    JOIN User u ON u.org.id = o.id
	    	        AND u.selfClient = true
	    	    LEFT JOIN Rfq r ON r.org.id = o.id
	    	        AND r.createdTS BETWEEN :startDate AND :endDate
	    	    LEFT JOIN GmtRfqVendors v ON v.rfq.id = r.id
	    	    WHERE o.orgType = :orgType
	    	    AND o.createdTS BETWEEN :startDate AND :endDate
	    	    GROUP BY
	    	        u.fullName, u.phone, o.companyName,
	    	        u.username, o.city, o.clientCategory,
	    	        u.activityTs, o.sourceType
	    	    ORDER BY MAX(o.createdTS) DESC
	    	""")
	    	List<BuyerSummaryDto> getBuyerSummary(
	    	        @Param("orgType") OrgType orgType,
	    	        @Param("startDate") Date startDate,
	    	        @Param("endDate") Date endDate
	    	);
	   
	    
//	    @Query(value = """
//	    	    SELECT o.*
//	    	    FROM organization o
//	    	    INNER JOIN org_types ot ON o.org_type_uuid = ot.uuid
//	    	    WHERE ot.type_name = :orgTypeName
//	    	      AND (
//	    	            :searchValue IS NULL OR :searchValue = '' OR
//	    	            (:searchType = 'companyName' AND LOWER(o.organization_name) LIKE LOWER(CONCAT('%', :searchValue, '%'))) OR
//	    	            (:searchType = 'city' AND LOWER(o.city) LIKE LOWER(CONCAT('%', :searchValue, '%'))) OR
//	    	            (:searchType = 'email' AND LOWER(o.email) LIKE LOWER(CONCAT('%', :searchValue, '%'))) OR
//	    	            (:searchType = 'vendorcategory' AND LOWER(o.vendorcategory) LIKE LOWER(CONCAT('%', :searchValue, '%'))) OR
//	    	            (:searchType = 'organizationPhonenumber' AND LOWER(o.organization_phonenumber) LIKE LOWER(CONCAT('%', :searchValue, '%')))
//	    	          )
//	    	    """, nativeQuery = true)
//	    	List<Organization> findVendorsBySearchType(
//	    	    @Param("orgTypeName") String orgTypeName,
//	    	    @Param("searchType") String searchType,
//	    	    @Param("searchValue") String searchValue
//	    	);
	    
	    @Query(value = """
	    	    SELECT DISTINCT o.*
	    	    FROM organization o
	    	    INNER JOIN org_types ot
	    	        ON o.org_type_uuid = ot.uuid
	    	    LEFT JOIN org_division_category odc
	    	        ON odc.organization_id = o.uuid
	    	    WHERE ot.type_name = :orgTypeName
	    	      AND (
	    	            :searchValue IS NULL OR :searchValue = '' OR
	    	            ((:searchType = 'companyName' OR :searchType = 'sellerName' OR :searchType = 'name' OR :searchType = 'company')
	    	                AND LOWER(o.organization_name) LIKE LOWER(CONCAT('%', :searchValue, '%'))) OR
	    	            (:searchType = 'city'
	    	                AND LOWER(o.city) LIKE LOWER(CONCAT('%', :searchValue, '%'))) OR
	    	            (:searchType = 'email'
	    	                AND LOWER(o.email) LIKE LOWER(CONCAT('%', :searchValue, '%'))) OR
	    	            ((:searchType = 'vendorcategory' OR :searchType = 'category')
	    	                AND (LOWER(odc.category) LIKE LOWER(CONCAT('%', :searchValue, '%')) OR LOWER(o.vendorcategory) LIKE LOWER(CONCAT('%', :searchValue, '%')) OR LOWER(o.sub_category) LIKE LOWER(CONCAT('%', :searchValue, '%')))) OR
	    	            ((:searchType = 'organizationPhonenumber' OR :searchType = 'mobileNumber' OR :searchType = 'mobile' OR :searchType = 'phone')
	    	                AND LOWER(o.organization_phonenumber) LIKE LOWER(CONCAT('%', :searchValue, '%'))) OR
	    	            (:searchType IS NULL OR :searchType = ''
	    	                AND (LOWER(o.organization_name) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	    	                  OR LOWER(o.email) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	    	                  OR LOWER(o.city) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	    	                  OR LOWER(o.vendorcategory) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	    	                  OR LOWER(odc.category) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	    	                  OR LOWER(o.organization_phonenumber) LIKE LOWER(CONCAT('%', :searchValue, '%'))))
	    	          )
	    	    """, nativeQuery = true)
	    	List<Organization> findVendorsBySearchType(
	    	    @Param("orgTypeName") String orgTypeName,
	    	    @Param("searchType") String searchType,
	    	    @Param("searchValue") String searchValue
	    	);

	    @Query(value = """
	    	    SELECT DISTINCT o.*
	    	    FROM organization o
	    	    INNER JOIN org_types ot
	    	        ON o.org_type_uuid = ot.uuid
	    	    WHERE ot.type_name = :orgTypeName
	    	      AND LOWER(o.email) IN :emails
	    	    """, nativeQuery = true)
	    	List<Organization> findVendorsByEmails(
	    	    @Param("orgTypeName") String orgTypeName,
	    	    @Param("emails") List<String> emails
	    	);

}