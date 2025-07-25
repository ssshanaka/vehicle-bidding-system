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


}