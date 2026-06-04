package com.sliit.vehiclebiddingsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordResetPageController {

	@GetMapping("/forgot-password")
	public String forgotPasswordPage() {
		return "reset-password-request";
	}

	@GetMapping("/reset-password")
	public String resetPasswordPage(@RequestParam(name = "token", required = false) String token, Model model) {
		model.addAttribute("token", token);
		return "reset-password";
	}
}


