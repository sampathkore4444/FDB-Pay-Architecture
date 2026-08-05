package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.merchant.service.dto.request.StoreRequest;
import com.fdbpay.merchant.service.dto.response.StoreResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.Store;
import com.fdbpay.merchant.service.model.enums.StoreStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.StoreRepository;
import com.fdbpay.merchant.service.service.StoreService;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final MerchantRepository merchantRepository;

    @Override
    public List<StoreResponse> getStoresByMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        return storeRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoreResponse createStore(UUID merchantId, StoreRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        Store store = Store.builder()
                .merchantId(merchant.getId())
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .status(StoreStatus.ACTIVE)
                .build();
        store = storeRepository.save(store);
        log.info("Store created: storeId={}, merchantId={}", store.getId(), merchantId);
        return mapToResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse updateStore(UUID merchantId, UUID storeId, StoreRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));
        if (!store.getMerchantId().equals(merchantId)) {
            throw new com.fdbpay.shared.exceptions.BusinessException(
                    com.fdbpay.shared.constants.ErrorCodes.UNAUTHORIZED, "Store does not belong to this merchant");
        }
        store.setName(request.getName());
        store.setAddress(request.getAddress());
        store.setCity(request.getCity());
        store.setPhone(request.getPhone());
        store = storeRepository.save(store);
        return mapToResponse(store);
    }

    private StoreResponse mapToResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .merchantId(store.getMerchantId())
                .name(store.getName())
                .address(store.getAddress())
                .city(store.getCity())
                .phone(store.getPhone())
                .status(store.getStatus())
                .createdAt(store.getCreatedAt())
                .build();
    }
}
