package com.fdbpay.merchant.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.merchant.service.dto.response.PaymentLinkPublicResponse;
import com.fdbpay.merchant.service.service.PaymentLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment-links")
@RequiredArgsConstructor
public class PaymentLinkPublicController {

    private final PaymentLinkService paymentLinkService;

    @GetMapping("/token/{token}")
    public ApiResponse<PaymentLinkPublicResponse> getByToken(@PathVariable String token) {
        return ApiResponse.success(paymentLinkService.getByToken(token));
    }

    @PostMapping("/token/{token}/paid")
    public ApiResponse<PaymentLinkPublicResponse> markPaid(@PathVariable String token) {
        return ApiResponse.success(paymentLinkService.markPaid(token));
    }
}
