package com.fdbpay.auth.service;

import com.fdbpay.auth.dto.request.*;
import com.fdbpay.auth.dto.response.AuthResponse;
import com.fdbpay.auth.dto.response.UserProfileResponse;

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
}
