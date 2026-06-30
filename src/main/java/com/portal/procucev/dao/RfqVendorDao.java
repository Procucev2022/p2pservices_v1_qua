package com.portal.procucev.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqVendor;

import jakarta.transaction.Transactional;

public interface RfqVendorDao extends JpaRepository<RfqVendor, String> {

	RfqVendor findByRfq(Rfq rfq);

	RfqVendor findByRfqAndOrganization(Rfq rfq, Organization orgId);

	@Query("select r from RfqVendor r where r.rfq.id=:id")
	List<RfqVendor> findDataByRfqId(@Param("id") String id);

	@Query("select count(r.organization) from RfqVendor r where r.rfq.id=:id")
	long findByVendorsByRfq(@Param("id") String id);

	@Modifying
	@Transactional
	@Query("UPDATE  RfqVendor r SET r.quotationReceived = true, r.vendorStatus=:quoteStatus WHERE  r.rfqId=:rfqId and r.organization.id=:vendorId")
	void updateQuotationReceived(@Param("rfqId") String rfqId,@Param("vendorId") String vendorId, @Param("quoteStatus") MasterStatus quoteStatus);

	@Query(value = """
		    SELECT *
		    FROM rfq_vendors
		    WHERE organization_uuid = :organizationId
		    ORDER BY created_ts DESC
		    LIMIT 1
		    """, nativeQuery = true)
		RfqVendor findLatestByOrganizationUuid(@Param("organizationId") String organizationId);
	
	@Query("""
		    SELECT COUNT(r)
		    FROM RfqVendor r
		    WHERE r.organization.id = :organizationId
		      AND r.isRfqNotified = 1
		""")
		long countCredentialEmailsSent(@Param("organizationId") String organizationId);

}
