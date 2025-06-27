package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;

@Repository("clientDao")
public interface ClientDao extends JpaRepository<Organization, String> {
	
//	List<Organization> findByCategoryManagerIn(List<User> categoryManager);
	
	
	List<Organization> findByCategoryManager(User categoryManager);

	List<Organization> findByOrgType(OrgType orgTypeObject, Sort sort);

	@Query("SELECT o.companyName from Organization o where o.orgType=:orgTypeObject")
	List<String> findClientByOrgType(OrgType orgTypeObject);

	List<Organization> findByOrgTypeAndSelfClient(OrgType orgTypeObject, boolean b, Sort by);

	@Query("SELECT o from Organization o where o.orgType=:orgTypeObject and o.selfClient = false Order By o.createdTS DESC")
	List<Organization> findByOrgTypeAndSelfClient(OrgType orgTypeObject, Sort by);

	@Query("SELECT o from Organization o where o.orgType=:orgTypeObject and o.selfClient = false and o.id IN (:client) Order By o.createdTS DESC")
	List<Organization> findByOrgTypeAndSelfClientAndClient(OrgType orgTypeObject, Sort by, List<String> client);

}
