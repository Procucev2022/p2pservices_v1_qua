package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.BFSUserComments;
import com.portal.procucev.model.User;

public interface BFSUserCommentsDao extends JpaRepository<BFSUserComments, String> {

	@Query("select new BFSUserComments(b.id,b.createdTS,b.comments,b.commentedBy,b.companyName)from BFSUserComments b where b.items=:items Order By b.createdTS DESC")
	List<BFSUserComments> findByItemId(BFSItems items);

	@Query("select new BFSUserComments(b.id,b.createdTS,b.comments,b.commentedBy,b.companyName)from BFSUserComments b where b.items=:items and b.user=:user Order By b.createdTS DESC")
	List<BFSUserComments> findByItemId(BFSItems items, User user);

}
