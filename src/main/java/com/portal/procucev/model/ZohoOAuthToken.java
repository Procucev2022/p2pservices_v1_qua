package com.portal.procucev.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "zoho_oauth_token")
@Data
public class ZohoOAuthToken {

    @Id
    private Long id = 1L;

    @Column(length = 2000)
    private String accessToken;

    @Column(length = 2000)
    private String refreshToken;

    private Instant expiryTime;

    private Instant lastUpdated;
}
