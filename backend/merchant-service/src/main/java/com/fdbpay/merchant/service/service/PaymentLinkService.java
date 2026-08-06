package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.CreatePaymentLinkRequest;
import com.fdbpay.merchant.service.dto.response.PaymentLinkPublicResponse;
import com.fdbpay.merchant.service.dto.response.PaymentLinkResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PaymentLinkService {

    PaymentLinkResponse create(UUID merchantId, CreatePaymentLinkRequest request);

    Page<PaymentLinkResponse> getByMerchant(UUID merchantId, int page, int size);

    PaymentLinkResponse deactivate(UUID merchantId, UUID linkId);

    PaymentLinkPublicResponse getByToken(String token);

    PaymentLinkPublicResponse markPaid(String token);

    PaymentLinkResponse resendReminder(UUID merchantId, UUID linkId);
}
