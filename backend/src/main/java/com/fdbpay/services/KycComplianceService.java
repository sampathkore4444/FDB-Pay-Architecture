package com.fdbpay.services;

import java.util.Map;
import java.util.UUID;

public interface KycComplianceService {

    void submitKycDocuments(UUID userId, Map<String, Object> documents);

    void reviewKyc(UUID userId, String status, String reviewNotes, UUID reviewedBy);

    Map<String, Object> getKycStatus(UUID userId);

    Map<String, Object> getPendingKycRequests(int page, int size);

    boolean isKycRequired(UUID userId, Long transactionAmount);
}
