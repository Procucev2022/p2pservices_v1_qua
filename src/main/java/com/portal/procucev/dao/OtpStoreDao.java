package com.portal.procucev.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.OtpStore;

@Repository
public interface OtpStoreDao extends JpaRepository<OtpStore, String> {

	Optional<OtpStore> findByOtpKey(String otpKey);

	void deleteByOtpKey(String otpKey);
}
