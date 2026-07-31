package com.fdbpay.auth.service.impl;

import com.fdbpay.auth.dto.request.*;
import com.fdbpay.auth.dto.response.AdminUserResponse;
import com.fdbpay.auth.dto.response.AuthResponse;
import com.fdbpay.auth.dto.response.UserProfileResponse;
import com.fdbpay.auth.model.User;
import com.fdbpay.auth.model.enums.UserStatus;
import com.fdbpay.auth.repository.UserRepository;
import com.fdbpay.auth.service.AuthService;
import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.config.JwtTokenProvider;
import com.fdbpay.shared.event.NotificationEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebClient.Builder webClientBuilder;

    @Value("${jwt.expiration-ms:900000}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Phone number already registered");
        }

        String referralCode = generateUniqueReferralCode();

        User user = User.builder()
                .phone(request.getPhone())
                .name(request.getName())
                .email(request.getEmail())
                .pinHash(passwordEncoder.encode(request.getPin()))
                .referralCode(referralCode)
                .referredBy(request.getReferralCode())
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getPhone());

        createDefaultWallet(user.getId().toString());

        publishNotificationEvent(user.getPhone(), "WELCOME", Map.of(
                "name", user.getName(),
                "referralCode", referralCode
        ));

        String accessToken = jwtTokenProvider.generateToken(user.getId().toString());
        String refreshToken = generateRefreshToken(user.getId().toString());

        return AuthResponse.of(accessToken, refreshToken, jwtExpirationMs / 1000, user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED, "Account is suspended");
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCodes.PIN_LOCKED, "Account is locked");
        }

        if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCodes.PIN_LOCKED, "PIN is locked. Try again later");
        }

        if (!passwordEncoder.matches(request.getPin(), user.getPinHash())) {
            handleFailedPinAttempt(user);
            throw new BusinessException(ErrorCodes.INVALID_PIN, "Invalid PIN");
        }

        resetPinAttempts(user);

        String accessToken = jwtTokenProvider.generateToken(user.getId().toString());
        String refreshToken = generateRefreshToken(user.getId().toString());

        return AuthResponse.of(accessToken, refreshToken, jwtExpirationMs / 1000, user);
    }

    @Override
    public void sendOtp(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        String otpCode = generateOtpCode();
        String redisKey = AppConstants.OTP_CACHE_PREFIX + phone;

        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(AppConstants.OTP_EXPIRY_MINUTES));
        log.info("OTP sent to {}: {}", phone, otpCode);

        publishNotificationEvent(phone, "OTP", Map.of("code", otpCode));
    }

    @Override
    public void verifyOtp(OtpRequest request) {
        String redisKey = AppConstants.OTP_CACHE_PREFIX + request.getPhone();
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new BusinessException(ErrorCodes.OTP_EXPIRED, "OTP has expired");
        }

        if (!storedCode.equals(request.getCode())) {
            throw new BusinessException(ErrorCodes.OTP_INVALID, "Invalid OTP code");
        }

        redisTemplate.delete(redisKey);

        userRepository.findByPhone(request.getPhone()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.PENDING) {
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
            }
        });

        log.info("OTP verified for {}", request.getPhone());
    }

    @Override
    @Transactional
    public void setPin(PinSetRequest request, String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        if (user.getPinHash() != null && !passwordEncoder.matches(request.getCurrentPin(), user.getPinHash())) {
            handleFailedPinAttempt(user);
            throw new BusinessException(ErrorCodes.INVALID_PIN, "Current PIN is incorrect");
        }

        user.setPinHash(passwordEncoder.encode(request.getNewPin()));
        user.setPinAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN updated for {}", phone);
    }

    @Override
    @Transactional
    public void resetPin(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        String tempPin = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
        user.setPinHash(passwordEncoder.encode(tempPin));
        user.setPinAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN reset for {}. Temporary PIN: {}", phone, tempPin);
        publishNotificationEvent(phone, "PIN_RESET", Map.of("tempPin", tempPin));
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String phone = extractPhoneFromRefreshToken(refreshToken);
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid refresh token"));

        String newAccessToken = jwtTokenProvider.generateToken(user.getId().toString());
        String newRefreshToken = generateRefreshToken(user.getId().toString());

        return AuthResponse.of(newAccessToken, newRefreshToken, jwtExpirationMs / 1000, user);
    }

    @Override
    public void logout(String phone) {
        redisTemplate.delete(AppConstants.OTP_CACHE_PREFIX + phone);
        log.info("User logged out: {}", phone);
    }

    @Override
    public UserProfileResponse getProfile(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "User not found"));
        return UserProfileResponse.from(user);
    }

    private void handleFailedPinAttempt(User user) {
        int attempts = user.getPinAttempts() + 1;
        user.setPinAttempts(attempts);

        if (attempts >= AppConstants.PIN_MAX_ATTEMPTS) {
            user.setStatus(UserStatus.LOCKED);
            user.setPinLockedUntil(LocalDateTime.now().plusMinutes(AppConstants.PIN_LOCKOUT_MINUTES));
            log.warn("Account locked for user {} after {} failed PIN attempts", user.getPhone(), attempts);
        }

        userRepository.save(user);
    }

    private void resetPinAttempts(User user) {
        if (user.getPinAttempts() > 0) {
            user.setPinAttempts(0);
            user.setPinLockedUntil(null);
            userRepository.save(user);
        }
    }

    private String generateOtpCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    private String generateUniqueReferralCode() {
        String code;
        int maxAttempts = 10;
        int attempt = 0;
        do {
            code = IdGenerator.generateReferralCode();
            attempt++;
            if (attempt >= maxAttempts) {
                code = code + ThreadLocalRandom.current().nextInt(10, 99);
                break;
            }
        } while (userRepository.existsByReferralCode(code));
        return code;
    }

    private String generateRefreshToken(String userId) {
        return jwtTokenProvider.generateToken(userId);
    }

    private String extractPhoneFromRefreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid refresh token");
        }
        String userId = jwtTokenProvider.getUsernameFromToken(refreshToken);
        return userRepository.findById(java.util.UUID.fromString(userId))
                .map(User::getPhone)
                .orElseThrow(() -> new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid refresh token"));
    }

    private void createDefaultWallet(String userId) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri("http://wallet-service/wallet")
                    .bodyValue(Map.of("userId", userId))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Default wallet created for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to create default wallet for user {}: {}", userId, e.getMessage());
        }
    }

    private void publishNotificationEvent(String phone, String type, Map<String, Object> data) {
        try {
            String title = type + " Notification";
            String body = data != null ? data.toString() : "";
            NotificationEvent event = NotificationEvent.builder()
                    .phone(phone)
                    .type(type)
                    .channel("SMS")
                    .title(title)
                    .body(body)
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send(AppConstants.TOPIC_NOTIFICATION, event);
        } catch (Exception e) {
            log.error("Failed to publish notification event: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers(String search, UserStatus status, int page, int size) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> status == null || u.getStatus() == status)
                .filter(u -> search == null || search.isBlank()
                        || u.getName().toLowerCase().contains(search.toLowerCase())
                        || u.getPhone().contains(search))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        return users.stream()
                .skip((long) page * size)
                .limit(size)
                .map(AdminUserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void updateUserStatus(UUID userId, UserStatus status, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "User not found with id: " + userId));
        user.setStatus(status);
        userRepository.save(user);
        log.info("User {} status updated to {} reason={}", userId, status, reason);
    }
}
