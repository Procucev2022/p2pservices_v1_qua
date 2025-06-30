package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portal.procucev.model.GmtItems;

public interface GmtItemsDao extends JpaRepository<GmtItems, String>{

	List<GmtItems> findByRfqItemIdIn(List<String> collect);

}
