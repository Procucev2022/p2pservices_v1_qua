package com.portal.procucev.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.portal.procucev.model.PincodeData;

public interface PincodeDao extends JpaRepository<PincodeData, String> {

	boolean existsByPincode(String pincode);

	@Query
	PincodeData findByPincode(String pincode);

}
