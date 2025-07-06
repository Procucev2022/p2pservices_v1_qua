package com.portal.procucev.dao;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;

@Repository("userDao")
public interface UserDao extends JpaRepository<User, String> {
	
	User findByUsername(String username);

	User findByUsernameAndActive(String username, boolean active);

	@Query("select u.phone from User u where u.id=:id")
	String findPhoneByUser(@Param("id") String id);

	@Query("select u.org.companyName from User u where u.id=:id")
	String findByUser(@Param("id") String id);

	List<User> findByOrg(Organization organization);

	@Query("select u.fullName from User u where u.username= :username and u.active = true")
	String getFullName(@Param("username") String username);
	
	List<User> findByOrgIn(List<Organization> orglist);
	
	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.active = false  WHERE u.id=:id")
	void deactiveUser(@Param("id") String id);
	
	@Query("select u.org.id from User u where u.id=:id")
	String findOrgIdByUser(@Param("id") String id);
	
	@Query("SELECT  new User(u.id,u.createdTS,u.username,u.clientStatus,u.phone,u.selfClient,u.fullName,u.active) from User u where u.org.id=:id and u.active = true")
	List<User> findByOrgAndActive(@Param("id") String id);
	
	@Query("select u.username from User u where u.org.id=:vendorId And u.active = true")
	List<String> findByOrg(@Param("vendorId") String vendorId);
	
	@Query("select u.username from User u where u.id=:id")
	String findEmailById(@Param("id") String id);

}
