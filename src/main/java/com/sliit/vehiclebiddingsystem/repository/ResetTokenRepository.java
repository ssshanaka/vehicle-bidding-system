package com.sliit.vehiclebiddingsystem.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.sliit.vehiclebiddingsystem.entity.ResetToken;
import com.sliit.vehiclebiddingsystem.entity.User;

public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {
	Optional<ResetToken> findByToken(String token);
	void deleteByUser(User user);
	
	@Modifying
	@Query("DELETE FROM ResetToken rt WHERE rt.expiry < :now")
	void deleteExpiredTokens(LocalDateTime now);
}

