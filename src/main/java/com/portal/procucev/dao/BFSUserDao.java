package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.BFSUsers;
import com.portal.procucev.model.MasterStatus;

import jakarta.transaction.Transactional;



public interface BFSUserDao extends JpaRepository<BFSUsers, String>{

	@Query("select b.status from  BFSUsers b where b.user.id=:user and b.items.id=:item Order By b.createdTS DESC ")
	List<MasterStatus> findByUserAndItem(String user, String item);

	@Query("select b.id from  BFSUsers b where b.user.id=:userId and b.items.id=:item")
	List<String> findByUserIdAndBfs(String userId, String item);

	@Query("select new BFSUsers(b.id,b.createdTS,b.buyPrice,b.discount,b.quantity,b.askPrice,b.status,b.org.companyName,b.user.username,b.org.address1,b.cmRemarks,b.sellerRemarks,b.buyerRemarks,b.user.id)from BFSUsers b where b.items=:item Order By b.createdTS DESC")
	List<BFSUsers> findByItems(BFSItems item);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSUsers b SET b.status =:status WHERE  b=:bfsUser")
	void updateStatus(BFSUsers bfsUser, MasterStatus status);

	@Query("select b from BFSUsers b where b.user.id=:id and b.status=:status Order By b.createdTS DESC")
	List<BFSUsers> findByStatusAndUser(MasterStatus status,String id);

	List<BFSUsers> findByStatusIn(List<MasterStatus> resultStatus, Sort by);
	
	@Query("select b from BFSUsers b where b.user.id=:id Order By b.createdTS DESC")
	List<BFSUsers> findByUser(String id);

	@Query("select b from BFSUsers b where b.user.id=:id  and b.items.id=:item Order By b.createdTS DESC")
	List<BFSUsers> findByUserAndItems(String id, String item);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSUsers b SET b.uniqueId =:uniqueId WHERE  b.id=:id")
	void updateUniqueId(String uniqueId, String id);

	@Query("select new BFSUsers(b.id,b.createdTS,b.buyPrice,b.discount,b.quantity,b.askPrice,b.status,b.org.companyName,b.user.username,b.org.address1,b.cmRemarks,b.sellerRemarks,b.buyerRemarks,b.user.id)from BFSUsers b where b.items=:item and b.status IN (:resultStatus) Order By b.createdTS DESC")
	List<BFSUsers> findByItemsAndStatus(BFSItems item, List<MasterStatus> resultStatus);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSUsers b SET b.status =:status,b.sellerRemarks=:sellerRemarks WHERE  b=:bfsUser")
	void updateSellerRejectStatus(BFSUsers bfsUser, MasterStatus status, String sellerRemarks);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSUsers b SET b.status =:status,b.cmRemarks=:cmRemarks WHERE  b=:bfsUser")
	void updateCmRemarksAndStatus(BFSUsers bfsUser, MasterStatus status, String cmRemarks);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSUsers b SET b.buyPrice =:buyPrice,b.discount=:discount,b.quantity=:quantity,b.askPrice=:askPrice WHERE  b.id=:id")
	void updatePrices(String id, double buyPrice, int quantity, double askPrice, double discount);

	@Query("select distinct b.items.id from BFSUsers b where b.user.id = :userId ")
	List<String> findItemByUser(String userId);

	@Query("select count(b) from BFSUsers b where b.user.id=:userId and b.status =:status ")
	long findItemStatusByUser(String userId, MasterStatus status);

	@Modifying
	@Transactional
	@Query("UPDATE  BFSUsers b SET b.quantity =:quantity WHERE  b.id=:id")
	void updateQuantity(String id, int quantity);

	@Query("select distinct b.items.id from BFSUsers b")
	List<String> findItems();

	@Query("select count(b) from BFSUsers b where b.status =:status and b.items.id=:item ")
	long getCountByStatusAndItem(MasterStatus status, String item);
	
	@Query("select b from BFSUsers b where b.uniqueId=:uniqueId")
	BFSUsers findByUniqueId(String uniqueId);
	
	// In bfsUserDao, add a method to fetch all statuses for multiple items
	@Query("select b.status from  BFSUsers b where b.user.id=:userId and b.items.id IN :itemIds")
	List<MasterStatus> findAllStatusesByUserAndItems(String userId,List<String> itemIds);

	@Query("select b.items.id from BFSUsers b where b.id=:id")
	String findItemByBfsUser(String id);

}
