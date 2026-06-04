package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public User register(String username, String email, String phone, String rawPassword) {
		// Validate password
		if (rawPassword == null || rawPassword.trim().isEmpty()) {
			throw new IllegalArgumentException("Password is required");
		}
		if (rawPassword.length() < 6) {
			throw new IllegalArgumentException("Password must be at least 6 characters long");
		}
		
		if (userRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("Username already exists");
		}
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email already exists");
		}
		User u = new User();
		u.setUsername(username);
		u.setEmail(email);
		u.setPhone(phone);
		u.setPasswordHash(passwordEncoder.encode(rawPassword));
		return userRepository.save(u);
	}

	public User findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	public void recordFailedAttempt(User user, int maxAttempts, int lockMinutes) {
		int attempts = user.getFailedAttempts() + 1;
		user.setFailedAttempts(attempts);
		if (attempts >= maxAttempts) {
			user.setLockoutUntil(LocalDateTime.now().plusMinutes(lockMinutes));
			user.setFailedAttempts(0);
		}
		userRepository.save(user);
	}

	public void resetFailedAttempts(User user) {
		user.setFailedAttempts(0);
		user.setLockoutUntil(null);
		user.setLastLoginTime(LocalDateTime.now());
		userRepository.save(user);
	}

	public User updateProfile(User user, String email, String phone) {
		user.setEmail(email);
		user.setPhone(phone);
		return userRepository.save(user);
	}
}




