package com.portal.procucev.dao;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;

public interface OrgDao  extends JpaRepository<Organization, String> {
	
	Organization findByCompanyName(String orgName);
	
	List<Organization> findByPanAndOrgType(String pan, OrgType orgTypeObject);

	List<Organization> findByCrnAndOrgType(String crn, OrgType orgTypeObject);

	@Query("select o.email from Organization o where o.id=:id")
	String findEmailById(@Param("id") String id);

	@Query("select o.otherEmails from Organization o where o.id=:id")
	String findOtherEmailById(@Param("id") String id);

	@Query("SELECT v.id,v.companyName,v.companyId,v.organizationPhonenumber,v.city,v.email from Organization v Order By v.createdTS DESC")
	List<Object[]> getAllVendor();

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

}
