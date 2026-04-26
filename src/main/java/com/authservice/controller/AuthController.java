package com.authservice.controller;

import com.authservice.dto.request.LoginRequest;
import com.authservice.dto.request.RegisterRequest;
import com.authservice.dto.response.AuthResponse;
import com.authservice.entity.RefreshToken;
import com.authservice.entity.User;
import com.authservice.security.JwtService;
import com.authservice.security.TokenBlacklistService;
import com.authservice.service.AuthService;
import com.authservice.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("REQUEST RECIEVED" +  request.toString());
        return authService.register(request);
    }
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "Admin access granted";
    }


    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody Map<String, String> request) {

        String refreshTokenStr = request.get("refreshToken");

        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenStr);

        User user = refreshToken.getUser();

        String role = user.getRoles().stream()
                .findFirst()
                .get()
                .getName();

        String newAccessToken = jwtService.generateToken(user.getEmail(), role);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request,
                         @RequestBody Map<String, String> body) {

        // 1. delete refresh token
        String refreshToken = body.get("refreshToken");
        refreshTokenService.deleteByToken(refreshToken);

        // 2. extract access token from header
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(accessToken);
        }

        return "Logged out successfully";
    }
}