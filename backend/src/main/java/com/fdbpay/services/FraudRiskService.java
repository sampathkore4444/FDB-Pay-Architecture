package com.fdbpay.services;

import com.fdbpay.models.entity.Transaction;
import com.fdbpay.models.enums.TransactionStatus;

public interface FraudRiskService {

    boolean evaluateTransaction(Transaction transaction);

    void recordTransactionOutcome(Transaction transaction, TransactionStatus outcome);

    boolean checkVelocity(UUID userId, Long amount);

    boolean checkDeviceFingerprint(String userId, String deviceId);

    boolean screenSanctions(String name, String nrcNumber);

    class UUID {
        private final String value;
        public UUID(String value) { this.value = value; }
    }
}
