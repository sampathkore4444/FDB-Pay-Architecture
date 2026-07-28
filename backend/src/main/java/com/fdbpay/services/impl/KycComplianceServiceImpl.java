package com.fdbpay.services.impl;

import com.fdbpay.models.entity.KycDocument;
import com.fdbpay.repositories.KycDocumentRepository;
import com.fdbpay.services.KycComplianceService;
import com.fdbpay.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycComplianceServiceImpl implements KycComplianceService {

    private final KycDocumentRepository kycDocumentRepository;
    private final NotificationService notificationService;

    @Override
    public void submitKycDocuments(UUID userId, Map<String, Object> documents) {
        KycDocument existing = kycDocumentRepository.findByUserId(userId).orElse(null);

        List<KycDocument.DocumentEntry> docEntries = List.of();

        if (existing != null) {
            existing.setDocuments(docEntries);
            existing.setStatus("PENDING");
            existing.setSubmittedAt(OffsetDateTime.now());
            kycDocumentRepository.save(existing);
        } else {
            KycDocument kyc = KycDocument.builder()
                    .userId(userId)
                    .tier("ENHANCED")
                    .documents(docEntries)
                    .status("PENDING")
                    .submittedAt(OffsetDateTime.now())
                    .build();
            kycDocumentRepository.save(kyc);
        }

        log.info("KYC documents submitted: userId={}", userId);
    }

    @Override
    public void reviewKyc(UUID userId, String status, String reviewNotes, UUID reviewedBy) {
        KycDocument kyc = kycDocumentRepository.findByUserId(userId).orElse(null);
        if (kyc != null) {
            kyc.setStatus(status);
            kyc.setReviewedAt(OffsetDateTime.now());
            kycDocumentRepository.save(kyc);
            notificationService.sendKycStatusUpdate(userId.toString(), status);
        }
        log.info("KYC reviewed: userId={}, status={}", userId, status);
    }

    @Override
    public Map<String, Object> getKycStatus(UUID userId) {
        KycDocument kyc = kycDocumentRepository.findByUserId(userId).orElse(null);
        if (kyc == null) {
            return Map.of("status", "NOT_SUBMITTED");
        }
        return Map.of("status", kyc.getStatus(), "tier", kyc.getTier(), "submittedAt", kyc.getSubmittedAt());
    }

    @Override
    public Map<String, Object> getPendingKycRequests(int page, int size) {
        return Map.of("requests", List.of(), "total", 0);
    }

    @Override
    public boolean isKycRequired(UUID userId, Long transactionAmount) {
        return transactionAmount > 500_000L;
    }
}
