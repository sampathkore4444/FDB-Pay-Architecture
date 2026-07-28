package com.fdbpay.kyc.service.service;

import com.fdbpay.kyc.service.dto.request.KycReviewRequest;
import com.fdbpay.kyc.service.dto.request.KycSubmitRequest;
import com.fdbpay.kyc.service.dto.response.KycStatusResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface KycService {

    KycStatusResponse submitDocuments(UUID userId, KycSubmitRequest request);

    KycStatusResponse reviewKyc(UUID userId, KycReviewRequest request, String reviewedBy);

    KycStatusResponse getKycStatus(UUID userId);

    Page<KycStatusResponse> getPendingRequests(int page, int size);
}
