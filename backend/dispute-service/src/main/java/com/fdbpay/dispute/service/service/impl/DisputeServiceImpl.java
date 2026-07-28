package com.fdbpay.dispute.service.service.impl;

import com.fdbpay.dispute.service.dto.request.AddEvidenceRequest;
import com.fdbpay.dispute.service.dto.request.CreateDisputeRequest;
import com.fdbpay.dispute.service.dto.request.ResolveDisputeRequest;
import com.fdbpay.dispute.service.dto.response.DisputeEvidenceResponse;
import com.fdbpay.dispute.service.dto.response.DisputeResponse;
import com.fdbpay.dispute.service.dto.response.DisputeStatsResponse;
import com.fdbpay.dispute.service.model.Dispute;
import com.fdbpay.dispute.service.model.DisputeEvidence;
import com.fdbpay.dispute.service.model.enums.DisputeStatus;
import com.fdbpay.dispute.service.repository.DisputeEvidenceRepository;
import com.fdbpay.dispute.service.repository.DisputeRepository;
import com.fdbpay.dispute.service.service.DisputeService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository evidenceRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSFER_SERVICE_BASE = "http://transfer-service/transfer";

    @Override
    @Transactional
    public DisputeResponse createDispute(UUID userId, CreateDisputeRequest request) {
        verifyTransactionExists(request.getTransactionId());

        Map<String, Object> transaction = fetchTransaction(request.getTransactionId());
        UUID respondentUserId = extractUserId(transaction, userId);

        Dispute dispute = Dispute.builder()
                .transactionId(request.getTransactionId())
                .complainantUserId(userId)
                .respondentUserId(respondentUserId)
                .type(request.getType())
                .status(DisputeStatus.OPEN)
                .amount(request.getAmount())
                .description(request.getDescription())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        dispute = disputeRepository.save(dispute);
        log.info("Dispute created: disputeId={}, transactionId={}, userId={}, type={}",
                dispute.getId(), request.getTransactionId(), userId, request.getType());

        publishDisputeEvent(dispute, "dispute.created");

        return mapToResponse(dispute);
    }

    @Override
    public DisputeResponse getDispute(UUID disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId.toString()));
        return mapToResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse updateDispute(UUID disputeId, CreateDisputeRequest request, UUID adminId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId.toString()));

        if (request.getType() != null) {
            dispute.setType(request.getType());
        }
        if (request.getAmount() != null) {
            dispute.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            dispute.setDescription(request.getDescription());
        }
        dispute.setStatus(DisputeStatus.INVESTIGATING);
        dispute.setUpdatedAt(OffsetDateTime.now());

        dispute = disputeRepository.save(dispute);
        log.info("Dispute updated by admin: disputeId={}, adminId={}", disputeId, adminId);

        return mapToResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse resolveDispute(UUID disputeId, ResolveDisputeRequest request, UUID adminId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId.toString()));

        dispute.setStatus(request.getStatus());
        dispute.setResolution(request.getResolution());
        dispute.setResolvedBy(adminId);
        dispute.setResolvedAt(OffsetDateTime.now());
        dispute.setUpdatedAt(OffsetDateTime.now());

        dispute = disputeRepository.save(dispute);
        log.info("Dispute resolved: disputeId={}, status={}, adminId={}", disputeId, request.getStatus(), adminId);

        publishDisputeEvent(dispute, "dispute.resolved");

        return mapToResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeEvidenceResponse addEvidence(UUID disputeId, UUID userId, AddEvidenceRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId.toString()));

        DisputeEvidence evidence = DisputeEvidence.builder()
                .disputeId(disputeId)
                .uploadedBy(userId)
                .fileUrl(request.getFileUrl())
                .description(request.getDescription())
                .createdAt(OffsetDateTime.now())
                .build();

        evidence = evidenceRepository.save(evidence);

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            dispute.setStatus(DisputeStatus.EVIDENCE_REQUIRED);
        }
        dispute.setUpdatedAt(OffsetDateTime.now());
        disputeRepository.save(dispute);

        log.info("Evidence added: disputeId={}, evidenceId={}, userId={}", disputeId, evidence.getId(), userId);

        return mapToEvidenceResponse(evidence);
    }

    @Override
    public Page<DisputeResponse> getMyDisputes(UUID userId, int page, int size) {
        Page<Dispute> disputes = disputeRepository
                .findByComplainantUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return disputes.map(this::mapToResponse);
    }

    @Override
    public Page<DisputeResponse> getAllDisputes(int page, int size, DisputeStatus status) {
        Page<Dispute> disputes;
        if (status != null) {
            disputes = disputeRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
        } else {
            disputes = disputeRepository.findAllOrderByCreatedAtDesc(PageRequest.of(page, size));
        }
        return disputes.map(this::mapToResponse);
    }

    @Override
    public DisputeStatsResponse getStats() {
        long totalOpen = disputeRepository.countByStatus(DisputeStatus.OPEN)
                + disputeRepository.countByStatus(DisputeStatus.INVESTIGATING)
                + disputeRepository.countByStatus(DisputeStatus.EVIDENCE_REQUIRED);

        long totalResolved = disputeRepository.countByStatus(DisputeStatus.RESOLVED)
                + disputeRepository.countByStatus(DisputeStatus.CLOSED);

        Double avgHours = disputeRepository.avgResolutionHours();
        double avgResolutionHours = avgHours != null ? avgHours : 0.0;

        return DisputeStatsResponse.builder()
                .totalOpen(totalOpen)
                .totalResolved(totalResolved)
                .avgResolutionHours(avgResolutionHours)
                .build();
    }

    @Override
    public List<DisputeEvidenceResponse> getDisputeEvidence(UUID disputeId) {
        disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId.toString()));
        return evidenceRepository.findByDisputeIdOrderByCreatedAtAsc(disputeId)
                .stream()
                .map(this::mapToEvidenceResponse)
                .toList();
    }

    private void verifyTransactionExists(UUID transactionId) {
        try {
            webClientBuilder.build()
                    .get()
                    .uri(TRANSFER_SERVICE_BASE + "/{id}", transactionId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.TRANSACTION_NOT_FOUND,
                    "Transaction not found or transfer-service unavailable: " + transactionId);
        }
    }

    private Map<String, Object> fetchTransaction(UUID transactionId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(TRANSFER_SERVICE_BASE + "/{id}", transactionId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.containsKey("data")) {
                return (Map<String, Object>) response.get("data");
            }
            return Map.of();
        } catch (Exception e) {
            log.warn("Could not fetch transaction details: {}", transactionId);
            return Map.of();
        }
    }

    private UUID extractUserId(Map<String, Object> transaction, UUID excludeUserId) {
        if (transaction.containsKey("senderUserId")) {
            UUID senderId = UUID.fromString(transaction.get("senderUserId").toString());
            if (!senderId.equals(excludeUserId)) {
                return senderId;
            }
        }
        if (transaction.containsKey("receiverUserId")) {
            UUID receiverId = UUID.fromString(transaction.get("receiverUserId").toString());
            if (!receiverId.equals(excludeUserId)) {
                return receiverId;
            }
        }
        return excludeUserId;
    }

    private void publishDisputeEvent(Dispute dispute, String topic) {
        try {
            Map<String, Object> event = Map.of(
                    "disputeId", dispute.getId().toString(),
                    "transactionId", dispute.getTransactionId().toString(),
                    "complainantUserId", dispute.getComplainantUserId().toString(),
                    "respondentUserId", dispute.getRespondentUserId().toString(),
                    "type", dispute.getType().name(),
                    "status", dispute.getStatus().name(),
                    "amount", dispute.getAmount(),
                    "timestamp", OffsetDateTime.now().toString()
            );
            kafkaTemplate.send(topic, dispute.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish dispute event: disputeId={}, topic={}", dispute.getId(), topic, e);
        }
    }

    private DisputeResponse mapToResponse(Dispute dispute) {
        return DisputeResponse.builder()
                .id(dispute.getId())
                .transactionId(dispute.getTransactionId())
                .complainantUserId(dispute.getComplainantUserId())
                .respondentUserId(dispute.getRespondentUserId())
                .type(dispute.getType())
                .status(dispute.getStatus())
                .amount(dispute.getAmount())
                .description(dispute.getDescription())
                .resolution(dispute.getResolution())
                .resolvedBy(dispute.getResolvedBy())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .resolvedAt(dispute.getResolvedAt())
                .build();
    }

    private DisputeEvidenceResponse mapToEvidenceResponse(DisputeEvidence evidence) {
        return DisputeEvidenceResponse.builder()
                .id(evidence.getId())
                .fileUrl(evidence.getFileUrl())
                .description(evidence.getDescription())
                .uploadedBy(evidence.getUploadedBy())
                .createdAt(evidence.getCreatedAt())
                .build();
    }
}
