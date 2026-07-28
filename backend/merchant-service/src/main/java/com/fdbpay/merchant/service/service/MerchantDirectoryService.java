package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.response.MerchantDirectoryEntry;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface MerchantDirectoryService {

    Page<MerchantDirectoryEntry> search(String query, String category,
                                         Double latitude, Double longitude,
                                         Double radius, int page, int size);

    MerchantDirectoryEntry getMerchantDetails(UUID merchantId);

    Page<MerchantDirectoryEntry> getNearbyMerchants(Double latitude, Double longitude,
                                                     Double radius, int page, int size);
}
