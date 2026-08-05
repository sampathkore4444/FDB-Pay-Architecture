package com.fdbpay.transfer.service.service;

import com.fdbpay.transfer.service.dto.request.TransferRequest;
import com.fdbpay.transfer.service.dto.request.ChargeRequest;
import com.fdbpay.transfer.service.dto.request.BulkRefundRequest;
import com.fdbpay.transfer.service.dto.request.BulkVoidRequest;
import com.fdbpay.transfer.service.dto.response.BulkOperationResponse;
import com.fdbpay.transfer.service.dto.response.TransactionResponse;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantStatement;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface TransferService {

    TransactionResponse initiateTransfer(UUID userId, TransferRequest request);

    TransactionResponse getTransferStatus(UUID transactionId);

    TransactionResponse confirmTransfer(UUID transactionId);

    TransactionResponse cancelTransfer(UUID transactionId);

    Page<TransactionResponse> getHistory(UUID userId, int page, int size);

    TransactionResponse charge(UUID merchantUserId, ChargeRequest request);

    BulkOperationResponse bulkRefund(UUID merchantUserId, BulkRefundRequest request);

    BulkOperationResponse bulkVoid(UUID merchantUserId, BulkVoidRequest request);

    MerchantStatement getStatement(UUID walletId, LocalDate from, LocalDate to,
                                   Integer rollingReservePercent, Integer rollingReservePeriodDays);
}
