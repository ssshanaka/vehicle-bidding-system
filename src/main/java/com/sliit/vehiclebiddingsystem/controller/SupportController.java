package com.sliit.vehiclebiddingsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sliit.vehiclebiddingsystem.dto.UserProfileDTO;
import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.CustomerQueryService;
import com.sliit.vehiclebiddingsystem.service.TicketConversationService;

@Controller
@RequestMapping("/support")
public class SupportController {

    @Autowired
    private CustomerQueryService customerQueryService;

    @Autowired
    private TicketConversationService ticketConversationService;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String supportTicket(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Support Ticket - Lanka Auto Traders");
        
        // Check if user is authenticated
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        if (isAuthenticated) {
            // Get current user for pre-filling form
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);
            
            if (currentUser != null) {
                // Convert User entity to DTO to avoid serialization issues
                UserProfileDTO userProfile = new UserProfileDTO(
                    currentUser.getUsername(),
                    currentUser.getEmail(),
                    currentUser.getPhone(),
                    currentUser.getRole(),
                    currentUser.getProfilePicture()
                );
                model.addAttribute("currentUser", userProfile);
            }
        }
        
        return "support";
    }

    @PostMapping
    public String submitSupportTicket(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String category,
            @RequestParam String priority,
            @RequestParam String message,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        
        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "You must be logged in to submit a support ticket. Please log in and try again.");
            return "redirect:/support";
        }
        
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);
            
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "User not found. Please log in again.");
                return "redirect:/support";
            }
            
            // Map form categories to CustomerQuery.QueryType
            CustomerQuery.QueryType queryType = mapCategoryToQueryType(category);
            
            // Map form priority to CustomerQuery.Priority
            CustomerQuery.Priority queryPriority = mapPriorityToQueryPriority(priority);
            
            // Create CustomerQuery entity
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setSubject(subject);
            customerQuery.setDescription(message);
            customerQuery.setQueryType(queryType);
            customerQuery.setPriority(queryPriority);
            customerQuery.setUser(currentUser);
            
            // Save to database
            CustomerQuery savedQuery = customerQueryService.createQuery(customerQuery);
            
            // Create initial conversation message
            ticketConversationService.addUserMessage(savedQuery, currentUser, message);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Your support ticket #" + savedQuery.getQueryId() + " has been submitted successfully! We'll get back to you within 2 hours during business hours.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "An error occurred while submitting your ticket. Please try again or contact support directly.");
        }
        
        return "redirect:/support?success=true";
    }
    
    private CustomerQuery.QueryType mapCategoryToQueryType(String category) {
        switch (category.toLowerCase()) {
            case "account":
                return CustomerQuery.QueryType.GENERAL_INQUIRY;
            case "bidding":
                return CustomerQuery.QueryType.TECHNICAL_SUPPORT;
            case "payment":
                return CustomerQuery.QueryType.BILLING_ISSUE;
            case "listing":
                return CustomerQuery.QueryType.GENERAL_INQUIRY;
            case "technical":
                return CustomerQuery.QueryType.TECHNICAL_SUPPORT;
            case "general":
                return CustomerQuery.QueryType.GENERAL_INQUIRY;
            case "complaint":
                return CustomerQuery.QueryType.COMPLAINT;
            case "other":
                return CustomerQuery.QueryType.GENERAL_INQUIRY;
            default:
                return CustomerQuery.QueryType.GENERAL_INQUIRY;
        }
    }
    
    private CustomerQuery.Priority mapPriorityToQueryPriority(String priority) {
        switch (priority.toLowerCase()) {
            case "low":
                return CustomerQuery.Priority.LOW;
            case "medium":
                return CustomerQuery.Priority.MEDIUM;
            case "high":
                return CustomerQuery.Priority.HIGH;
            case "critical":
                return CustomerQuery.Priority.URGENT;
            default:
                return CustomerQuery.Priority.MEDIUM;
        }
    }
}
