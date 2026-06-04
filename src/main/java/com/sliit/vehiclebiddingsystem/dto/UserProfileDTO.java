package com.sliit.vehiclebiddingsystem.dto;

import com.sliit.vehiclebiddingsystem.entity.User;

public class UserProfileDTO {
    private String username;
    private String email;
    private String phone;
    private User.Role role;
    private String profilePicture;

    // Constructors
    public UserProfileDTO() {}

    public UserProfileDTO(String username, String email, String phone, User.Role role, String profilePicture) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.profilePicture = profilePicture;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    // Helper method to get display name (using username as fallback)
    public String getDisplayName() {
        return username != null ? username : "User";
    }
}
