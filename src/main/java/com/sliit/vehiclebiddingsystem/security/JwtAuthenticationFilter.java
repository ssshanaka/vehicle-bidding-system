package com.sliit.vehiclebiddingsystem.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sliit.vehiclebiddingsystem.service.JwtTokenBlacklistService;
import com.sliit.vehiclebiddingsystem.service.UserSessionService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	@Autowired
	private JwtTokenService jwtTokenService;
	
	@Autowired
	private JwtTokenBlacklistService jwtTokenBlacklistService;
	
	@Autowired
	private UserSessionService userSessionService;
	
	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		String token = null;
		
		// Check for token in Authorization header
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		}
		
		
		// Check for token in cookies
		if (token == null) {
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (Cookie c : cookies) {
					if ("JWT".equals(c.getName())) {
						token = c.getValue();
						break;
					}
				}
			}
		}
		
		// Process token if found and not empty
		if (token != null && !token.trim().isEmpty()) {
			// Check if token is blacklisted
			if (jwtTokenBlacklistService.isTokenBlacklisted(token)) {
				logger.warn("JWT token is blacklisted");
				response.addCookie(createExpiredCookie());
				SecurityContextHolder.clearContext();
				filterChain.doFilter(request, response);
				return;
			}
			
			try {
				Jws<Claims> jws = jwtTokenService.parse(token);
				String username = jws.getBody().getSubject();
				String role = jws.getBody().get("role", String.class);
				
				// Validate required fields
				if (username == null || username.trim().isEmpty()) {
					logger.warn("JWT token missing username");
					response.addCookie(createExpiredCookie());
					SecurityContextHolder.clearContext();
					filterChain.doFilter(request, response);
					return;
				}
				
				// Default role if not specified
				if (role == null || role.trim().isEmpty()) {
					role = "USER";
				}
				
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					username,
					null,
					List.of(new SimpleGrantedAuthority("ROLE_" + role))
				);
				SecurityContextHolder.getContext().setAuthentication(authentication);
				
				// Register the session
				userSessionService.registerSession(username, token);
				
				logger.debug("User {} authenticated via JWT with role: ROLE_{}", username, role);
			} catch (ExpiredJwtException e) {
                logger.warn("JWT token is expired: {}", e.getMessage());
                response.addCookie(createExpiredCookie());
                SecurityContextHolder.clearContext();
            } catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
                logger.warn("Invalid JWT token: {}", e.getMessage());
                response.addCookie(createExpiredCookie());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                logger.error("Error processing JWT: {}", e.getMessage(), e);
                response.addCookie(createExpiredCookie());
                SecurityContextHolder.clearContext();
            }
		} else {
			// No token found, clear context
			SecurityContextHolder.clearContext();
		}
		
		filterChain.doFilter(request, response);
	}

    private Cookie createExpiredCookie() {
        Cookie cookie = new Cookie("JWT", null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}