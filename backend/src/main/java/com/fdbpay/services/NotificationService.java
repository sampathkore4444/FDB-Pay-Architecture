package com.fdbpay.services;

public interface NotificationService {

    void sendTransactionNotification(String userId, String type, Long amount, String reference);

    void sendOtpSms(String phone, String code);

    void sendKycStatusUpdate(String userId, String status);

    void sendSettlementNotification(String merchantId, Long amount, String settlementRef);

    void sendPushNotification(String userId, String title, String body);

    void sendBulkNotification(String[] userIds, String title, String body);
}
