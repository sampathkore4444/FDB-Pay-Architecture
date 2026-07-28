package com.fdbpay.services;

import com.fdbpay.schemas.response.SettlementResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface SettlementService {

    void triggerDailySettlement();

    SettlementResponse getSettlement(UUID settlementId);

    Map<String, Object> getSettlementReport(UUID merchantId, String startDate, String endDate);

    void reconcile();
}
