package com.portal.procucev.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.User;

@Repository("userDao")
public interface UserDao extends JpaRepository<User, String> {
	
	User findByUsername(String username);

	User findByUsernameAndActive(String username, boolean active);
}
