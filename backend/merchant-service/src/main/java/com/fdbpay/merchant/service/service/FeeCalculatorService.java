package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.response.FeeCalculationResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeCalculatorService {

    private final MerchantRepository merchantRepository;

    public FeeCalculationResponse calculate(UUID merchantId, Long amount) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        double rate = 0.02;
        try {
            rate = merchant.getFeeSchedule() == null ? 0.02 : Double.parseDouble(merchant.getFeeSchedule());
        } catch (Exception e) {
            log.warn("Invalid fee schedule for merchant {}: {}", merchantId, merchant.getFeeSchedule());
        }
        double scale = merchant.getRollingReservePercent() == null ? 0 : merchant.getRollingReservePercent();
        long fee = Math.round(amount * rate);
        long net = amount - fee - Math.round(amount * scale / 100.0);
        return FeeCalculationResponse.builder()
                .amount(amount)
                .fee(fee)
                .net(net)
                .feeSchedule(merchant.getFeeSchedule() == null ? "2%" : merchant.getFeeSchedule())
                .feeRate(rate)
                .build();
    }
}
