package com.sliit.vehiclebiddingsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    @Query("SELECT COUNT(u) FROM User u WHERE u.isBanned = true")
    long countByIsBannedTrue();
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);
    
    // IT Consultant specific queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.lockoutUntil > CURRENT_TIMESTAMP")
    long countLockedAccounts();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.failedAttempts > 0")
    long countUsersWithFailedAttempts();
    
    @Query("SELECT u FROM User u WHERE u.lockoutUntil > CURRENT_TIMESTAMP")
    List<User> findLockedUsers();
    
    @Query("SELECT u FROM User u WHERE u.failedAttempts > 0")
    List<User> findUsersWithFailedAttempts();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") User.Role role);
    
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") User.Role role);
    
    // Find users who have logged in recently (within specified time)
    @Query("SELECT u FROM User u WHERE u.lastLoginTime >= :since AND u.isBanned = false AND (u.lockoutUntil IS NULL OR u.lockoutUntil < CURRENT_TIMESTAMP)")
    List<User> findRecentlyActiveUsers(@Param("since") LocalDateTime since);
}