package com.fdbpay.settlement.service.service;

import com.fdbpay.settlement.service.dto.request.SettlementQueryRequest;
import com.fdbpay.settlement.service.dto.request.TriggerSettlementRequest;
import com.fdbpay.settlement.service.dto.response.SettlementBatchResponse;
import com.fdbpay.settlement.service.dto.response.SettlementResponse;
import com.fdbpay.settlement.service.dto.response.SettlementSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface SettlementService {

    void triggerDailySettlement();

    SettlementResponse triggerMerchantSettlement(TriggerSettlementRequest request);

    SettlementResponse getSettlement(UUID settlementId);

    Page<SettlementResponse> getMerchantSettlements(UUID merchantId, int page, int size);

    SettlementSummaryResponse getSettlementSummary(UUID batchId);

    SettlementBatchResponse reconcile();
}
