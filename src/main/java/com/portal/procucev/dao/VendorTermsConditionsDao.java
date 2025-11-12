package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.VendorTermsConditions;

@Repository
public interface VendorTermsConditionsDao extends JpaRepository<VendorTermsConditions, String> {

//	@Query(value = "SELECT v FROM VendorTermsConditions v WHERE v.org.id = :id ORDER BY v.createdTS DESC")
//	List<VendorTermsConditions> findTCByVendor(@Param("id") String id);
	
    List<VendorTermsConditions> findByOrgIdOrderByCreatedTSDesc(String id);

}
