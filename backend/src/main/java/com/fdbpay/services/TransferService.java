package com.fdbpay.services;

import com.fdbpay.schemas.request.TransferRequest;
import com.fdbpay.schemas.response.TransactionResponse;
import com.fdbpay.models.enums.TransactionStatus;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface TransferService {

    TransactionResponse initiateTransfer(UUID senderUserId, TransferRequest request);

    TransactionResponse getTransferStatus(UUID transactionId);

    TransactionResponse confirmTransfer(UUID transactionId, String pin);

    void cancelTransfer(UUID transactionId);

    Map<String, Object> getTransferHistory(UUID userId, Pageable pageable);

    void processScheduledTransfers();
}
