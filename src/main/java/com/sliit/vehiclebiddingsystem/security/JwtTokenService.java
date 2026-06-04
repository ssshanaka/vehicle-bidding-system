package com.sliit.vehiclebiddingsystem.security;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenService {

	@Value("${app.jwt.secret:change-me-super-secret-key-change-me-super-secret-key}")
	private String secret;

	@Value("${app.jwt.expirationMs:86400000}")
	private long expirationMs;

	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}

	public String generateToken(String username, String role) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + expirationMs);
		return Jwts.builder()
			.setSubject(username)
			.claim("role", role)
			.setIssuedAt(now)
			.setExpiration(exp)
			.signWith(getSigningKey(), SignatureAlgorithm.HS256)
			.compact();
	}

	public Jws<Claims> parse(String token) {
		return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
	}
}




