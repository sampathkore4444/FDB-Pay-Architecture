package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.ChargebackEvidenceRequest;
import com.fdbpay.merchant.service.dto.response.ChargebackEvidenceResponse;
import com.fdbpay.merchant.service.model.Chargeback;
import com.fdbpay.merchant.service.model.ChargebackEvidence;
import com.fdbpay.merchant.service.repository.ChargebackEvidenceRepository;
import com.fdbpay.merchant.service.repository.ChargebackRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final ChargebackEvidenceRepository evidenceRepository;
    private final ChargebackRepository chargebackRepository;
    private final MerchantRepository merchantRepository;

    public List<ChargebackEvidenceResponse> listEvidence(UUID merchantId, UUID chargebackId) {
        requireMerchant(merchantId);
        getOwnedChargeback(merchantId, chargebackId);
        return evidenceRepository.findByChargebackIdOrderByCreatedAtDesc(chargebackId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ChargebackEvidenceResponse addEvidence(UUID merchantId, ChargebackEvidenceRequest request) {
        requireMerchant(merchantId);
        getOwnedChargeback(merchantId, request.getChargebackId());
        ChargebackEvidence evidence = ChargebackEvidence.builder()
                .chargebackId(request.getChargebackId())
                .merchantId(merchantId)
                .type(request.getType())
                .reference(request.getReference())
                .content(request.getContent())
                .build();
        evidence = evidenceRepository.save(evidence);
        return mapToResponse(evidence);
    }

    public long evidenceCount(UUID merchantId, UUID chargebackId) {
        requireMerchant(merchantId);
        getOwnedChargeback(merchantId, chargebackId);
        return evidenceRepository.countByChargebackId(chargebackId);
    }

    private Chargeback getOwnedChargeback(UUID merchantId, UUID chargebackId) {
        Chargeback chargeback = chargebackRepository.findById(chargebackId)
                .orElseThrow(() -> new ResourceNotFoundException("Chargeback", chargebackId.toString()));
        if (!chargeback.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Chargeback does not belong to this merchant");
        }
        return chargeback;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private ChargebackEvidenceResponse mapToResponse(ChargebackEvidence evidence) {
        return ChargebackEvidenceResponse.builder()
                .id(evidence.getId())
                .chargebackId(evidence.getChargebackId())
                .type(evidence.getType())
                .reference(evidence.getReference())
                .content(evidence.getContent())
                .createdAt(evidence.getCreatedAt())
                .build();
    }
}
