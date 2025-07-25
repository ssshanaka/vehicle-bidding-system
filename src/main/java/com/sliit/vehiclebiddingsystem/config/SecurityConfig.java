package com.sliit.vehiclebiddingsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sliit.vehiclebiddingsystem.security.BanAccessDeniedHandler;
import com.sliit.vehiclebiddingsystem.security.JwtAuthenticationFilter;
import com.sliit.vehiclebiddingsystem.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Autowired
	private CustomUserDetailsService userDetailsService;
	
	@Autowired
	private BanAccessDeniedHandler banAccessDeniedHandler;

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/")
				.invalidateHttpSession(true)
				.deleteCookies("JWT")
				.clearAuthentication(true)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/",
					"/css/**",
					"/js/**",
					"/images/**",
					"/favicon.ico",
					"/login",
					"/register",
					"/logout",
					"/clear-session",
					"/forgot-password",
					"/reset-password",
					"/listings",
					"/listings/**",
					"/auctions",
					"/auctions/details/**",
					"/auctions/list",
					"/auth/register",
					"/auth/login",
					"/auth/request-password-reset",
					"/auth/reset-password",
					"/ws/**",
					"/app/**",
					"/topic/**",
					"/error",  // Added to permit error endpoint
					"/banned/**",  // Allow access to ban pages
					"/appeal",  // Allow appeal submission
					"/help",  // Allow public access to help center
					"/contact",  // Allow public access to contact page
					"/privacy",  // Allow public access to privacy policy
					"/terms"  // Allow public access to terms of service
				).permitAll()
				.requestMatchers(HttpMethod.POST, "/logout").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/auctions/**").permitAll()
				// Bid placement requires authentication
				.requestMatchers("/auctions/place-bid/**").authenticated()
				// Specific admin endpoints with broader access (must come before general /admin/** rule)
				.requestMatchers("/admin/dashboard").hasAnyRole("ADMIN_OFFICER", "IT_CONSULTANT", "SALES_MANAGER", "CUSTOMER_SERVICE", "VEHICLE_INSPECTOR")
				.requestMatchers("/admin/auctions/**").hasAnyRole("ADMIN_OFFICER", "IT_CONSULTANT", "SALES_MANAGER")
				.requestMatchers("/admin/reports/**", "/admin/notifications/**").hasAnyRole("ADMIN_OFFICER", "IT_CONSULTANT", "CUSTOMER_SERVICE")
				.requestMatchers("/admin/listings/**").hasAnyRole("ADMIN_OFFICER", "IT_CONSULTANT", "VEHICLE_INSPECTOR")
				.requestMatchers("/admin/users/**").hasAnyRole("ADMIN_OFFICER", "IT_CONSULTANT")
				// Vehicle Inspector API endpoints
				.requestMatchers("/admin/api/vehicle-inspector/**").hasAnyRole("VEHICLE_INSPECTOR")
				// Sales Manager API endpoints
				.requestMatchers("/api/sales-manager/**").hasAnyRole("SALES_MANAGER", "ADMIN_OFFICER", "IT_CONSULTANT")
				// General admin access (most restrictive - must come last)
				.requestMatchers("/admin/**").hasAnyRole("ADMIN_OFFICER", "IT_CONSULTANT")
				.anyRequest().authenticated()
			)
			.exceptionHandling(eh -> eh
				.authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
				.accessDeniedHandler(banAccessDeniedHandler)
			)
			.authenticationProvider(authenticationProvider());
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}