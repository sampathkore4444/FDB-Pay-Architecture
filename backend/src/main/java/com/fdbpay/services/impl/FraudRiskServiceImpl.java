package com.fdbpay.services.impl;

import com.fdbpay.models.entity.Transaction;
import com.fdbpay.models.enums.TransactionStatus;
import com.fdbpay.services.FraudRiskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class FraudRiskServiceImpl implements FraudRiskService {

    private static final Long MAX_TRANSACTION_AMOUNT = 20_000_000L;
    private static final int MAX_TRANSACTIONS_PER_HOUR = 20;

    @Override
    public boolean evaluateTransaction(Transaction transaction) {
        if (transaction.getAmount() > MAX_TRANSACTION_AMOUNT) {
            log.warn("High-value transaction flagged: txnId={}, amount={}", transaction.getId(), transaction.getAmount());
            return false;
        }

        if (!checkVelocity(String.valueOf(transaction.getSenderWallet().getUser().getId()), transaction.getAmount())) {
            log.warn("Velocity check failed: userId={}", transaction.getSenderWallet().getUser().getId());
            return false;
        }

        return true;
    }

    @Override
    public void recordTransactionOutcome(Transaction transaction, TransactionStatus outcome) {
        log.info("Transaction outcome recorded: txnId={}, outcome={}", transaction.getId(), outcome);
    }

    @Override
    public boolean checkVelocity(UUID userId, Long amount) {
        log.debug("Velocity check for userId={}", userId);
        return true;
    }

    @Override
    public boolean checkDeviceFingerprint(String userId, String deviceId) {
        log.debug("Device fingerprint check for userId={}, deviceId={}", userId, deviceId);
        return true;
    }

    @Override
    public boolean screenSanctions(String name, String nrcNumber) {
        log.debug("Sanctions screening for name={}", name);
        return true;
    }

    class UUID {
        private final String value;
        public UUID(String value) { this.value = value; }
    }
}
