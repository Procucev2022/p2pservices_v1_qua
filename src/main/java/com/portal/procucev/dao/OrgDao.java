package com.portal.procucev.dao;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;

public interface OrgDao  extends JpaRepository<Organization, String> {
	
	Organization findByCompanyName(String orgName);
	
	List<Organization> findByPanAndOrgType(String pan, OrgType orgTypeObject);

	List<Organization> findByCrnAndOrgType(String crn, OrgType orgTypeObject);
}
