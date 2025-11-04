package com.portal.procucev.dao;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.MasterStatus;

import jakarta.transaction.Transactional;

public interface BFSDao extends JpaRepository<BFSItems, String>{

	
//	
//	@Query("SELECT new BFSItems(b.id, b.createdTS, b.description, b.specification, b.totalQuantity, b.availableQuantity, b.category, b.itemNumber, b.location, b.ageOfAsset, b.sellPrice, b.discount, b.askPrice, b.bfsGroup, b.proxyId,b.unitofMeasures,b.remarks,b.status,b.commentsFlag) " +
//		       "FROM BFSItems b " +
//		       "WHERE b.userId != :user " +
//		       "AND NOT EXISTS (" +
//		       "    SELECT 1 " +
//		       "    FROM BFSItems b2 " +
//		       "    WHERE b2.userId = b.userId " +
//		       "    AND b2.proxyId = :user" +
//		       ") " +
//		       "ORDER BY b.createdTS DESC")
	@Query("SELECT new BFSItems(b.id, b.createdTS, b.description, b.specification, b.totalQuantity, b.availableQuantity, b.category, b.itemNumber, b.location, b.ageOfAsset, b.sellPrice, b.discount, b.askPrice, b.bfsGroup, b.proxyId,b.unitofMeasures,b.remarks,b.status,b.commentsFlag,b.buyPriceDisclosure,b.disclosedBuypriceValue) " +
		       "FROM BFSItems b " +
		       "WHERE b.userId != :user " +
		       "ORDER BY b.createdTS DESC")
	List<BFSItems> findAllItems(String user);

	@Query("select new BFSItems(b.id,b.createdTS,b.description,b.specification,b.totalQuantity,b.availableQuantity,b.category,b.itemNumber,b.location,b.ageOfAsset,b.sellPrice,b.discount,b.askPrice,b.bfsGroup,b.proxyId,b.unitofMeasures,b.remarks,b.status,b.commentsFlag,b.buyPriceDisclosure,b.disclosedBuypriceValue)from BFSItems b where b.org.id =:id and b.userId=:user or b.proxyId=:user Order By b.createdTS DESC")
	List<BFSItems> findByOrgAndUser(String id, String user);

	@Query("select new BFSItems(b.id,b.createdTS,b.description,b.specification,b.totalQuantity,b.availableQuantity,b.category,b.itemNumber,b.location,b.ageOfAsset,b.sellPrice,b.discount,b.askPrice,b.bfsGroup,b.proxyId,b.unitofMeasures,b.remarks,b.status,b.commentsFlag,b.buyPriceDisclosure,b.disclosedBuypriceValue)from BFSItems b where b.id IN (:itemIds) Order By b.createdTS DESC")
	List<BFSItems> findByIdIn(List<String> itemIds);
	
	@Modifying
	@Transactional
	@Query("UPDATE  BFSItems b SET b.availableQuantity =:availableQuantity WHERE  b.id=:id")
	void updateAvailableQuantity(double availableQuantity, String id);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSItems b SET b.commentsFlag = true WHERE  b.id=:id")
	void updateCommentsFlag(String id);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSItems b SET b.commentsFlag = false WHERE  b.id=:id")
	void deactivateComment(String id);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSItems b SET b.status =:status WHERE  b.id=:id")
	void updateStatus(String id, MasterStatus status);

}
