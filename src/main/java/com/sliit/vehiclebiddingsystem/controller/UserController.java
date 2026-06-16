package com.sliit.vehiclebiddingsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sliit.vehiclebiddingsystem.dto.UpdateProfileRequest;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.UserService;

@RestController
@RequestMapping("/me")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@GetMapping
	public ResponseEntity<?> me(Authentication auth) {
		User user = userRepository.findByUsername(auth.getName());
		return ResponseEntity.ok(user);
	}

	@PostMapping("/profile")
	public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody UpdateProfileRequest req) {
		User user = userRepository.findByUsername(auth.getName());
		userService.updateProfile(user, req.getEmail(), req.getPhone());
		return ResponseEntity.ok().build();
	}
}
