package com.sliit.vehiclebiddingsystem.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sliit.vehiclebiddingsystem.dto.AuthResponse;
import com.sliit.vehiclebiddingsystem.dto.LoginRequest;
import com.sliit.vehiclebiddingsystem.dto.PasswordResetConfirmRequest;
import com.sliit.vehiclebiddingsystem.dto.PasswordResetInitiateRequest;
import com.sliit.vehiclebiddingsystem.dto.RegisterRequest;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.security.JwtTokenService;
import com.sliit.vehiclebiddingsystem.service.PasswordResetService;
import com.sliit.vehiclebiddingsystem.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	private UserService userService;

	@Autowired
	private AuthenticationManager authManager;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private PasswordResetService passwordResetService;

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest req, HttpServletResponse resp) {
		User u;
		try {
			u = userService.register(req.getUsername(), req.getEmail(), req.getPhone(), req.getPassword());
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
		}
		if (u == null) {
			return ResponseEntity.badRequest().body("Registration failed");
		}
		AuthResponse res = new AuthResponse();
		res.setUsername(u.getUsername());
		res.setRole(u.getRole() != null ? u.getRole().name() : null);
		String token = jwtTokenService.generateToken(u.getUsername(), u.getRole() != null ? u.getRole().name() : "");
		res.setToken(token);

		Cookie cookie = new Cookie("JWT", token);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setSecure(false); // Set to true in production with HTTPS
		cookie.setMaxAge(86400); // 24 hours
		resp.addCookie(cookie);
		return ResponseEntity.ok(res);
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse resp) {
		try {
			User user = userService.findByUsername(req.getUsername());
			if (user == null) {
				return ResponseEntity.status(401).body("Invalid credentials");
			}
			if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(LocalDateTime.now())) {
				return ResponseEntity.status(423).body("Account locked. Try later.");
			}
			
			authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
			userService.resetFailedAttempts(user);
			
			AuthResponse res = new AuthResponse();
			res.setUsername(user.getUsername());
			res.setRole(user.getRole() != null ? user.getRole().name() : "USER");
			String token = jwtTokenService.generateToken(user.getUsername(), user.getRole() != null ? user.getRole().name() : "USER");
			res.setToken(token);
			
			Cookie cookie = new Cookie("JWT", token);
			cookie.setHttpOnly(true);
			cookie.setPath("/");
			cookie.setSecure(false); // Set to true in production with HTTPS
			cookie.setMaxAge(86400); // 24 hours
			// Ensure cookie is available for all requests
			resp.addCookie(cookie);
			
			return ResponseEntity.ok(res);
		} catch (BadCredentialsException e) {
			User user = userService.findByUsername(req.getUsername());
			if (user != null) {
				userService.recordFailedAttempt(user, 5, 15);
			}
			return ResponseEntity.status(401).body("Invalid credentials");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Login failed: " + e.getMessage());
		}
	}

	@PostMapping("/request-password-reset")
	public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetInitiateRequest req) {
		try {
			passwordResetService.requestReset(req.getEmail());
			return ResponseEntity.ok().build();
		} catch (RuntimeException e) {
			if (e.getMessage().contains("Too many password reset requests")) {
				return ResponseEntity.status(429).body("Too many password reset requests. Please try again later.");
			}
			return ResponseEntity.badRequest().body("Failed to process password reset request: " + e.getMessage());
		}
	}

	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody PasswordResetConfirmRequest req) {
		boolean ok = passwordResetService.resetPassword(req.getToken(), req.getNewPassword());
		return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body("Invalid or expired token");
	}
} 




