package com.fdbpay.auth.service;

import com.fdbpay.auth.dto.request.*;
import com.fdbpay.auth.dto.response.AdminUserResponse;
import com.fdbpay.auth.dto.response.AuthResponse;
import com.fdbpay.auth.dto.response.UserProfileResponse;
import com.fdbpay.auth.model.enums.UserRole;
import com.fdbpay.auth.model.enums.UserStatus;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void sendOtp(String phone);

    void verifyOtp(OtpRequest request);

    void setPin(PinSetRequest request, String phone);

    void resetPin(String phone);

    AuthResponse refreshToken(String refreshToken);

    void logout(String phone);

    UserProfileResponse getProfile(String phone);

    List<AdminUserResponse> getUsers(String search, UserStatus status, int page, int size);

    void updateUserStatus(UUID userId, UserStatus status, String reason);

    void updateUserRole(UUID userId, UserRole role);

    UserProfileResponse getUserById(UUID userId);
}
