package com.fdbpay.shared.constants;

public final class ErrorCodes {
    private ErrorCodes() {}

    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String WALLET_NOT_FOUND = "WALLET_NOT_FOUND";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String MERCHANT_NOT_FOUND = "MERCHANT_NOT_FOUND";
    public static final String TRANSACTION_NOT_FOUND = "TRANSACTION_NOT_FOUND";
    public static final String INVALID_PIN = "INVALID_PIN";
    public static final String PIN_LOCKED = "PIN_LOCKED";
    public static final String ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";
    public static final String KYC_REQUIRED = "KYC_REQUIRED";
    public static final String LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
    public static final String DUPLICATE_TRANSACTION = "DUPLICATE_TRANSACTION";
    public static final String MERCHANT_SUSPENDED = "MERCHANT_SUSPENDED";
    public static final String OTP_EXPIRED = "OTP_EXPIRED";
    public static final String OTP_INVALID = "OTP_INVALID";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
}
