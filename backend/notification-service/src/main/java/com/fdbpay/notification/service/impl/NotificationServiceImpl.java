package com.fdbpay.notification.service.impl;

import com.fdbpay.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    @Async
    public void sendOtpSms(String phone, String code) {
        log.info("[OTP SMS] Sending OTP to phone={}, code={}, timestamp={}", phone, code, OffsetDateTime.now());
        log.info("[OTP SMS] SMS gateway integration would be called here for phone={}", phone);
    }

    @Override
    @Async
    public void sendTransactionNotification(UUID userId, String type, Long amount, String reference) {
        log.info("[TRANSACTION] Sending transaction notification: userId={}, type={}, amount={}, reference={}, timestamp={}",
                userId, type, amount, reference, OffsetDateTime.now());
        log.info("[TRANSACTION] Push/SMS notification would be dispatched for userId={}", userId);
    }

    @Override
    @Async
    public void sendPushNotification(UUID userId, String title, String body) {
        log.info("[PUSH] Sending push notification: userId={}, title={}, body={}, timestamp={}",
                userId, title, body, OffsetDateTime.now());
        log.info("[PUSH] FCM/APNs integration would be called here for userId={}", userId);
    }

    @Override
    @Async
    public void sendSettlementNotification(UUID merchantId, Long amount, String reference) {
        log.info("[SETTLEMENT] Sending settlement notification: merchantId={}, amount={}, reference={}, timestamp={}",
                merchantId, amount, reference, OffsetDateTime.now());
        log.info("[SETTLEMENT] Settlement notification would be dispatched for merchantId={}", merchantId);
    }

    @Override
    @Async
    public void sendBulkNotification(List<UUID> userIds, String title, String body) {
        log.info("[BULK] Sending bulk notification: userCount={}, title={}, timestamp={}",
                userIds.size(), title, OffsetDateTime.now());
        userIds.forEach(userId ->
                log.info("[BULK] Notification queued for userId={}, title={}", userId, title)
        );
    }
}
