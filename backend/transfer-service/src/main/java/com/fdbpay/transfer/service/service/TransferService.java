package com.fdbpay.transfer.service.service;

import com.fdbpay.transfer.service.dto.request.TransferRequest;
import com.fdbpay.transfer.service.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface TransferService {

    TransactionResponse initiateTransfer(UUID userId, TransferRequest request);

    TransactionResponse getTransferStatus(UUID transactionId);

    TransactionResponse confirmTransfer(UUID transactionId);

    TransactionResponse cancelTransfer(UUID transactionId);

    Page<TransactionResponse> getHistory(UUID userId, int page, int size);
}
