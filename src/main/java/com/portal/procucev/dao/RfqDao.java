package com.portal.procucev.dao;

import java.util.Date;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Rfq;

public interface RfqDao extends JpaRepository<Rfq, String>{

	@Query("SELECT r.rfqId FROM Rfq r WHERE  r.user=:user and r.noPrFlag = true Order By r.createdTS DESC")
	List<String> findRFQIdsNoPrRfqByClient(@Param("user") String  user);

	@Query("SELECT r FROM Rfq r WHERE  r.user=:user and r.noPrFlag = true Order By r.createdTS DESC")
	List<Rfq> findNoPrRfqByClient(@Param("user") String user);

	@Query("SELECT r FROM Rfq r WHERE  (r.noPrFlag = true and r.byClient = false)or (r.byClient = true and r.clientStatus =:status )Order By r.createdTS DESC")
	List<Rfq> findAllRfqNoPrByCM(MasterStatus status);

	@Query("SELECT r FROM Rfq r WHERE  r.byClient = true and r.noPrFlag = true Order By r.createdTS DESC")
	List<Rfq> findAllClientRfqNoPr();

	@Modifying
	@Transactional
	@Query("UPDATE Rfq c SET c.count = c.count+1 WHERE c =:rfq")
	void updateCount(@Param("rfq") Rfq rfq);

	@Modifying
	@Transactional
	@Query("UPDATE Rfq c SET c.count = c.count-1 WHERE c =:rfq")
	void updateRfqCount(@Param("rfq") Rfq rfq);

	@Modifying
	@Transactional
	@Query("UPDATE  Rfq r SET r.quotationReceived = true, r.quoteCount = r.quoteCount + 1, r.quoteSubmittedDate = COALESCE(r.quoteSubmittedDate, CURRENT_TIMESTAMP), r.clientStatus= :quoteStatus WHERE  r.rfqId=:rfqId")
	void updateRfqByRfqId(@Param("rfqId") String rfqId,@Param("quoteStatus") MasterStatus quoteStatus);

	@Query("SELECT r FROM Rfq r WHERE  (r.noPrFlag = true and r.byClient = false)or (r.byClient = true and r.clientStatus =:status )Order By r.createdTS DESC")
	List<Rfq> findAllRfqNoPr(MasterStatus status);
	
	@Query("SELECT r FROM Rfq r " +
		       "WHERE (r.noPrFlag = true AND r.byClient = false) " +
		       "   OR (r.byClient = true AND r.clientStatus IN :statuses) " +
		       "ORDER BY r.createdTS DESC")
		List<Rfq> findAllRfqNoPrInStatuses(@Param("statuses") List<MasterStatus> statuses);

	@Query("SELECT r FROM Rfq r WHERE r.createdBy= :fullName  and r.noPrFlag = true Order By r.createdTS DESC ")
	List<Rfq> getRfqsByNoPrFlagIsTrue(@Param("fullName") String fullName);
	
	@Query("SELECT r.user FROM Rfq r WHERE  r.rfqId=:rfqId")
	List<String> findRFQByRfQId(@Param("rfqId") String rfqId);


	List<Rfq> findByUserAndRfqIdIn(String clientId, List<String> rfqIds);

	  @Query(value = "SELECT r FROM Rfq r WHERE r.user = :clientId ORDER BY r.rfqClosingDate DESC")
	List<Rfq> findLast3ByClientId(@Param("clientId") String clientId, PageRequest of);

	 
//	 @Query("SELECT DISTINCT r FROM Rfq r JOIN r.rfqItem i WHERE i.category IN :category ORDER BY r.createdTS DESC")
//	 List<Rfq> findTopRfqsByCategory(@Param("category") List<String> categoryList, Pageable pageable);
//
//	 
//	 @Query("SELECT COUNT(DISTINCT r) FROM Rfq r JOIN r.rfqItem i WHERE i.category IN :category")
//	 long countByRfqItemCategory(@Param("category") List<String> categoryList);
	  
