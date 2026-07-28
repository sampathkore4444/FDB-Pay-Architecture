package com.fdbpay.services;

import com.fdbpay.schemas.request.LoginRequest;
import com.fdbpay.schemas.request.OtpRequest;
import com.fdbpay.schemas.request.RegisterRequest;
import com.fdbpay.schemas.response.AuthResponse;
import com.fdbpay.schemas.response.UserProfileResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void sendOtp(String phone);

    void verifyOtp(OtpRequest request);

    void setPin(String userId, String currentPin, String newPin);

    void resetPin(String phone, String otp, String newPin);

    AuthResponse refreshToken(String refreshToken);

    void logout(String userId);

    UserProfileResponse getProfile(String userId);
}
