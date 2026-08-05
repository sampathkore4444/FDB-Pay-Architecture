package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.FinancingApplicationRequest;
import com.fdbpay.merchant.service.dto.response.FinancingApplicationResponse;
import com.fdbpay.merchant.service.dto.response.FinancingEligibilityResponse;

import java.util.List;
import java.util.UUID;

public interface FinancingService {

    FinancingEligibilityResponse getEligibility(UUID merchantId, UUID walletId);

    FinancingApplicationResponse apply(UUID merchantId, UUID walletId, FinancingApplicationRequest request);

    List<FinancingApplicationResponse> getApplications(UUID merchantId);

    FinancingApplicationResponse getApplication(UUID merchantId, UUID applicationId);
}
