package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.response.RiskAlertResponse;

import java.util.List;
import java.util.UUID;

public interface RiskAlertService {

    List<RiskAlertResponse> getAlerts(UUID merchantId, UUID walletId);

    RiskAlertResponse acknowledge(UUID merchantId, UUID alertId);
}
