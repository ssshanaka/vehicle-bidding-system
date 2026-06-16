package com.sliit.vehiclebiddingsystem.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.sliit.vehiclebiddingsystem.dto.HomepageAuctionDto;
import com.sliit.vehiclebiddingsystem.dto.HomepageStatsDto;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.HomepageService;
import com.sliit.vehiclebiddingsystem.service.NotificationService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AuthViewController {

	private static final Logger logger = LoggerFactory.getLogger(AuthViewController.class);

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private HomepageService homepageService;
	
	@Autowired
	private NotificationService notificationService;



	@GetMapping("/clear-session")
	public String clearSession(HttpServletResponse response) {
		logger.info("Clearing session - removing JWT cookies");
		
		// Clear the Security Context
		SecurityContextHolder.clearContext();
		
		// Clear JWT cookies with comprehensive approach
		clearJwtCookies(response);
		
		return "redirect:/";
	}

	@GetMapping("/")
	public String homepage(Authentication auth, Model model) {
		// Set page title for the header
		model.addAttribute("pageTitle", "Discover Quality Vehicles");
		
		if (auth != null && auth.isAuthenticated()) {
			User user = userRepository.findByUsername(auth.getName());
			if (user != null) {
				model.addAttribute("user", user);
				
				// Add notification count for authenticated users
				try {
					long unreadCount = notificationService.getUnreadCount(user.getUserId());
					model.addAttribute("unreadCount", unreadCount);
				} catch (Exception e) {
					model.addAttribute("unreadCount", 0);
				}
				
				// Get featured auctions for homepage using DTOs
				List<HomepageAuctionDto> featuredAuctions = homepageService.getFeaturedAuctions();
				model.addAttribute("featuredAuctions", featuredAuctions);
				
				// Add statistics using DTO
				HomepageStatsDto stats = homepageService.getHomepageStats();
				model.addAttribute("totalListings", stats.getTotalListings());
				model.addAttribute("activeAuctions", stats.getActiveAuctions());
				model.addAttribute("totalUsers", stats.getTotalUsers());
				model.addAttribute("completedAuctions", stats.getCompletedAuctions());
			}
		} else {
			// For unauthenticated users, show public content using DTOs
			List<HomepageAuctionDto> featuredAuctions = homepageService.getFeaturedAuctions();
			model.addAttribute("featuredAuctions", featuredAuctions);
			
			// Add statistics using DTO
			HomepageStatsDto stats = homepageService.getHomepageStats();
			model.addAttribute("totalListings", stats.getTotalListings());
			model.addAttribute("activeAuctions", stats.getActiveAuctions());
			model.addAttribute("totalUsers", stats.getTotalUsers());
			model.addAttribute("completedAuctions", stats.getCompletedAuctions());
		}
		
		return "index";
	}

	@GetMapping("/login")
	public String loginPage() {
		return "login";
	}

	@GetMapping("/register")
	public String registerPage() {
		return "register";
	}

	@GetMapping("/logout")
	public String logoutPage(HttpServletResponse response) {
		logger.info("Logout GET requested - clearing authentication");
		
		// Clear the Security Context
		SecurityContextHolder.clearContext();
		
		// Clear JWT cookies with comprehensive approach
		clearJwtCookies(response);
		
		logger.info("Authentication cleared, redirecting to homepage");
		
		// Redirect to index page
		return "redirect:/";
	}
	
	@PostMapping("/logout")
	public String logoutPost(HttpServletResponse response) {
		logger.info("Logout POST requested - clearing authentication");
		
		// Clear the Security Context
		SecurityContextHolder.clearContext();
		
		// Clear JWT cookies with comprehensive approach
		clearJwtCookies(response);
		
		logger.info("Authentication cleared, redirecting to homepage");
		
		// Redirect to index page
		return "redirect:/";
	}
	
	/**
	 * Comprehensive JWT cookie clearing method
	 */
	private void clearJwtCookies(HttpServletResponse response) {
		// Create multiple cookie variations to ensure complete removal
		Cookie[] cookies = {
			createExpiredCookie("JWT", true),   // HttpOnly
			createExpiredCookie("JWT", false),  // Non-HttpOnly
			createExpiredCookie("JWT", true, "/"),   // With path
			createExpiredCookie("JWT", false, "/"),  // With path, non-HttpOnly
		};
		
		for (Cookie cookie : cookies) {
			response.addCookie(cookie);
		}
		
		logger.debug("JWT cookies cleared with {} variations", cookies.length);
	}
	
	/**
	 * Create an expired cookie to clear JWT
	 */
	private Cookie createExpiredCookie(String name, boolean httpOnly) {
		return createExpiredCookie(name, httpOnly, "/");
	}
	
	/**
	 * Create an expired cookie to clear JWT with custom path
	 */
	private Cookie createExpiredCookie(String name, boolean httpOnly, String path) {
		Cookie cookie = new Cookie(name, "");
		cookie.setMaxAge(0);
		cookie.setHttpOnly(httpOnly);
		cookie.setPath(path);
		cookie.setSecure(false); // Set to true in production with HTTPS
		return cookie;
	}

	@GetMapping("/dashboard")
	public String dashboard(Authentication auth) {
		if (auth != null && auth.isAuthenticated()) {
			User user = userRepository.findByUsername(auth.getName());
			if (user != null && user.getRole() != null) {
				// Redirect admin roles to their specific dashboards
				switch (user.getRole()) {
					case ADMIN_OFFICER:
						return "redirect:/admin/dashboard";
					case IT_CONSULTANT:
						return "redirect:/admin/dashboard";
					case SALES_MANAGER:
						return "redirect:/admin/dashboard";
					case CUSTOMER_SERVICE:
						return "redirect:/admin/dashboard";
					case VEHICLE_INSPECTOR:
						return "redirect:/admin/dashboard";
					default:
						// Regular users go to standard dashboard
						return "dashboard";
				}
			}
		}
		return "dashboard";
	}
}
