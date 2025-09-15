package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.VendorCatalogue;

@Repository
public interface VendorCatalogueDao extends JpaRepository<VendorCatalogue, String> {

	  @Query(value = "SELECT v FROM VendorCatalogue v WHERE v.org.id = :id ORDER BY v.createdTS DESC")
	List<VendorCatalogue> findCatalogueByVendor(@Param("id") String id);

}
