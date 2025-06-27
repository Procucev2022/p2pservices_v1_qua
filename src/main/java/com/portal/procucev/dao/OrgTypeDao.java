package com.portal.procucev.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import com.portal.procucev.model.OrgType;

public interface OrgTypeDao  extends JpaRepository<OrgType, String> {

	OrgType findByTypeName(String vendor);
}
