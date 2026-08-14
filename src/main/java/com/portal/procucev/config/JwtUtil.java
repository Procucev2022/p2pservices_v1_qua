package com.portal.procucev.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);

	@Value("${jwt.secret:}")
	private String secret; // injected from config

	private Key secretKey;

	@PostConstruct
	public void init() {
		if (secret == null || secret.isBlank()) {
			// Deliberately not throwing here. A missing secret used to abort the
			// whole application context, which took every endpoint down with a
			// bare 404 and no way to see why. Token operations now fail loudly
			// instead, and the app stays up to report the misconfiguration.
			LOGGER.error("jwt.secret is not configured (JWT_SECRET is unset or empty). "
					+ "Authentication will fail until it is set.");
			return;
		}
		// Make sure key length is valid (>= 256 bits for HS256)
		byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
		secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	private Key requireSecretKey() {
		if (secretKey == null) {
			throw new IllegalStateException("JWT signing key is not configured. Set the JWT_SECRET "
					+ "environment variable (or the jwt.secret property) to at least 32 characters.");
		}
		return secretKey;
	}

	public String generateToken(UserDetails userDetails, String phone) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("phone", phone);
		return createToken(claims, userDetails.getUsername());
	}

	private String createToken(Map<String, Object> claims, String subject) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours validity
				.signWith(requireSecretKey(), SignatureAlgorithm.HS256).compact();
	}

	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		if (username == null || !username.equals(userDetails.getUsername())) {
			return false;
		}
		return !isTokenExpired(token);
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claims != null ? claimsResolver.apply(claims) : null;
	}

	private Claims extractAllClaims(String token) {
		// Outside the try: a missing key is a configuration fault, not an invalid
		// token, and must not be swallowed into a silent "unauthenticated".
		final Key key = requireSecretKey();
		try {
			return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		} catch (Exception e) {
			return null;
		}
	}

	private Boolean isTokenExpired(String token) {
		Date expiration = extractExpiration(token);
		return expiration != null && expiration.before(new Date());
	}

	public String extractPhone(String token) {
		return extractClaim(token, claims -> claims.get("phone", String.class));
	}

}