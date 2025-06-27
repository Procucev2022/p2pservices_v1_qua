/**
 * 
 */
package com.portal.procucev.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Role;

/**
 * @author Chiranjeevi
 *
 */
@Repository
public interface RoleDao extends JpaRepository<Role, String> {

	Role findByRoleNameAndActive(String vendorRegistrationRole, boolean active);

	Role findByRoleNameAndActiveAndOrgtype(String registration, boolean b, OrgType orgTypeObject);

	List<Role> findByOrgtype(OrgType type);

	List<Role> findByActiveAndRoleNameIn(boolean b, List<String> rolename);


}
