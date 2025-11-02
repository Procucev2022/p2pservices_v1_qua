package com.portal.procucev.dao;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portal.procucev.model.BFSImages;
import com.portal.procucev.model.BFSItems;

public interface BFSImagesDao extends JpaRepository<BFSImages, String>{

	@Query("select b from BFSImages b where b.bfs.id=:id")
	List<BFSImages> findByBfs(String id);

	@Query("select count(b) from BFSImages b where b.bfs=:item")
	long findCountByBfs(BFSItems item);

	// In bfsImagesDao, add a method to count images for multiple items
	@Query("SELECT i.bfs.id, COUNT(i) FROM BFSImages i WHERE i.bfs.id IN :itemIds")
	Map<String, Long> findCountByBfsItems(@Param("itemIds") List<String> itemIds);


}
