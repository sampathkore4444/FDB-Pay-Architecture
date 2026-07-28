package com.fdbpay.services.impl;

import com.fdbpay.common.constants.AppConstants;
import com.fdbpay.common.constants.ErrorCodes;
import com.fdbpay.common.exceptions.BusinessException;
import com.fdbpay.common.exceptions.ResourceNotFoundException;
import com.fdbpay.common.utils.IdGenerator;
import com.fdbpay.core.security.JwtTokenProvider;
import com.fdbpay.models.entity.User;
import com.fdbpay.models.entity.Wallet;
import com.fdbpay.models.enums.KycTier;
import com.fdbpay.models.enums.UserRole;
import com.fdbpay.models.enums.UserStatus;
import com.fdbpay.models.enums.WalletStatus;
import com.fdbpay.repositories.UserRepository;
import com.fdbpay.repositories.WalletRepository;
import com.fdbpay.schemas.request.LoginRequest;
import com.fdbpay.schemas.request.OtpRequest;
import com.fdbpay.schemas.request.RegisterRequest;
import com.fdbpay.schemas.response.AuthResponse;
import com.fdbpay.schemas.response.UserProfileResponse;
import com.fdbpay.services.AuthService;
import com.fdbpay.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Phone number already registered");
        }

        User user = User.builder()
                .phone(request.getPhone())
                .name(request.getName())
                .email(request.getEmail())
                .pinHash(passwordEncoder.encode(request.getPin()))
                .status(UserStatus.ACTIVE)
                .kycTier(KycTier.BASIC)
                .role(UserRole.CONSUMER)
                .referralCode(IdGenerator.generateReferralCode())
                .build();

        user = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(user)
                .currency("MMK")
                .status(WalletStatus.ACTIVE)
                .kycTier(KycTier.BASIC)
                .dailyLimit(AppConstants.DAILY_LIMIT_BASIC)
                .monthlyLimit(AppConstants.MONTHLY_LIMIT_BASIC)
                .build();

        walletRepository.save(wallet);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPin()));

        String accessToken = tokenProvider.generateAccessToken(auth);
        String refreshToken = tokenProvider.generateRefreshToken(auth);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToProfile(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getPhone()));

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED, "Account is " + user.getStatus().name().toLowerCase());
        }

        if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCodes.PIN_LOCKED, "Account is temporarily locked. Try again later.");
        }

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPin()));

        String accessToken = tokenProvider.generateAccessToken(auth);
        String refreshToken = tokenProvider.generateRefreshToken(auth);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToProfile(user))
                .build();
    }

    @Override
    public void sendOtp(String phone) {
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        redisTemplate.opsForValue().set(
                AppConstants.OTP_CACHE_PREFIX + phone,
                code,
                AppConstants.OTP_EXPIRY_MINUTES,
                TimeUnit.MINUTES);

        notificationService.sendOtpSms(phone, code);
    }

    @Override
    public void verifyOtp(OtpRequest request) {
        String cachedCode = redisTemplate.opsForValue().get(AppConstants.OTP_CACHE_PREFIX + request.getPhone());
        if (cachedCode == null) {
            throw new BusinessException(ErrorCodes.OTP_EXPIRED, "OTP has expired");
        }
        if (!cachedCode.equals(request.getCode())) {
            throw new BusinessException(ErrorCodes.OTP_INVALID, "Invalid OTP code");
        }
        redisTemplate.delete(AppConstants.OTP_CACHE_PREFIX + request.getPhone());
    }

    @Override
    @Transactional
    public void setPin(String userId, String currentPin, String newPin) {
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(currentPin, user.getPinHash())) {
            throw new BusinessException(ErrorCodes.INVALID_PIN, "Current PIN is incorrect");
        }

        user.setPinHash(passwordEncoder.encode(newPin));
        user.setPinAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPin(String phone, String otp, String newPin) {
        verifyOtp(new OtpRequest() {{
            setPhone(phone);
            setCode(otp);
        }});

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User", phone));

        user.setPinHash(passwordEncoder.encode(newPin));
        user.setPinAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByPhone(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getPhone(), null, java.util.List.of());
        String newAccessToken = tokenProvider.generateAccessToken(auth);
        String newRefreshToken = tokenProvider.generateRefreshToken(auth);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToProfile(user))
                .build();
    }

    @Override
    public void logout(String userId) {
        log.info("User {} logged out", userId);
    }

    @Override
    public UserProfileResponse getProfile(String userId) {
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return mapToProfile(user);
    }

    private UserProfileResponse mapToProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .kycTier(user.getKycTier().name())
                .role(user.getRole().name())
                .referralCode(user.getReferralCode())
                .build();
    }
}
