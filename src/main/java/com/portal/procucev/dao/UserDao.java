package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;

import jakarta.transaction.Transactional;

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
	
	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.username = :username, u.fullName = :fullName, u.phone = :phone, u.clientStatus = :status WHERE u.id = :id")
	void updateUserDetailsAndStatus(
	    @Param("id") String id,
	    @Param("username") String username,
	    @Param("fullName") String fullName,
	    @Param("phone") String phone,
	    @Param("status") MasterStatus status
	);
	
	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.username = :username,u.fullName =:fullName,u.phone =:phone  WHERE u.id=:id")
	void updateUserDetails(@Param("id") String id,
		    @Param("username") String username,
		    @Param("fullName") String fullName,
		    @Param("phone") String phone);

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.clientStatus = :status  WHERE u=:user")
	void updateClientStatus(@Param("user") User user, @Param("status") MasterStatus status);

	List<User> findByUsernameAndPhone(String email, String organizationPhonenumber);

	User findByUsernameAndPhoneAndActive(String username, String phoneNumber, boolean b);


	@Query("SELECT  new User(u.id,u.username,u.phone,u.fullName,u.orgId,u.orgId,u.uniqueId,u.activityTs) from User u where u.phone=:phone and u.active = true")
	List<User> findByPhoneNumber(@Param("phone") String phone);

}
