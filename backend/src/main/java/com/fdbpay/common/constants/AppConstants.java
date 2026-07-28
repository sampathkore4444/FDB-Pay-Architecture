package com.fdbpay.common.constants;

public final class AppConstants {

    private AppConstants() {}

    public static final String WALLET_BALANCE_CACHE_PREFIX = "wallet:balance:";
    public static final String OTP_CACHE_PREFIX = "otp:";
    public static final String IDEMPOTENCY_CACHE_PREFIX = "idempotency:";
    public static final String RATE_LIMIT_PREFIX = "rl:";
    public static final String SESSION_PREFIX = "session:";

    public static final int OTP_EXPIRY_MINUTES = 3;
    public static final int OTP_MAX_ATTEMPTS = 3;
    public static final int PIN_MAX_ATTEMPTS = 5;
    public static final int PIN_LOCKOUT_MINUTES = 30;
    public static final int MAX_TRUSTED_DEVICES = 3;

    public static final Long DAILY_LIMIT_BASIC = 500_000L;
    public static final Long MONTHLY_LIMIT_BASIC = 5_000_000L;
    public static final Long DAILY_LIMIT_ENHANCED = 5_000_000L;
    public static final Long MONTHLY_LIMIT_ENHANCED = 50_000_000L;
    public static final Long DAILY_LIMIT_FULL = 50_000_000L;
    public static final Long MONTHLY_LIMIT_FULL = 500_000_000L;

    public static final String TOPIC_TXN = "txn.";
    public static final String TOPIC_MERCHANT = "merchant.";
    public static final String TOPIC_KYC = "kyc.";
    public static final String TOPIC_SETTLEMENT = "settlement.";
    public static final String TOPIC_AUDIT = "audit.";

    public static final String API_VERSION = "/v1";
}
