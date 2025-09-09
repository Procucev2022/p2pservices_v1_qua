package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.model.OrgDivisionCategory;

public interface OrgCategoryDivisionDao  extends JpaRepository<OrgDivisionCategory, String>{

	@Query("select o.category from OrgDivisionCategory o where o.organization.id=:id")
	List<String> findCategoryByOrg(@Param("id") String id);

	@Query("select o from OrgDivisionCategory o where o.userId=:id")
	List<OrgDivisionCategory> findbyUser(@Param("id") String id);


}
