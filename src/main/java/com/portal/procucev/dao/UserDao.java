package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;

@Repository("userDao")
public interface UserDao extends JpaRepository<User, String> {
	
	User findByUsername(String username);

	User findByUsernameAndActive(String username, boolean active);

	@Query("select u.phone from User u where u.id=:id")
	String findPhoneByUser(String id);

	@Query("select u.org.companyName from User u where u.id=:id")
	String findByUser(String id);

	List<User> findByOrg(Organization organization);

	@Query("select u.fullName from User u where u.username= :username and u.active = true")
	String getFullName(String username);
}
