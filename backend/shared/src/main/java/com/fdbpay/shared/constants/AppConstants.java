package com.fdbpay.shared.constants;

public final class AppConstants {
    private AppConstants() {}

    public static final String WALLET_BALANCE_CACHE_PREFIX = "wallet:balance:";
    public static final String OTP_CACHE_PREFIX = "otp:";
    public static final String IDEMPOTENCY_CACHE_PREFIX = "idempotency:";
    public static final String RATE_LIMIT_PREFIX = "rl:";

    public static final int OTP_EXPIRY_MINUTES = 3;
    public static final int OTP_MAX_ATTEMPTS = 3;
    public static final int PIN_MAX_ATTEMPTS = 5;
    public static final int PIN_LOCKOUT_MINUTES = 30;

    public static final Long DAILY_LIMIT_BASIC = 500_000L;
    public static final Long MONTHLY_LIMIT_BASIC = 5_000_000L;
    public static final Long DAILY_LIMIT_ENHANCED = 5_000_000L;
    public static final Long MONTHLY_LIMIT_ENHANCED = 50_000_000L;
    public static final Long DAILY_LIMIT_FULL = 50_000_000L;
    public static final Long MONTHLY_LIMIT_FULL = 500_000_000L;

    public static final String TOPIC_TXN_COMPLETED = "txn.completed";
    public static final String TOPIC_TXN_FAILED = "txn.failed";
    public static final String TOPIC_KYC_SUBMITTED = "kyc.submitted";
    public static final String TOPIC_SETTLEMENT_DAILY = "settlement.daily";
    public static final String TOPIC_AUDIT_LOG = "audit.log";
    public static final String TOPIC_NOTIFICATION = "notification.send";
}
