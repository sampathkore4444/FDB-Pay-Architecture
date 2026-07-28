package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.MerchantRegisterRequest;
import com.fdbpay.merchant.service.dto.response.MerchantResponse;
import com.fdbpay.merchant.service.dto.response.QrCodeResponse;

import java.util.UUID;

public interface MerchantService {

    MerchantResponse register(UUID userId, MerchantRegisterRequest request);

    MerchantResponse getProfile(UUID merchantId);

    MerchantResponse updateProfile(UUID merchantId, MerchantRegisterRequest request);

    QrCodeResponse generateQrCode(UUID merchantId);
}
