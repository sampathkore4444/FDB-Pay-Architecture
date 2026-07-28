package com.fdbpay.services;

import com.fdbpay.schemas.request.MerchantRegisterRequest;
import com.fdbpay.schemas.response.MerchantResponse;
import com.fdbpay.schemas.response.SettlementResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface MerchantService {

    MerchantResponse register(UUID userId, MerchantRegisterRequest request);

    MerchantResponse getProfile(UUID merchantId);

    MerchantResponse updateProfile(UUID merchantId, MerchantRegisterRequest request);

    Map<String, Object> getTransactions(UUID merchantId, Pageable pageable);

    Map<String, Object> getSettlements(UUID merchantId, Pageable pageable);

    SettlementResponse getSettlementDetail(UUID merchantId, UUID settlementId);

    String generateQrCode(UUID merchantId, String type, Long amount);

    void processSettlements();
}
