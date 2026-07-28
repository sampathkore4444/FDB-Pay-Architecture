package com.fdbpay.controllers;

import com.fdbpay.schemas.request.*;
import com.fdbpay.schemas.response.ApiResponse;
import com.fdbpay.schemas.response.AuthResponse;
import com.fdbpay.schemas.response.UserProfileResponse;
import com.fdbpay.services.AuthService;
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
    public ResponseEntity<ApiResponse<Void>> setPin(@RequestParam String userId,
                                                    @Valid @RequestBody PinSetRequest request) {
        authService.setPin(userId, request.getCurrentPin(), request.getNewPin());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/pin/reset")
    public ResponseEntity<ApiResponse<Void>> resetPin(@RequestParam String phone,
                                                      @RequestParam String otp,
                                                      @RequestParam String newPin) {
        authService.resetPin(phone, otp, newPin);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestParam String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestParam String userId) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@RequestParam String userId) {
        UserProfileResponse profile = authService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
