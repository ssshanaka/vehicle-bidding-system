package com.sliit.vehiclebiddingsystem.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOAD_DIR = "src/main/resources/static/images/profile/";

    @GetMapping("/profile")
    public String profilePage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("pageTitle", "My Profile");
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestParam(value = "currentPassword", required = false) String currentPassword,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // Validate username uniqueness (if changed)
            if (!currentUser.getUsername().equals(username)) {
                User existingUser = userRepository.findByUsername(username);
                if (existingUser != null && !existingUser.getUserId().equals(currentUser.getUserId())) {
                    redirectAttributes.addFlashAttribute("error", "Username already exists. Please choose a different username.");
                    return "redirect:/profile";
                }
            }

            // Validate email uniqueness (if changed)
            if (!currentUser.getEmail().equals(email)) {
                User existingUser = userRepository.findByEmail(email);
                if (existingUser != null && !existingUser.getUserId().equals(currentUser.getUserId())) {
                    redirectAttributes.addFlashAttribute("error", "Email already exists. Please choose a different email.");
                    return "redirect:/profile";
                }
            }

            // Update basic information
            currentUser.setUsername(username);
            currentUser.setEmail(email);
            currentUser.setPhone(phone);

            // Handle profile picture upload
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String profilePictureUrl = saveProfilePicture(profilePicture, currentUser.getUserId());
                if (profilePictureUrl != null) {
                    // Extract filename from URL for storage
                    String filename = profilePictureUrl.substring(profilePictureUrl.lastIndexOf("/") + 1);
                    currentUser.setProfilePicture(filename);
                }
            }

            // Handle password change
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Current password is required to change password.");
                    return "redirect:/profile";
                }

                if (!passwordEncoder.matches(currentPassword, currentUser.getPasswordHash())) {
                    redirectAttributes.addFlashAttribute("error", "Current password is incorrect.");
                    return "redirect:/profile";
                }

                if (!newPassword.equals(confirmPassword)) {
                    redirectAttributes.addFlashAttribute("error", "New password and confirm password do not match.");
                    return "redirect:/profile";
                }

                if (newPassword.length() < 6) {
                    redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long.");
                    return "redirect:/profile";
                }

                currentUser.setPasswordHash(passwordEncoder.encode(newPassword));
            }

            // Save updated user
            userRepository.save(currentUser);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while updating your profile: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    private String saveProfilePicture(MultipartFile file, Long userId) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String filename = "profile_" + userId + "_" + UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/images/profile/" + filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            return userRepository.findByUsername(authentication.getName());
        }
        return null;
    }
}
