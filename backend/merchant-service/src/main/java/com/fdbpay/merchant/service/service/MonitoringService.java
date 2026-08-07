package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.client.TransferServiceClient;
import com.fdbpay.merchant.service.client.WalletServiceClient;
import com.fdbpay.merchant.service.dto.response.MonitoringResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.Refund;
import com.fdbpay.merchant.service.model.enums.RefundStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.RefundRepository;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final MerchantRepository merchantRepository;
    private final RefundRepository refundRepository;
    private final TransferServiceClient transferServiceClient;
    private final WalletServiceClient walletServiceClient;

    public MonitoringResponse monitor(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        UUID walletId = walletServiceClient.getWalletId(merchant.getUserId());
        List<Map<String, Object>> txns = walletId == null ? List.of() : transferServiceClient.getTransactions(walletId, 100);
        long recent = txns.size();
        long failed = txns.stream().filter(t -> {
            Object status = t.get("status");
            String s = status == null ? "" : status.toString();
            return s.equalsIgnoreCase("FAILED") || s.equalsIgnoreCase("REVERSED") || s.equalsIgnoreCase("CANCELLED");
        }).count();

        long pendingRefunds = refundRepository.findByMerchantIdAndStatus(merchantId, RefundStatus.PENDING).size();
        List<String> alerts = new ArrayList<>();
        if (pendingRefunds > 0) {
            alerts.add(pendingRefunds + " refund(s) awaiting approval");
        }
        if (failed > 0 && recent > 0 && (failed * 100.0 / recent) > 10) {
            alerts.add("Failure rate above 10% in the last " + recent + " transactions");
        }
        double anomalyScore = recent == 0 ? 0 : Math.round(failed * 100.0 / recent);
        if (failed > 0 && anomalyScore < 10) {
            alerts.add("Spike in failed transactions detected");
        }
        return MonitoringResponse.builder()
                .recentTransactions(recent)
                .failedTransactions(failed)
                .pendingRefunds(pendingRefunds)
                .anomalyScore(anomalyScore)
                .alerts(alerts)
                .build();
    }
}
