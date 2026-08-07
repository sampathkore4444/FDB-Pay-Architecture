package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.response.ReferralPerformanceResponse;
import com.fdbpay.merchant.service.service.ReferralService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/merchant/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/performance")
    public ApiResponse<ReferralPerformanceResponse> performance(@RequestParam UUID userId) {
        return ApiResponse.success(referralService.performance(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/register")
    public ApiResponse<ReferralPerformanceResponse> register(@RequestParam UUID userId,
                                                             @RequestParam(required = false) UUID programId,
                                                             @RequestParam String referredPhone) {
        return ApiResponse.success(referralService.register(accessHelper.resolveMerchantId(userId), programId, referredPhone));
    }

    @PostMapping("/{registrationId}/convert")
    public ApiResponse<ReferralPerformanceResponse> convert(@RequestParam UUID userId,
                                                            @PathVariable UUID registrationId,
                                                            @RequestParam(required = false) Long bonusPaid) {
        return ApiResponse.success(referralService.markConverted(accessHelper.resolveMerchantId(userId), registrationId, bonusPaid));
    }
}
