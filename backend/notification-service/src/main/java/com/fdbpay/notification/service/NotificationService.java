package com.fdbpay.notification.service;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void sendOtpSms(String phone, String code);

    void sendTransactionNotification(UUID userId, String type, Long amount, String reference);

    void sendPushNotification(UUID userId, String title, String body);

    void sendSettlementNotification(UUID merchantId, Long amount, String reference);

    void sendBulkNotification(List<UUID> userIds, String title, String body);
}
