package com.fdbpay.services.impl;

import com.fdbpay.services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    @Async
    public void sendTransactionNotification(String userId, String type, Long amount, String reference) {
        log.info("Sending transaction notification: userId={}, type={}, amount={}, ref={}", userId, type, amount, reference);
    }

    @Override
    @Async
    public void sendOtpSms(String phone, String code) {
        log.info("Sending OTP SMS to phone={}", phone);
    }

    @Override
    @Async
    public void sendKycStatusUpdate(String userId, String status) {
        log.info("Sending KYC status update: userId={}, status={}", userId, status);
    }

    @Override
    @Async
    public void sendSettlementNotification(String merchantId, Long amount, String settlementRef) {
        log.info("Sending settlement notification: merchantId={}, amount={}", merchantId, amount);
    }

    @Override
    @Async
    public void sendPushNotification(String userId, String title, String body) {
        log.info("Sending push notification: userId={}, title={}", userId, title);
    }

    @Override
    @Async
    public void sendBulkNotification(String[] userIds, String title, String body) {
        log.info("Sending bulk notification to {} users", userIds.length);
    }
}
