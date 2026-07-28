package com.fdbpay.services.impl;

import com.fdbpay.services.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {

    @Override
    public Map<String, Object> getFeeSchedules() {
        return Map.of(
                "STANDARD", Map.of("merchantFee", 0.015, "p2pFee", 0.0),
                "PREMIUM", Map.of("merchantFee", 0.01, "p2pFee", 0.0)
        );
    }

    @Override
    public Map<String, Object> getTransactionLimits() {
        return Map.of(
                "BASIC", Map.of("daily", 500000, "monthly", 5000000, "perTxn", 200000),
                "ENHANCED", Map.of("daily", 5000000, "monthly", 50000000, "perTxn", 2000000),
                "FULL", Map.of("daily", 50000000, "monthly", 500000000, "perTxn", 20000000)
        );
    }

    @Override
    public Map<String, Object> getSystemParameters() {
        return Map.of(
                "otpExpiryMinutes", 3,
                "pinMaxAttempts", 5,
                "pinLockoutMinutes", 30,
                "maxTrustedDevices", 3
        );
    }

    @Override
    public void updateFeeSchedule(String scheduleId, Map<String, Object> updates) {
        log.info("Fee schedule updated: id={}, updates={}", scheduleId, updates);
    }

    @Override
    public void updateTransactionLimits(Map<String, Object> limits) {
        log.info("Transaction limits updated: {}", limits);
    }

    @Override
    public void updateSystemParameter(String key, String value) {
        log.info("System parameter updated: key={}, value={}", key, value);
    }
}
