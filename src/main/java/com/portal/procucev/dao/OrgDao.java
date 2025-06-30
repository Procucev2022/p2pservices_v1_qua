package com.portal.procucev.dao;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;

public interface OrgDao  extends JpaRepository<Organization, String> {
	
	Organization findByCompanyName(String orgName);
	
	List<Organization> findByPanAndOrgType(String pan, OrgType orgTypeObject);

	List<Organization> findByCrnAndOrgType(String crn, OrgType orgTypeObject);

	@Query("select o.email from Organization o where o.id=:id")
	String findEmailById(String id);

	@Query("select o.otherEmails from Organization o where o.id=:id")
	String findOtherEmailById(String id);

	@Query("SELECT v.id,v.companyName,v.companyId,v.organizationPhonenumber,v.city,v.email from Organization v Order By v.createdTS DESC")
	List<Object[]> getAllVendor();

	@Query("SELECT v.id, v.companyName, v.companyId, v.organizationPhonenumber, v.city, v.email " +
	        "FROM Organization v " +
	        "WHERE v.vendorcategory LIKE CONCAT('%', :category, '%') OR v.subCategory LIKE CONCAT('%', :category, '%') " +
	        "ORDER BY v.createdTS DESC")
	List<Object[]> getAllVendorByCategory(@Param("category") String category);
}
