package com.portal.procucev.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import com.portal.procucev.model.MasterStatus;

public interface MasterStatusDao  extends JpaRepository<MasterStatus, String> {
	MasterStatus findByStatus(String status);
}
