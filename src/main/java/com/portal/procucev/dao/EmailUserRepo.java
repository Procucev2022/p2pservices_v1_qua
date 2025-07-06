package com.portal.procucev.dao;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.portal.procucev.model.EmailUser;

public interface EmailUserRepo extends JpaRepository<EmailUser, String>{

	EmailUser findByEmail(String email);


	@Modifying
	@Transactional
	@Query("UPDATE EmailUser e SET e.password = :password WHERE e.email = :email")
	void updatePassword(String email, String password);


}
