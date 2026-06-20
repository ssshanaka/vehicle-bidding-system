package com.sliit.vehiclebiddingsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Global Model Attribute Handler to ensure user object is available in all templates
 * This interceptor adds the current user to the model for every request
 */
@Component
public class GlobalModelAttributeHandler implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                          @NonNull Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        
        if (modelAndView != null) {
            // Get current authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() 
                && !authentication.getName().equals("anonymousUser")) {
                
                try {
                    // Get current user
                    User currentUser = userRepository.findByUsername(authentication.getName());
                    
                    if (currentUser != null) {
                        // Add user object to model if not already present
                        if (!modelAndView.getModel().containsKey("user")) {
                            modelAndView.addObject("user", currentUser);
                        }
                        
                        // Also add currentUser for backward compatibility
                        if (!modelAndView.getModel().containsKey("currentUser")) {
                            modelAndView.addObject("currentUser", currentUser);
                        }
                        
                        // Add unread notification count if needed
                        // This can be extended to include notification counts
                        // For now, we'll add a placeholder
                        if (!modelAndView.getModel().containsKey("unreadCount")) {
                            modelAndView.addObject("unreadCount", 0);
                        }
                    }
                } catch (Exception e) {
                    // Log error but don't break the request
                    System.err.println("Error adding user to model: " + e.getMessage());
                }
            }
        }
    }
}
