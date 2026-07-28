package com.fdbpay.notification.controller;

import com.fdbpay.notification.service.NotificationService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<?>> sendOtp(
            @RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");
        notificationService.sendOtpSms(phone, code);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "OTP sent successfully", "phone", phone)));
    }

    @PostMapping("/send-transaction")
    public ResponseEntity<ApiResponse<?>> sendTransactionNotification(
            @RequestBody Map<String, Object> request) {
        UUID userId = UUID.fromString((String) request.get("userId"));
        String type = (String) request.get("type");
        Long amount = Long.valueOf(request.get("amount").toString());
        String reference = (String) request.get("reference");
        notificationService.sendTransactionNotification(userId, type, amount, reference);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Transaction notification sent", "userId", userId)));
    }

    @PostMapping("/send-push")
    public ResponseEntity<ApiResponse<?>> sendPushNotification(
            @RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        String title = request.get("title");
        String body = request.get("body");
        notificationService.sendPushNotification(userId, title, body);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Push notification sent", "userId", userId)));
    }

    @PostMapping("/send-settlement")
    public ResponseEntity<ApiResponse<?>> sendSettlementNotification(
            @RequestBody Map<String, Object> request) {
        UUID merchantId = UUID.fromString((String) request.get("merchantId"));
        Long amount = Long.valueOf(request.get("amount").toString());
        String reference = (String) request.get("reference");
        notificationService.sendSettlementNotification(merchantId, amount, reference);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Settlement notification sent", "merchantId", merchantId)));
    }

    @PostMapping("/send-bulk")
    public ResponseEntity<ApiResponse<?>> sendBulkNotification(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> userIdStrings = (List<String>) request.get("userIds");
        List<UUID> userIds = userIdStrings.stream().map(UUID::fromString).toList();
        String title = (String) request.get("title");
        String body = (String) request.get("body");
        notificationService.sendBulkNotification(userIds, title, body);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Bulk notification queued", "count", userIds.size())));
    }
}
