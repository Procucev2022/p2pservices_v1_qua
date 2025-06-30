package com.portal.procucev.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqVendor;

public interface RfqVendorDao extends JpaRepository<RfqVendor, String> {

	RfqVendor findByRfq(Rfq rfq);

	RfqVendor findByRfqAndOrganization(Rfq rfq, Organization orgId);

	@Query("select r from RfqVendor r where r.rfq.id=:id")
	List<RfqVendor> findDataByRfqId(String id);

	@Query("select count(r.organization) from RfqVendor r where r.rfq.id=:id")
	long findByVendorsByRfq(String id);

	

}
