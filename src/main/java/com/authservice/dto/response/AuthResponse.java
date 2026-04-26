package com.authservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthResponse {
    private String message;
    private String accessToken;
    private String refreshToken;
}