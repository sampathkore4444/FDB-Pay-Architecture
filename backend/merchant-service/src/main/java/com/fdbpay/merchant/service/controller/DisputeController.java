package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.ChargebackEvidenceRequest;
import com.fdbpay.merchant.service.dto.response.ChargebackEvidenceResponse;
import com.fdbpay.merchant.service.service.DisputeService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/chargebacks")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/{chargebackId}/evidence")
    public ApiResponse<List<ChargebackEvidenceResponse>> list(@RequestParam UUID userId, @PathVariable UUID chargebackId) {
        return ApiResponse.success(disputeService.listEvidence(accessHelper.resolveMerchantId(userId), chargebackId));
    }

    @PostMapping("/evidence")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChargebackEvidenceResponse> add(@RequestParam UUID userId,
                                                       @Valid @RequestBody ChargebackEvidenceRequest request) {
        return ApiResponse.success(disputeService.addEvidence(accessHelper.resolveMerchantId(userId), request));
    }

    @GetMapping("/{chargebackId}/evidence/count")
    public ApiResponse<Map<String, Long>> count(@RequestParam UUID userId, @PathVariable UUID chargebackId) {
        return ApiResponse.success(Map.of("count", disputeService.evidenceCount(accessHelper.resolveMerchantId(userId), chargebackId)));
    }
}
