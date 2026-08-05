package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.StoreRequest;
import com.fdbpay.merchant.service.dto.response.StoreResponse;

import java.util.List;
import java.util.UUID;

public interface StoreService {

    List<StoreResponse> getStoresByMerchant(UUID merchantId);

    StoreResponse createStore(UUID merchantId, StoreRequest request);

    StoreResponse updateStore(UUID merchantId, UUID storeId, StoreRequest request);
}
