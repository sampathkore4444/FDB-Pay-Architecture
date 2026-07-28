package com.fdbpay.shared.utils;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {}

    public static String generateIdempotencyKey() {
        return "idem_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateReferralCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public static String generateSettlementRef() {
        return "STL_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
