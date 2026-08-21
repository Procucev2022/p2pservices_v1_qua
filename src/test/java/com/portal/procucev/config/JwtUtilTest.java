package com.portal.procucev.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;
    private String secret = "mysecretkey123456789012345678901234567890";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        jwtUtil.init();

        userDetails = new User("testuser", "password", Collections.emptyList());
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken(userDetails, "9876543210");
        assertNotNull(token);

        String username = jwtUtil.extractUsername(token);
        assertEquals("testuser", username);

        String phone = jwtUtil.extractPhone(token);
        assertEquals("9876543210", phone);

        Date expiration = jwtUtil.extractExpiration(token);
        assertTrue(expiration.after(new Date()));

        assertTrue(jwtUtil.validateToken(token, userDetails));

        UserDetails wrongUser = new User("wronguser", "password", Collections.emptyList());
        assertFalse(jwtUtil.validateToken(token, wrongUser));
    }

    @Test
    void testExpiredTokenValidation() {
        byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
        Key secretKey = Keys.hmacShaKeyFor(keyBytes);

        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 20000))
                .setExpiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.validateToken(expiredToken, userDetails));
    }

    @Test
    void testMalformedOrNullToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.str", userDetails));
        assertNull(jwtUtil.extractUsername("invalid.token.str"));
    }

    @Test
    void testTokenWithoutExpirationClaimIsNotTreatedAsExpired() {
        byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
        Key secretKey = Keys.hmacShaKeyFor(keyBytes);

        String tokenWithoutExpiry = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        assertNull(jwtUtil.extractExpiration(tokenWithoutExpiry));
        assertTrue(jwtUtil.validateToken(tokenWithoutExpiry, userDetails));
    }

    @Test
    void testUnconfiguredSecretKey() {
        JwtUtil unconfigured = new JwtUtil();
        ReflectionTestUtils.setField(unconfigured, "secret", "");
        unconfigured.init();
        assertThrows(IllegalStateException.class, () -> unconfigured.extractUsername("token"));
    }
}
