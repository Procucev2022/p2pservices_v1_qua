package com.portal.procucev.dao;

import com.portal.procucev.model.ZohoOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZohoOAuthTokenRepository extends JpaRepository<ZohoOAuthToken, Long> {
}
