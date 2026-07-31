package com.fdbpay.auth.controller;

import com.fdbpay.auth.dto.request.*;
import com.fdbpay.auth.dto.response.AuthResponse;
import com.fdbpay.auth.dto.response.UserProfileResponse;
import com.fdbpay.auth.service.AuthService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@RequestParam String phone) {
        authService.sendOtp(phone);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody OtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/pin/set")
    public ResponseEntity<ApiResponse<Void>> setPin(
            @Valid @RequestBody PinSetRequest request,
            @RequestHeader("X-User-Phone") String phone) {
        authService.setPin(request, phone);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/pin/reset")
    public ResponseEntity<ApiResponse<Void>> resetPin(@RequestParam String phone) {
        authService.resetPin(phone);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestParam String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("X-User-Phone") String phone) {
        authService.logout(phone);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@RequestHeader("X-User-Phone") String phone) {
        UserProfileResponse profile = authService.getProfile(phone);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/user/by-phone")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByPhone(@RequestParam String phone) {
        UserProfileResponse profile = authService.getProfile(phone);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
