package com.portal.procucev.dao;
import java.util.Date;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.Dto.SellerSummaryDto;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.SubscriptionPlan;

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

	@Query("SELECT v.id, v.companyName, v.companyId, v.organizationPhonenumber, v.city, v.email " +
	        "FROM Organization v " +
	        "WHERE v.vendorcategory LIKE CONCAT('%', :category, '%') OR v.subCategory LIKE CONCAT('%', :category, '%') " +
	        "ORDER BY v.createdTS DESC")
	List<Object[]> getAllVendorByCategory(@Param("category") String category);
	
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


}