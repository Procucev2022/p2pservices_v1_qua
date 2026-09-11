package com.portal.procucev.dao;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.portal.procucev.Dto.AuthUserView;
import com.portal.procucev.Dto.BuyerSellerReportDto;
import com.portal.procucev.Dto.SellerSubscriptionReportDto;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;

import jakarta.transaction.Transactional;

@Repository("userDao")
public interface UserDao extends JpaRepository<User, String> {

	User findByUsername(String username);
	
	@Query("SELECT u FROM User u WHERE u.username = :username ORDER BY u.createdTS DESC LIMIT 1")
	User findByLatestUserName(@Param("username") String username);

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
	void updateUserDetailsAndStatus(@Param("id") String id, @Param("username") String username,
			@Param("fullName") String fullName, @Param("phone") String phone, @Param("status") MasterStatus status);

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.username = :username,u.fullName =:fullName,u.phone =:phone  WHERE u.id=:id")
	void updateUserDetails(@Param("id") String id, @Param("username") String username,
			@Param("fullName") String fullName, @Param("phone") String phone);

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.clientStatus = :status, u.isApproved = true  WHERE u=:user")
	void updateClientStatus(@Param("user") User user, @Param("status") MasterStatus status);

	List<User> findByUsernameAndPhone(String email, String organizationPhonenumber);

	User findByUsernameAndPhoneAndActive(String username, String phoneNumber, boolean b);

	/**
	 * Authentication-path lookup. Returns only the columns needed to build the Spring Security
	 * principal, so it costs one SELECT instead of the three that loading the {@code User}
	 * entity triggers via its eager {@code role} and {@code clientStatus} associations.
	 */
	@Query("select u.username as username, u.password as password, u.phone as phone "
			+ "from User u where u.username=:username and u.phone=:phone and u.active = true")
	AuthUserView findAuthViewByUsernameAndPhone(@Param("username") String username, @Param("phone") String phone);

	@Query("SELECT  new User(u.id,u.username,u.phone,u.org.companyName,u.fullName,u.org.id,u.uniqueId,u.activityTs,u.isWebApp,u.isWhatsApp,u.isBot,u.org.city,u.isApproved,u.selfClient,u.active,u.sourceType,u.verificationStatus) from User u where u.phone=:phone and u.active = true")
	List<User> findByPhone(@Param("phone") String phone);

	@Query("SELECT  new User(u.id,u.username,u.phone,u.org.companyName,u.fullName,u.org.id,u.uniqueId,u.activityTs,u.isWebApp,u.isWhatsApp,u.isBot,u.org.zipCode,u.clientStatus,u.createdTS,u.sourceType) from User u where u.selfClient = true and u.active = true and u.role=:role ORDER BY u.createdTS DESC")
	List<User> getUsersBySelfClientAndRole(@Param("role") Role role);

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.activityTs = CURRENT_TIMESTAMP WHERE u.username=:email and u.phone=:phone and u.active = true")
	void updateActivityTs(@Param("email") String email, @Param("phone") String phone);

	@Query("select u.activityTs from User u where u.id=:id")
	List<Date> findActivityTsByOrg(@Param("id") String id);

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.activityTs = CURRENT_TIMESTAMP,u.verificationStatus = :emailVerified WHERE u.username=:email and u.phone=:phone and u.active = true")
	int updateActivityTs(@Param("email") String email, @Param("phone") String phone,
			@Param("emailVerified") String emailVerified);

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.activityTs = CURRENT_TIMESTAMP,u.verificationStatus = :emailVerificationFailed WHERE u.username=:email and u.phone=:phone and u.active = true")
	void updateVerificationStatus(@Param("email") String email, @Param("phone") String phone,
			@Param("emailVerificationFailed") String emailVerificationFailed);

	@Query("SELECT  new User(u.id,u.username,u.phone,u.org.companyName,u.fullName,u.org.id,u.uniqueId,u.activityTs,u.isWebApp,u.isWhatsApp,u.isBot,u.org.city,u.isApproved,u.selfClient,u.active,u.sourceType,u.verificationStatus) from User u where u.phone IN (:variants) and u.active = true")
	List<User> findByPhoneIn(@Param("variants") List<String> variants);

	List<User> findByUsernameAndPhoneInAndActive(String username, List<String> variants, boolean b);

	@Query("SELECT  new User(u.id,u.createdTS,u.username,u.phone,u.fullName) from User u where u.org.id=:id and u.active = true")
	List<User> findByOrganization(@Param("id") String id);

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.bfsGroup = :bfsGroup WHERE u.id = :userId")
	void updateBfsGroupForUsers(@Param("bfsGroup") String bfsGroup, @Param("userId") String userId);

	@Query("SELECT  new User(u.username,u.phone,u.org.companyName,u.fullName) from User u where u.id=:id and u.active = true")
	User findOrgByID(@Param("id") String id);

	@Query("select u.username from User u where u.id=:clientInitiator")
	String findByUserID(@Param("clientInitiator") String clientInitiator);

	@Query("SELECT  new User(u.username,u.phone,u.org.companyName,u.fullName) from User u where u.id=:id")
	User findUserById(@Param("id") String id);

	@Query("select u from User u where u.username=:email and u.phone=:normalizedPhone and u.active = true and u.role =:role")
	User findByUsernameAndPhoneAndActiveAndRole(@Param("email") String email,
			@Param("normalizedPhone") String normalizedPhone, @Param("role") Role role);

	@Query("select u from User u where u.username=:email and u.active = true and u.role =:role")
	User findByUsernameAndActiveAndRole(@Param("email") String email, @Param("role") Role role);

