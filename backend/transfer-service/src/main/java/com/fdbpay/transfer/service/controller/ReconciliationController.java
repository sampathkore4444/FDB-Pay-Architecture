package com.fdbpay.transfer.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.transfer.service.dto.response.ReconciliationRow;
import com.fdbpay.transfer.service.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transfer/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @GetMapping
    public ResponseEntity<?> reconcile(
            @RequestParam UUID walletId,
            @RequestParam UUID merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format) {

        List<ReconciliationRow> rows = reconciliationService.reconcile(walletId, merchantId, from, to);

        if ("csv".equalsIgnoreCase(format)) {
            byte[] csv = reconciliationService.toCsv(rows).getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reconciliation.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(csv.length)
                    .body(csv);
        }
        return ResponseEntity.ok(ApiResponse.success(rows));
    }
}
