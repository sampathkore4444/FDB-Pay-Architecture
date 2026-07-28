package com.fdbpay.services;

import java.util.Map;

public interface ConfigService {

    Map<String, Object> getFeeSchedules();

    Map<String, Object> getTransactionLimits();

    Map<String, Object> getSystemParameters();

    void updateFeeSchedule(String scheduleId, Map<String, Object> updates);

    void updateTransactionLimits(Map<String, Object> limits);

    void updateSystemParameter(String key, String value);
}
