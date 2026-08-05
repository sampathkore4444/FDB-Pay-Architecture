package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.NotificationEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.merchant.service.client.AuthServiceClient;
import com.fdbpay.merchant.service.dto.request.MerchantRegisterRequest;
import com.fdbpay.merchant.service.dto.response.MerchantResponse;
import com.fdbpay.merchant.service.dto.response.QrCodeResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.enums.MerchantStatus;
import com.fdbpay.merchant.service.model.enums.SettlementType;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public MerchantResponse register(UUID userId, MerchantRegisterRequest request) {
        String businessLicense = request.getBusinessLicense();
        String taxId = request.getTaxId();
        boolean licenseConflict = StringUtils.hasText(businessLicense)
                && merchantRepository.existsByBusinessLicense(businessLicense);
        boolean taxConflict = StringUtils.hasText(taxId)
                && merchantRepository.existsByTaxId(taxId);
        if (licenseConflict || taxConflict) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "A merchant with this business license or tax ID already exists");
        }

        Merchant merchant = Merchant.builder()
                .userId(userId)
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType())
                .businessLicense(request.getBusinessLicense())
                .taxId(request.getTaxId())
                .settlementAccount(request.getSettlementAccount())
                .settlementType(request.getSettlementType())
                .feeSchedule(request.getFeeSchedule())
                .status(MerchantStatus.PENDING)
                .category(request.getCategory())
                .address(request.getAddress())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        merchant = merchantRepository.save(merchant);
        log.info("Merchant registered: merchantId={}, userId={}, businessName={}",
                merchant.getId(), userId, merchant.getBusinessName());

        publishMerchantRegisteredEvent(merchant);

        return mapToResponse(merchant);
    }

    @Override
    public MerchantResponse getProfile(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        return mapToResponse(merchant);
    }

    @Override
    public MerchantResponse getProfileByUserId(UUID userId) {
        Merchant merchant = merchantRepository.findByUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId=" + userId));
        return mapToResponse(merchant);
    }

    @Override
    @Transactional
    public MerchantResponse updateProfile(UUID merchantId, MerchantRegisterRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        if (merchant.getStatus() == MerchantStatus.CLOSED || merchant.getStatus() == MerchantStatus.REJECTED) {
            throw new BusinessException(ErrorCodes.MERCHANT_SUSPENDED,
                    "Cannot update merchant in status: " + merchant.getStatus());
        }

        merchant.setBusinessName(request.getBusinessName());
        merchant.setBusinessType(request.getBusinessType());
        merchant.setBusinessLicense(request.getBusinessLicense());
        merchant.setTaxId(request.getTaxId());
        merchant.setSettlementAccount(request.getSettlementAccount());
        merchant.setSettlementType(request.getSettlementType());
        merchant.setFeeSchedule(request.getFeeSchedule());
        merchant.setCategory(request.getCategory());
        merchant.setAddress(request.getAddress());
        merchant.setUpdatedAt(OffsetDateTime.now());

        merchant = merchantRepository.save(merchant);
        log.info("Merchant profile updated: merchantId={}", merchantId);

        return mapToResponse(merchant);
    }

    @Override
    public QrCodeResponse generateQrCode(UUID merchantId, Long amount) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.MERCHANT_SUSPENDED,
                    "Merchant is not active. Status: " + merchant.getStatus());
        }

        String deepLink = "fdbpay://pay?merchant=" + merchant.getId()
                + "&name=" + merchant.getBusinessName();
        String qrUrl = "https://pay.fdbpay.com/qr/" + merchant.getId();
        if (amount != null && amount > 0) {
            deepLink += "&amount=" + amount;
            qrUrl += "?amount=" + amount;
        }

        QrCodeResponse response = QrCodeResponse.builder()
                .merchantId(merchant.getId())
                .qrUrl(qrUrl)
                .deepLink(deepLink)
                .build();

        merchant.setQrStaticUrl(qrUrl);
        merchantRepository.save(merchant);

        log.info("QR code generated: merchantId={}, amount={}", merchantId, amount);
        return response;
    }

    @Override
    @Transactional
    public MerchantResponse updateSettlementType(UUID merchantId, SettlementType settlementType) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        merchant.setSettlementType(settlementType);
        merchant.setUpdatedAt(OffsetDateTime.now());
        merchant = merchantRepository.save(merchant);
        log.info("Merchant settlement type updated: merchantId={}, type={}", merchantId, settlementType);
        return mapToResponse(merchant);
    }

    @Override
    @Transactional
    public MerchantResponse updateTerminalFields(UUID merchantId, String terminalFields) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        merchant.setTerminalFields(terminalFields);
        merchant.setUpdatedAt(OffsetDateTime.now());
        merchant = merchantRepository.save(merchant);
        log.info("Merchant terminal fields updated: merchantId={}", merchantId);
        return mapToResponse(merchant);
    }

    @Override
    @Transactional
    public MerchantResponse updateRollingReserve(UUID merchantId, Integer percent, Integer periodDays) {
        if (percent == null || percent < 0 || percent > 100) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Rolling reserve percent must be between 0 and 100");
        }
        if (periodDays == null || periodDays < 1) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Rolling reserve period must be at least 1 day");
        }
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        merchant.setRollingReservePercent(percent);
        merchant.setRollingReservePeriodDays(periodDays);
        merchant.setUpdatedAt(OffsetDateTime.now());
        merchant = merchantRepository.save(merchant);
        log.info("Merchant rolling reserve updated: merchantId={}, percent={}, periodDays={}", merchantId, percent, periodDays);
        return mapToResponse(merchant);
    }

    private void publishMerchantRegisteredEvent(Merchant merchant) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(merchant.getUserId())
                    .channel("SYSTEM")
                    .type("MERCHANT_REGISTERED")
                    .title("Merchant Registration")
                    .body("Your merchant account has been registered and is pending approval.")
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send("notification.send", merchant.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish merchant registered event: merchantId={}", merchant.getId(), e);
        }
    }

    private MerchantResponse mapToResponse(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getId())
                .userId(merchant.getUserId())
                .businessName(merchant.getBusinessName())
                .businessType(merchant.getBusinessType())
                .businessLicense(merchant.getBusinessLicense())
                .taxId(merchant.getTaxId())
                .settlementAccount(merchant.getSettlementAccount())
                .settlementType(merchant.getSettlementType())
                .feeSchedule(merchant.getFeeSchedule())
                .status(merchant.getStatus())
                .category(merchant.getCategory())
                .address(merchant.getAddress())
                .latitude(merchant.getLatitude())
                .longitude(merchant.getLongitude())
                .qrStaticUrl(merchant.getQrStaticUrl())
                .rollingReservePercent(merchant.getRollingReservePercent())
                .rollingReservePeriodDays(merchant.getRollingReservePeriodDays())
                .rollingReserveBalance(merchant.getRollingReserveBalance())
                .terminalFields(merchant.getTerminalFields())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantResponse> getMerchants(String search, MerchantStatus status, int page, int size) {
        List<Merchant> merchants = merchantRepository.findAll().stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> search == null || search.isBlank()
                        || m.getBusinessName().toLowerCase().contains(search.toLowerCase())
                        || (m.getCategory() != null && m.getCategory().toLowerCase().contains(search.toLowerCase())))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        return merchants.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public MerchantResponse updateStatus(UUID merchantId, MerchantStatus status, String reason) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        MerchantStatus previousStatus = merchant.getStatus();
        merchant.setStatus(status);
        merchant.setUpdatedAt(OffsetDateTime.now());
        merchant = merchantRepository.save(merchant);
        if (status == MerchantStatus.ACTIVE && previousStatus != MerchantStatus.ACTIVE) {
            authServiceClient.upgradeUserRole(merchant.getUserId(), "MERCHANT");
        }
        log.info("Merchant {} status updated to {} reason={}", merchantId, status, reason);
        return mapToResponse(merchant);
    }
}
