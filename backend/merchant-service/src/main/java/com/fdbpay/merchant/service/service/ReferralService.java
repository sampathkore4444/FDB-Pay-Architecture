package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.response.ReferralPerformanceResponse;
import com.fdbpay.merchant.service.model.ReferralRegistration;
import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.ReferralRegistrationRepository;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRegistrationRepository registrationRepository;
    private final MerchantRepository merchantRepository;

    @Transactional
    public ReferralPerformanceResponse register(UUID merchantId, UUID programId, String referredPhone) {
        requireMerchant(merchantId);
        ReferralRegistration registration = ReferralRegistration.builder()
                .merchantId(merchantId)
                .programId(programId)
                .referredPhone(referredPhone)
                .status(ActiveStatus.ACTIVE)
                .build();
        registration = registrationRepository.save(registration);
        return performance(merchantId);
    }

    @Transactional
    public ReferralPerformanceResponse markConverted(UUID merchantId, UUID registrationId, Long bonusPaid) {
        requireMerchant(merchantId);
        ReferralRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("ReferralRegistration", registrationId.toString()));
        if (!registration.getMerchantId().equals(merchantId)) {
            throw new com.fdbpay.shared.exceptions.BusinessException(
                    com.fdbpay.shared.constants.ErrorCodes.UNAUTHORIZED, "Registration does not belong to this merchant");
        }
        registration.setConvertedAt(OffsetDateTime.now());
        registration.setBonusPaid(bonusPaid == null ? 0L : bonusPaid);
        registrationRepository.save(registration);
        return performance(merchantId);
    }

    public ReferralPerformanceResponse performance(UUID merchantId) {
        requireMerchant(merchantId);
        List<ReferralRegistration> all = registrationRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        long registrations = all.size();
        long converted = all.stream().filter(r -> r.getConvertedAt() != null).count();
        long bonusPaid = all.stream().mapToLong(r -> r.getBonusPaid() == null ? 0L : r.getBonusPaid()).sum();
        double conversionRate = registrations == 0 ? 0 : Math.round(converted * 10000.0 / registrations) / 100.0;
        return ReferralPerformanceResponse.builder()
                .totalRegistrations(registrations)
                .convertedRegistrations(converted)
                .pendingRegistrations(registrations - converted)
                .conversionRatePct(conversionRate)
                .totalBonusPaid(bonusPaid)
                .registrations(all)
                .build();
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }
}
