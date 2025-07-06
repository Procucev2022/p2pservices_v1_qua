package com.portal.procucev.dao;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
	void updateCount(Rfq rfq);

	@Modifying
	@Transactional
	@Query("UPDATE Rfq c SET c.count = c.count-1 WHERE c =:rfq")
	void updateRfqCount(Rfq rfq);

	@Modifying
	@Transactional
	@Query("UPDATE  Rfq r SET r.quotationReceived = true WHERE  r.rfqId=:rfqId")
	void updateRfqByRfqId(@Param("rfqId") String rfqId);

	@Query("SELECT r FROM Rfq r WHERE  (r.noPrFlag = true and r.byClient = false)or (r.byClient = true and r.clientStatus =:status )Order By r.createdTS DESC")
	List<Rfq> findAllRfqNoPr(MasterStatus status);

	@Query("SELECT r FROM Rfq r WHERE r.createdBy= :fullName  and r.noPrFlag = true Order By r.createdTS DESC ")
	List<Rfq> getRfqsByNoPrFlagIsTrue(@Param("fullName") String fullName);
	
	@Query("SELECT r.user FROM Rfq r WHERE  r.rfqId=:rfqId")
	List<String> findRFQByRfQId(@Param("rfqId") String rfqId);

}