	@Query("select u from User u where u.username=:email and u.active = true and u.role.roleName in :roleNames order by u.createdTS desc")
	List<User> findActiveUsersByUsernameAndRoleNames(@Param("email") String email, @Param("roleNames") List<String> roleNames);

	@Query("SELECT  new User(u.username,u.phone,u.org.companyName,u.fullName) from User u where u.org.id=:id and u.active = true")
	User findUserByOrgId(@Param("id") String id);

	@Query("""
			    SELECT u.org.id, MAX(u.activityTs)
			    FROM User u
			    WHERE u.org.id IN :orgIds
			    GROUP BY u.org.id
			""")
	List<Object[]> findLastLoginByOrgIds(@Param("orgIds") List<String> orgIds);

	@Query("""
					    	    SELECT
					    	        o.id,
					    	        o.companyName,
					    	        o.email,
					    	        o.organizationPhonenumber,
					    	        u.fullName,
					    	        u.activityTs,
					    	        c.category
					    	    FROM Organization o
					    	    LEFT JOIN User u ON u.org = o
					    	    LEFT JOIN OrgDivisionCategory c ON c.organization = o
					    	    WHERE o.createdTS BETWEEN :fromDate AND :toDate
			""")
	List<Object[]> getSellerCategoryRawData(Date fromDate, Date toDate);

//	List<?> getSellerReport(String startDate, String endDate);
//
//	List<?> getSellerSummary(String startDate, String endDate);
//
//	List<?> getSellerCategoryReport(String startDate, String endDate);
	
	@Query("""
		    SELECT
		        o.id,
		        u.fullName,
		        u.username,
		        u.phone,
		        o.companyName,
		        o.city,
		        u.activityTs,
		        oc.category
		    FROM User u
		    JOIN u.org o
		    LEFT JOIN OrgDivisionCategory oc ON oc.organization = o
		    WHERE u.selfClient = true
		    AND u.activityTs BETWEEN :startDate AND :endDate
		    ORDER BY o.id, oc.category
		""")
		List<Object[]> getBuyerCategoryRawData(
		        @Param("startDate") Date startDate,
		        @Param("endDate") Date endDate
		);
		
		@Query(value = """
			    SELECT 
			        u.activity_ts,
			       CASE WHEN ot.type_name = 'CLIENT' THEN 'Buyer' 
             WHEN ot.type_name = 'VENDOR' THEN 'Seller'
             WHEN ot.type_name = 'PROCUCEV' THEN 'Procucev'
             ELSE 'Unknown' END,
			        u.full_name,
			        u.phone,
			        o.organization_name,
			        u.username,
			        o.city,
			        CASE WHEN u.is_web_app = 1 THEN 'GMT'
			             WHEN u.is_whats_app = 1 THEN 'BFS'
			             ELSE 'GMT' END,
			        r.rfq_id,
			        ri.category
			    FROM user u
			    JOIN organization o ON o.uuid = u.org_uuid
			    JOIN org_types ot ON ot.uuid = o.org_type_uuid
			    LEFT JOIN rfq_header r ON r.org_uuid = o.uuid
			    LEFT JOIN rfq_items ri ON ri.rfq_uuid = r.uuid
			    WHERE u.activity_ts >= :startDate
			    AND u.activity_ts < :endDate
			    ORDER BY u.activity_ts DESC
			""", nativeQuery = true)
			List<Object[]> getDailyBuyerSellerReport(
			        @Param("startDate") Date startDate,
			        @Param("endDate") Date endDate
			);
		
			@Query(value = """
				    SELECT
				        u.activity_ts,
				        u.full_name,
				        u.phone,
				        o.organization_name,
				        u.username,
				        o.city,
				        CASE WHEN o.subscription_plan_uuid IS NOT NULL 
				             THEN 'Yes' ELSE 'No' END,
				        o.rfq_used_count,
				        r.rfq_id
				    FROM user u
				    JOIN organization o ON o.uuid = u.org_uuid
				    JOIN org_types ot ON ot.uuid = o.org_type_uuid
				    LEFT JOIN rfq_header r ON r.org_uuid = o.uuid
				    WHERE ot.type_name = 'VENDOR'
				    AND u.activity_ts >= :startDate
				    AND u.activity_ts < :endDate
				    ORDER BY u.activity_ts DESC
				""", nativeQuery = true)
				List<Object[]> getDailySellerSubscriptionReport(
				        @Param("startDate") Date startDate,
				        @Param("endDate") Date endDate
				);
				
				// org is fetched eagerly because callers read companyName/org id straight after.
				// Left lazy, it produced one organization SELECT per distinct user in the batch.
				@Query("""
						SELECT u
						FROM User u
						LEFT JOIN FETCH u.org
						WHERE u.id IN :userIds
						""")
						List<User> findUsersByIds(@Param("userIds") List<String> userIds);
				
				// Search users by phone (contactNumber)
				@Query("""
				    SELECT u FROM User u
				    WHERE LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchValue, '%'))
				    """)
				List<User> findUsersByPhone(@Param("searchValue") String searchValue);
				
				// Search users by their org's company name
				@Query("""
				    SELECT u FROM User u
				    WHERE LOWER(u.org.companyName) LIKE LOWER(CONCAT('%', :searchValue, '%'))
				    """)
				List<User> findUsersByOrgCompanyName(@Param("searchValue") String searchValue);

				//@Query("select u From User u where u.userName=:email and  u.role=:initiatorRole and u.active = true and u.role=:role ")
				Optional<User> findFirstByUsernameAndRoleAndActiveTrueOrderByCreatedTSDesc(
				        String email,
				        Role role);

				List<User> findBySourceTypeAndVerificationStatus(String sourceType, String verificationStatus);
}

