package com.portal.procucev.dao;

import java.util.Date;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.Dto.SellerReportDto;
import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;

public interface GmtRfqVendorDao extends JpaRepository<GmtRfqVendors, String> {

	GmtRfqVendors findByVendorAndRfq(Organization org, Rfq rfq);

//	@Query("SELECT  new GmtRfqVendors(g.id,g.vendor.id,g.vendor.companyName,g.vendor.companyId,g.status,g.query,g.vendor.otherEmails,g.quoteSubmittedDate) from GmtRfqVendors g where g.rfq =:rfq Order By g.createdTS DESC")
//	List<GmtRfqVendors> findByRfq(@Param("rfq") Rfq rfq);
	@Query("""
			SELECT g
			FROM GmtRfqVendors g
			WHERE g.rfq = :rfq
			ORDER BY g.createdTS DESC
			""")
	List<GmtRfqVendors> findByRfq(@Param("rfq") Rfq rfq);

	@Modifying
	@Transactional
	@Query("UPDATE GmtRfqVendors g SET g.status = :resultStatus WHERE g.rfq = :rfq and g.vendor= :vendor")
	void updateStatus(@Param("rfq") Rfq rfq, @Param("vendor") Organization vendor,
			@Param("resultStatus") MasterStatus resultStatus);

	@Modifying
	@Transactional
	@Query("UPDATE GmtRfqVendors g SET g.query = :query,g.status = :resultStatus  WHERE g.rfq.id = :rfq and g.vendor.id= :vendor")
	void updateQuery(@Param("rfq") String rfq, @Param("vendor") String vendor, @Param("query") String query,
			@Param("resultStatus") MasterStatus resultStatus);

	@Modifying
	@Transactional
	@Query("UPDATE GmtRfqVendors g SET g.status = :resultStatus, g.acceptedDate= :date WHERE g.rfq = :rfq and g.vendor= :vendor")
	void updateAcceptStatus(@Param("rfq") Rfq rfq, @Param("vendor") Organization vendor,
			@Param("resultStatus") MasterStatus resultStatus, @Param("date") Date date);

	@Query("SELECT gv FROM GmtRfqVendors gv " + "WHERE gv.vendor.id = :vendorId " + "AND gv.quotationReceived = false "
			+ "ORDER BY gv.requestedDate ASC")
	List<GmtRfqVendors> findLastOpenRfqsByVendor(@Param("vendorId") String vendorId, Pageable pageable);

	@Modifying
	@Transactional
	@Query("UPDATE  GmtRfqVendors r SET r.quotationReceived = true, r.status=:quoteStatus, r.quoteSubmittedDate = COALESCE(r.quoteSubmittedDate, CURRENT_TIMESTAMP) WHERE  r.rfq.id=:rfqId and r.vendor.id=:vendorId")
	void updateQuotationReceived(@Param("rfqId") String rfqId, @Param("vendorId") String vendorId,
			@Param("quoteStatus") MasterStatus quoteStatus);

	@Query("""
			SELECT v
			FROM GmtRfqVendors v
			WHERE v.vendor.id = :vendorUuid
			  AND v.rfq.rfqId IN :rfqIds
			""")
	List<GmtRfqVendors> findByVendorUuidAndRfqIds(@Param("vendorUuid") String vendorUuid,
			@Param("rfqIds") List<String> rfqIds);

	@Query("""
			SELECT v
			FROM GmtRfqVendors v
			WHERE v.vendor.id = :vendorUuid
			ORDER BY v.requestedDate DESC
			""")
	List<GmtRfqVendors> findLatest5ByVendorUuid(@Param("vendorUuid") String vendorUuid, Pageable pageable);

	@Query("select count(r.vendor) from GmtRfqVendors r where r.rfq.id=:id")
	long findByVendorsByRfq(@Param("id") String id);

	@Query("""
			SELECT g
			FROM GmtRfqVendors g
			WHERE g.vendor = :org
			AND g.rfq.id IN :rfqList
			""")
	List<GmtRfqVendors> findByVendorAndRfqIn(@Param("org") Organization org, @Param("rfqList") List<String> rfqIds);

	@Query("""
		    SELECT new com.portal.procucev.Dto.SellerReportDto(
		        r.createdTS,
		        r.rfqId,
		        COALESCE(NULLIF(r.category, ''), (SELECT MAX(ri.category) FROM RfqItem ri WHERE ri.rfq = r AND ri.category IS NOT NULL AND ri.category <> ''), o.vendorcategory),
		        o.companyName,
		        o.email,
		        o.organizationPhonenumber,
		        o.city,
		        CONCAT(d.city, ', ', d.state, ' - ', d.pincode), 
		        u.activityTs,
		        r.sourceType,
		        v.requestedDate,
		        v.quoteSubmittedDate,
		        v.query
		    )
		    FROM GmtRfqVendors v
		    JOIN v.rfq r
		    JOIN v.vendor o
		    LEFT JOIN r.clientdeliverylocationrfq d ON d.rfq.id = r.id
		    LEFT JOIN User u ON u.org.id = o.id
		    WHERE r.createdTS BETWEEN :startDate AND :endDate
		""")
		List<SellerReportDto> getSellerReport(
		        @Param("startDate") Date startDate,
		        @Param("endDate") Date endDate
		);
	
	@Query("""
			SELECT g.rfq.id, COUNT(g.id)
			FROM GmtRfqVendors g
			WHERE g.rfq.id IN :rfqIds
			GROUP BY g.rfq.id
			""")
			List<Object[]> countVendorsByRfqIds(
			        @Param("rfqIds") List<String> rfqIds);

}
