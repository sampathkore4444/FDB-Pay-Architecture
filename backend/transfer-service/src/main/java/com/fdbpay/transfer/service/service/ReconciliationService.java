package com.fdbpay.transfer.service.service;

import com.fdbpay.transfer.service.dto.response.ReconciliationRow;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReconciliationService {

    List<ReconciliationRow> reconcile(UUID walletId, UUID merchantId, LocalDate from, LocalDate to);

    String toCsv(List<ReconciliationRow> rows);
}
