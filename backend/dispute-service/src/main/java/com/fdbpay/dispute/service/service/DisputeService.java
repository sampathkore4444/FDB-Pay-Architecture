package com.fdbpay.dispute.service.service;

import com.fdbpay.dispute.service.dto.request.AddEvidenceRequest;
import com.fdbpay.dispute.service.dto.request.CreateDisputeRequest;
import com.fdbpay.dispute.service.dto.request.ResolveDisputeRequest;
import com.fdbpay.dispute.service.dto.response.DisputeEvidenceResponse;
import com.fdbpay.dispute.service.dto.response.DisputeResponse;
import com.fdbpay.dispute.service.dto.response.DisputeStatsResponse;
import com.fdbpay.dispute.service.model.enums.DisputeStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface DisputeService {

    DisputeResponse createDispute(UUID userId, CreateDisputeRequest request);

    DisputeResponse getDispute(UUID disputeId);

    DisputeResponse updateDispute(UUID disputeId, CreateDisputeRequest request, UUID adminId);

    DisputeResponse resolveDispute(UUID disputeId, ResolveDisputeRequest request, UUID adminId);

    DisputeEvidenceResponse addEvidence(UUID disputeId, UUID userId, AddEvidenceRequest request);

    Page<DisputeResponse> getMyDisputes(UUID userId, int page, int size);

    Page<DisputeResponse> getAllDisputes(int page, int size, DisputeStatus status);

    DisputeStatsResponse getStats();

    List<DisputeEvidenceResponse> getDisputeEvidence(UUID disputeId);
}
