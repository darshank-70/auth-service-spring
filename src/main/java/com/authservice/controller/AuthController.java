package com.authservice.controller;

import com.authservice.dto.request.LoginRequest;
import com.authservice.dto.request.RegisterRequest;
import com.authservice.dto.response.ApiResponse;
import com.authservice.dto.response.AuthResponse;
import com.authservice.entity.RefreshToken;
import com.authservice.entity.User;
import com.authservice.security.JwtService;
import com.authservice.security.TokenBlacklistService;
import com.authservice.service.AuthService;
import com.authservice.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
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
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("REQUEST RECIEVED" +  request.toString());
        authService.register(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .timestamp(LocalDateTime.now())
                        .message("SUCCESS").success(true)
                        .build()
        );
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        ResponseCookie cookie = ResponseCookie.from(HttpHeaders.SET_COOKIE,authResponse.getRefreshToken())
                .sameSite("Strict")
                .httpOnly(true)
                .maxAge(Duration.ofDays(7))
                .path("/")
                .secure(true)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .data(authResponse)
                        .success(true)
                        .message("Login Success")
                        .timestamp(LocalDateTime.now())
                        .build()
        );


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