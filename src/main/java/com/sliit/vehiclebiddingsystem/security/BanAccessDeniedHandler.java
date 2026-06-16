package com.sliit.vehiclebiddingsystem.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BanAccessDeniedHandler implements AccessDeniedHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      org.springframework.security.access.AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            
            if (user != null && user.isBanned()) {
                // Redirect to appropriate ban page
                if ("temp".equals(user.getBanType())) {
                    response.sendRedirect("/banned/temporary");
                    return;
                } else if ("perm".equals(user.getBanType())) {
                    response.sendRedirect("/banned/permanent");
                    return;
                }
            }
        }
        
        // Default access denied response
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
    }
}
