package com.portal.procucev.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.UserActivity;

@Repository
public interface UserActivityDao  extends JpaRepository<UserActivity,Integer>{
	
	

}
