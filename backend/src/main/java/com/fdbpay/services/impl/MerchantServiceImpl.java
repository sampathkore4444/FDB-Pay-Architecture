package com.fdbpay.services.impl;

import com.fdbpay.common.constants.ErrorCodes;
import com.fdbpay.common.exceptions.BusinessException;
import com.fdbpay.common.exceptions.ResourceNotFoundException;
import com.fdbpay.models.entity.Merchant;
import com.fdbpay.models.entity.User;
import com.fdbpay.models.enums.MerchantStatus;
import com.fdbpay.repositories.MerchantRepository;
import com.fdbpay.repositories.UserRepository;
import com.fdbpay.schemas.request.MerchantRegisterRequest;
import com.fdbpay.schemas.response.MerchantResponse;
import com.fdbpay.schemas.response.SettlementResponse;
import com.fdbpay.services.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MerchantResponse register(UUID userId, MerchantRegisterRequest request) {
        if (merchantRepository.existsByUserId(userId)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "User is already registered as a merchant");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Merchant merchant = Merchant.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType())
                .businessLicense(request.getBusinessLicense())
                .taxId(request.getTaxId())
                .settlementAccount(request.getSettlementAccount())
                .category(request.getCategory())
                .address(request.getAddress())
                .status(MerchantStatus.PENDING)
                .build();

        merchant = merchantRepository.save(merchant);
        log.info("Merchant registered: userId={}, merchantId={}", userId, merchant.getId());

        return mapToResponse(merchant);
    }

    @Override
    public MerchantResponse getProfile(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        return mapToResponse(merchant);
    }

    @Override
    @Transactional
    public MerchantResponse updateProfile(UUID merchantId, MerchantRegisterRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        merchant.setBusinessName(request.getBusinessName());
        merchant.setBusinessType(request.getBusinessType());
        merchant.setCategory(request.getCategory());
        merchant.setAddress(request.getAddress());

        merchant = merchantRepository.save(merchant);
        return mapToResponse(merchant);
    }

    @Override
    public Map<String, Object> getTransactions(UUID merchantId, Pageable pageable) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        Map<String, Object> result = new HashMap<>();
        result.put("merchantId", merchantId);
        result.put("message", "Transaction list for merchant");
        result.put("page", pageable.getPageNumber());
        result.put("size", pageable.getPageSize());
        return result;
    }

    @Override
    public Map<String, Object> getSettlements(UUID merchantId, Pageable pageable) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        Map<String, Object> result = new HashMap<>();
        result.put("merchantId", merchantId);
        result.put("message", "Settlement list for merchant");
        return result;
    }

    @Override
    public SettlementResponse getSettlementDetail(UUID merchantId, UUID settlementId) {
        return SettlementResponse.builder()
                .id(settlementId)
                .merchantId(merchantId)
                .build();
    }

    @Override
    public String generateQrCode(UUID merchantId, String type, Long amount) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        String qrData = String.format("fdbpay://pay?merchant=%s&type=%s&amount=%s",
                merchant.getId(), type, amount != null ? amount : "dynamic");

        log.info("QR generated for merchant: {}", merchantId);
        return qrData;
    }

    @Override
    public void processSettlements() {
        log.info("Processing daily merchant settlements...");
    }

    private MerchantResponse mapToResponse(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getId())
                .userId(merchant.getUser().getId())
                .businessName(merchant.getBusinessName())
                .businessType(merchant.getBusinessType())
                .category(merchant.getCategory())
                .status(merchant.getStatus().name())
                .settlementType(merchant.getSettlementType().name())
                .feeSchedule(merchant.getFeeSchedule())
                .address(merchant.getAddress())
                .qrStaticUrl(merchant.getQrStaticUrl())
                .createdAt(merchant.getCreatedAt())
                .build();
    }
}
