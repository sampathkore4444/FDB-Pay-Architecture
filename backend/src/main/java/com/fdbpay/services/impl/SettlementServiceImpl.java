package com.fdbpay.services.impl;

import com.fdbpay.services.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SettlementServiceImpl implements SettlementService {

    @Override
    public void triggerDailySettlement() {
        log.info("Triggering daily merchant settlement...");
    }

    @Override
    public com.fdbpay.schemas.response.SettlementResponse getSettlement(UUID settlementId) {
        return com.fdbpay.schemas.response.SettlementResponse.builder()
                .id(settlementId)
                .build();
    }

    @Override
    public Map<String, Object> getSettlementReport(UUID merchantId, String startDate, String endDate) {
        return Map.of("merchantId", merchantId, "totalSettled", 0L);
    }

    @Override
    public void reconcile() {
        log.info("Running settlement reconciliation...");
    }
}
