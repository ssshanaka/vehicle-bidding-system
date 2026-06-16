package com.sliit.vehiclebiddingsystem.dto;

import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
	private String token;
	private String newPassword;
}
