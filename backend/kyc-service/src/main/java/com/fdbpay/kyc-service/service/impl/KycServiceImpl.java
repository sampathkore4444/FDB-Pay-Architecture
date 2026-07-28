package com.fdbpay.kyc.service.service.impl;

import com.fdbpay.kyc.service.dto.request.DocumentRequest;
import com.fdbpay.kyc.service.dto.request.KycReviewRequest;
import com.fdbpay.kyc.service.dto.request.KycSubmitRequest;
import com.fdbpay.kyc.service.dto.response.KycStatusResponse;
import com.fdbpay.kyc.service.model.KycAudit;
import com.fdbpay.kyc.service.model.KycDocument;
import com.fdbpay.kyc.service.model.KycDocument.DocumentEntry;
import com.fdbpay.kyc.service.repository.KycAuditRepository;
import com.fdbpay.kyc.service.repository.KycDocumentMongoRepository;
import com.fdbpay.kyc.service.service.KycService;
import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final KycDocumentMongoRepository kycDocumentRepository;
    private final KycAuditRepository kycAuditRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public KycStatusResponse submitDocuments(UUID userId, KycSubmitRequest request) {
        log.info("Submitting KYC documents for user: {}", userId);

        KycDocument existingDoc = kycDocumentRepository.findByUserId(userId).orElse(null);

        List<DocumentEntry> documentEntries = request.getDocuments().stream()
                .map(doc -> DocumentEntry.builder()
                        .type(doc.getType())
                        .fileUrl(doc.getFileUrl())
                        .uploadedAt(OffsetDateTime.now())
                        .verified(false)
                        .build())
                .toList();

        KycDocument kycDocument;
        if (existingDoc != null) {
            existingDoc.setTier(request.getTier());
            existingDoc.setDocuments(documentEntries);
            existingDoc.setStatus(STATUS_PENDING);
            existingDoc.setSubmittedAt(OffsetDateTime.now());
            existingDoc.setReviewedAt(null);
            kycDocument = kycDocumentRepository.save(existingDoc);
        } else {
            kycDocument = KycDocument.builder()
                    .userId(userId)
                    .tier(request.getTier())
                    .documents(documentEntries)
                    .status(STATUS_PENDING)
                    .submittedAt(OffsetDateTime.now())
                    .build();
            kycDocument = kycDocumentRepository.save(kycDocument);
        }

        saveAuditLog(userId, "KYC_SUBMITTED", null, "Documents submitted for tier: " + request.getTier());

        publishKycEvent("kyc.submitted", userId, STATUS_PENDING, request.getTier());

        log.info("KYC documents submitted successfully for user: {}", userId);
        return mapToResponse(kycDocument);
    }

    @Override
    @Transactional
    public KycStatusResponse reviewKyc(UUID userId, KycReviewRequest request, String reviewedBy) {
        log.info("Reviewing KYC for user: {} by {}", userId, reviewedBy);

        KycDocument kycDocument = kycDocumentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC Document", userId.toString()));

        String newStatus = request.getStatus().toUpperCase();
        if (!STATUS_VERIFIED.equals(newStatus) && !STATUS_REJECTED.equals(newStatus)) {
            throw new BusinessException("INVALID_STATUS", "Status must be either VERIFIED or REJECTED");
        }

        kycDocument.setStatus(newStatus);
        kycDocument.setReviewedAt(OffsetDateTime.now());

        for (DocumentEntry entry : kycDocument.getDocuments()) {
            entry.setVerified(STATUS_VERIFIED.equals(newStatus));
            entry.setVerifiedBy(UUID.fromString(reviewedBy));
            if (STATUS_REJECTED.equals(newStatus) && request.getNotes() != null) {
                entry.setRejectionReason(request.getNotes());
            }
        }

        KycDocument saved = kycDocumentRepository.save(kycDocument);

        String action = STATUS_VERIFIED.equals(newStatus) ? "KYC_VERIFIED" : "KYC_REJECTED";
        saveAuditLog(userId, action, reviewedBy, request.getNotes());

        publishKycEvent("kyc.reviewed", userId, newStatus, kycDocument.getTier());

        log.info("KYC {} for user {} by {}", newStatus, userId, reviewedBy);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(UUID userId) {
        KycDocument kycDocument = kycDocumentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC Document", userId.toString()));
        return mapToResponse(kycDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KycStatusResponse> getPendingRequests(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<KycDocument> pendingDocs = kycDocumentRepository.findByStatus(STATUS_PENDING);

        int start = Math.min(page * size, pendingDocs.size());
        int end = Math.min(start + size, pendingDocs.size());
        List<KycStatusResponse> content = pendingDocs.subList(start, end).stream()
                .map(this::mapToResponse)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(content, pageRequest, pendingDocs.size());
    }

    private void saveAuditLog(UUID userId, String action, String reviewedBy, String notes) {
        KycAudit audit = KycAudit.builder()
                .userId(userId)
                .action(action)
                .reviewedBy(reviewedBy)
                .notes(notes)
                .createdAt(OffsetDateTime.now())
                .build();
        kycAuditRepository.save(audit);
    }

    private void publishKycEvent(String topic, UUID userId, String status, String tier) {
        try {
            Map<String, Object> event = Map.of(
                    "userId", userId.toString(),
                    "status", status,
                    "tier", tier,
                    "timestamp", OffsetDateTime.now().toString()
            );
            kafkaTemplate.send(topic, userId.toString(), event);
            log.info("Published {} event for user {}", topic, userId);
        } catch (Exception e) {
            log.error("Failed to publish {} event for user {}: {}", topic, userId, e.getMessage());
        }
    }

    private KycStatusResponse mapToResponse(KycDocument doc) {
        return KycStatusResponse.builder()
                .userId(doc.getUserId())
                .tier(doc.getTier())
                .status(doc.getStatus())
                .submittedAt(doc.getSubmittedAt())
                .reviewedAt(doc.getReviewedAt())
                .build();
    }
}
