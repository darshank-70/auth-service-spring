package com.authservice.service;

import com.authservice.dto.request.LoginRequest;
import com.authservice.dto.request.RegisterRequest;
import com.authservice.dto.response.AuthResponse;
import com.authservice.entity.RefreshToken;
import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.exception.BadRequestException;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {

        // 1. check if user exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("User already exists");
        }

        // 2. get default role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        // 3. create user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .roles(Set.of(userRole))
                .build();

        // 4. save
        userRepository.save(user);

        return AuthResponse.builder()
                .message("User registered successfully")
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String role = user.getRoles().stream()
                .findFirst()
                .orElseThrow().getName();

        String accessToken = jwtService.generateToken(user.getEmail(), role);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

//        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
//                .maxAge(Duration.ofDays(7))
//                .httpOnly(true)
//                .secure(true)
//                .path("/")
//                .sameSite("Strict")
//                .build();
//        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
//service only returns the DATA***. and only COntroller works with Response and requests
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .message("Logged in as " + user.getEmail()).build();
    }
}