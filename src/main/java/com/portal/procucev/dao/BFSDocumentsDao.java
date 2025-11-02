package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portal.procucev.model.BFSDocuments;
import com.portal.procucev.model.BFSItems;

public interface BFSDocumentsDao extends JpaRepository<BFSDocuments, String> {

	List<BFSDocuments> findByBfs(BFSItems item);

}
