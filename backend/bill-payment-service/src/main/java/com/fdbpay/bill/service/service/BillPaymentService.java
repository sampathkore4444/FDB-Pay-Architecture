package com.fdbpay.bill.service.service;

import com.fdbpay.bill.service.dto.request.BillPaymentRequest;
import com.fdbpay.bill.service.dto.response.BillerResponse;
import com.fdbpay.bill.service.dto.response.BillLookupResponse;
import com.fdbpay.bill.service.dto.response.BillPaymentResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface BillPaymentService {

    List<BillerResponse> getCategories();

    List<BillerResponse> getBillers(String category);

    BillLookupResponse lookupBill(UUID billerId, String accountNumber);

    BillPaymentResponse payBill(UUID userId, BillPaymentRequest request);

    Page<BillPaymentResponse> getHistory(UUID userId, int page, int size);
}
