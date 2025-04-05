package com.biletal.biletalbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String role;
    private String message;
    private UserResponseDto user;
}