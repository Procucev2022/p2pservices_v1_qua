package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;

@Repository
public interface RFQItemsDao extends JpaRepository<RfqItem, String> {

	@Query("SELECT r from RfqItem r WHERE r.rfq= :rfq 	Order By r.serialNo ASC")
	List<RfqItem> findByRfq(Rfq rfq);

	@Query("SELECT r.description from RfqItem r WHERE r.id= :string")
	String findByitemId(String string);

}
