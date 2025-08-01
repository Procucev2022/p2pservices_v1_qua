package com.portal.procucev.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portal.procucev.model.PincodeData;

public interface PincodeDao extends JpaRepository<PincodeData, String> {

	boolean existsByPincode(String pincode);
	
	PincodeData findByPincode(String pincode);

}
