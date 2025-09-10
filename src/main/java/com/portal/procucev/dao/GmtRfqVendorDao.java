package com.portal.procucev.dao;

import java.util.Date;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;

public interface GmtRfqVendorDao extends JpaRepository<GmtRfqVendors, String> {

	GmtRfqVendors findByVendorAndRfq(Organization org, Rfq rfq);

	@Query("SELECT  new GmtRfqVendors(g.id,g.vendor.id,g.vendor.companyName,g.vendor.companyId,g.status,g.query,g.vendor.otherEmails) from GmtRfqVendors g where g.rfq =:rfq Order By g.createdTS DESC")
	List<GmtRfqVendors> findByRfq(Rfq rfq);


	@Modifying
	@Transactional
	@Query("UPDATE GmtRfqVendors g SET g.status = :resultStatus WHERE g.rfq = :rfq and g.vendor= :vendor")
	void updateStatus(Rfq rfq, Organization vendor, MasterStatus resultStatus);

	@Modifying
	@Transactional
	@Query("UPDATE GmtRfqVendors g SET g.query = :query WHERE g.rfq.id = :rfq and g.vendor.id= :vendor")
	void updateQuery(String rfq, String vendor, String query);


	@Modifying
	@Transactional
	@Query("UPDATE GmtRfqVendors g SET g.status = :resultStatus, g.acceptedDate= :date WHERE g.rfq = :rfq and g.vendor= :vendor")
	void updateAcceptStatus(Rfq rfq, Organization vendor, MasterStatus resultStatus, Date date);

	@Query("SELECT gv FROM GmtRfqVendors gv " +
	           "WHERE gv.vendor.id = :vendorId " +
	           "AND gv.quotationReceived = false " +
	           "ORDER BY gv.requestedDate DESC")
	    List<GmtRfqVendors> findLastOpenRfqsByVendor(@Param("vendorId") String vendorId, Pageable pageable);

}
