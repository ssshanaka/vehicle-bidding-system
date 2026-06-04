package com.sliit.vehiclebiddingsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String username;
    private String role;
    private String token;
}
