package com.sliit.vehiclebiddingsystem.entity;

import java.time.LocalDateTime;

import com.sliit.vehiclebiddingsystem.converter.RoleConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 10)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Convert(converter = RoleConverter.class)
    private Role role = Role.USER;

    @Column(name = "failed_attempts")
    private int failedAttempts = 0;

    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    @Column(name = "is_banned")
    private boolean isBanned = false;

    @Column(name = "ban_type")
    private String banType;

    @Column(name = "ban_duration")
    private Integer banDuration;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "profile_picture")
    private String profilePicture;

    public enum Role {
        USER, SALES_MANAGER, CUSTOMER_SERVICE, VEHICLE_INSPECTOR, ADMIN_OFFICER, IT_CONSULTANT
    }
}