package com.fdbpay.services;

import com.fdbpay.schemas.request.BillPaymentRequest;
import com.fdbpay.schemas.response.TransactionResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BillPaymentService {

    List<Map<String, String>> getCategories();

    List<Map<String, String>> getBillers(String category);

    Map<String, Object> lookupBill(String billerId, String accountNumber);

    TransactionResponse payBill(UUID userId, BillPaymentRequest request);

    Map<String, Object> getBillPaymentHistory(UUID userId, int page, int size);
}
