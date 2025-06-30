package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.portal.procucev.model.CategoryDivision;

public interface CategoryDivisionDao  extends JpaRepository<CategoryDivision, String>{

	@Query("SELECT distinct(c.division) FROM CategoryDivision c Order By c.division ASC")
	List<String> getAllDivision();

	@Query("SELECT distinct(c.category) FROM CategoryDivision c Order By c.category ASC")
	List<String> getAllCategory();

	@Query("SELECT c.category FROM CategoryDivision c where c.division=:division Order By c.category ASC")
	List<String> getCategoryByDivision(String division);

}
