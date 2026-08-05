package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.StaffAccount;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.StaffAccountRepository;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MerchantAccessHelper {

    private final MerchantRepository merchantRepository;
    private final StaffAccountRepository staffAccountRepository;

    public UUID resolveMerchantId(UUID userId) {
        return merchantRepository.findByUserId(userId).stream().findFirst()
                .map(Merchant::getId)
                .orElseGet(() -> staffAccountRepository.findAllByUserId(userId).stream().findFirst()
                        .map(StaffAccount::getMerchantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId=" + userId)));
    }
}