	  @Query("""
			    SELECT DISTINCT r
			    FROM Rfq r
			    JOIN r.rfqItem i
			    WHERE i.category IN :category
			      AND NOT EXISTS (
			          SELECT 1
			          FROM GmtRfqVendors v
			          WHERE v.rfq = r
			            AND v.vendor.id = :sellerId
			      )
			    ORDER BY r.createdTS DESC
			""")
			List<Rfq> findTopRfqsByCategory(
			        @Param("category") List<String> categoryList,
			        @Param("sellerId") String sellerId,
			        Date fromDate, Pageable pageable
			);

	  
	  @Query("""
			    SELECT COUNT(DISTINCT r)
			    FROM Rfq r
			    JOIN r.rfqItem i
			    WHERE i.category IN :category
			      AND NOT EXISTS (
			          SELECT 1
			          FROM GmtRfqVendors v
			          WHERE v.rfq = r
			            AND v.vendor.id = :sellerId
			      )
			""")
			long countByRfqItemCategory(
			        @Param("category") List<String> categoryList,
			        @Param("sellerId") String sellerId
			);

	Rfq findByRfqId(String rfqId);

	@Query("SELECT r.id FROM Rfq r WHERE r.rfqId=:rfqId")
	List<String> getIdbyRfqId(@Param("rfqId") String rfqId);

	@Modifying
	@Transactional
	@Query("UPDATE  Rfq r SET r.clientStatus = :resultStatus WHERE  r =:rfq")
	void updateRfqStatus(@Param("rfq") Rfq rfq,@Param("resultStatus") MasterStatus resultStatus);
	
	@Query("""
			   SELECT r
			   FROM Rfq r
			   WHERE r.rfqId IN :rfqIds
			   AND NOT EXISTS (
			       SELECT 1
			       FROM GmtRfqVendors v
			       WHERE v.rfq = r
			       AND v.vendor.id = :sellerId
			   )
			""")
			List<Rfq> findRfqsByIdsExcludingSellerRequested(
			        @Param("rfqIds") List<String> rfqIds,
			        @Param("sellerId") String sellerId
			);
	
	@Query("""
			   SELECT r
			   FROM Rfq r
			   WHERE NOT EXISTS (
			       SELECT 1
			       FROM GmtRfqVendors v
			       WHERE v.rfq = r
			       AND v.vendor.id = :sellerId
			   )
			   ORDER BY r.createdTS DESC
			""")
			List<Rfq> findLatest5RfqsExcludingSellerRequested(
			        @Param("sellerId") String sellerId,
			        Pageable pageable
			);
	
	
	@Query("select r.org.id from Rfq r WHERE r.id = :id")
	String findClientById(@Param("id") String id);

	@Query("SELECT COUNT(r) FROM Rfq r WHERE r.user = :user")
	int findRfqCountByUser(@Param("user") String user);

	@Query("SELECT r FROM Rfq r WHERE  (r.noPrFlag = true and r.byClient = false)or (r.byClient = true and r.clientStatus IN :statusList)Order By r.createdTS DESC")
	List<Rfq> findAllRfqNoPrByCM(@Param("statusList") List<MasterStatus> statusList);

	@Modifying
	@Transactional
	@Query("UPDATE  Rfq r SET r.newCommentAvailableVendor = true  WHERE  r.id=:id")
	void updateRfqCommentFlag(@Param("id")  String id);

	@Modifying
	@Transactional
	@Query("UPDATE  Rfq r SET r.newCommentAvailableVendor = false  WHERE  r.id=:id")
	void updateNewCommentAvailableVendor(@Param("id")  String id);




//	@Modifying
//	@Transactional
//	@Query("UPDATE  Rfq r SET r.quoteSubmittedDate = true, r.quoteCount = r.quoteCount + 1 WHERE  r.rfqId=:rfqId")
//	void updateQuoteSubmissionDate(String rfqId);




}
