package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.MerchantRegisterRequest;
import com.fdbpay.merchant.service.dto.response.MerchantResponse;
import com.fdbpay.merchant.service.dto.response.QrCodeResponse;
import com.fdbpay.merchant.service.model.enums.MerchantStatus;

import java.util.List;
import java.util.UUID;

public interface MerchantService {

    MerchantResponse register(UUID userId, MerchantRegisterRequest request);

    MerchantResponse getProfile(UUID merchantId);

    MerchantResponse getProfileByUserId(UUID userId);

    MerchantResponse updateProfile(UUID merchantId, MerchantRegisterRequest request);

    QrCodeResponse generateQrCode(UUID merchantId);

    List<MerchantResponse> getMerchants(String search, MerchantStatus status, int page, int size);

    MerchantResponse updateStatus(UUID merchantId, MerchantStatus status, String reason);
}
