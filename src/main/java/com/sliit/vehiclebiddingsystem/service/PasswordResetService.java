package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sliit.vehiclebiddingsystem.entity.ResetToken;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.ResetTokenRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Service
public class PasswordResetService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ResetTokenRepository resetTokenRepository;

	@Autowired
	private EmailService emailService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RateLimitService rateLimitService;

    @Transactional
    public void requestReset(String email) {
		// Check rate limiting
		if (!rateLimitService.isAllowed(email)) {
			throw new RuntimeException("Too many password reset requests. Please try again later.");
		}
		
		User user = userRepository.findByEmail(email);
		if (user == null) return;
		resetTokenRepository.deleteByUser(user);
		ResetToken token = new ResetToken();
		token.setUser(user);
		token.setToken(UUID.randomUUID().toString());
		token.setExpiry(LocalDateTime.now().plusHours(2));
		resetTokenRepository.save(token);
		emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
	}

    @Transactional
    public boolean resetPassword(String tokenValue, String newPassword) {
		ResetToken token = resetTokenRepository.findByToken(tokenValue).orElse(null);
		if (token == null || token.getExpiry().isBefore(LocalDateTime.now())) {
			return false;
		}
		User user = token.getUser();
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		resetTokenRepository.delete(token);
		return true;
	}

	public boolean isRateLimited(String email) {
		return !rateLimitService.isAllowed(email);
	}

	public long getTimeUntilNextRefill(String email) {
		return rateLimitService.getTimeUntilNextRefill(email);
	}
}




