package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.merchant.service.dto.response.MerchantDirectoryEntry;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.enums.MerchantStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.service.MerchantDirectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantDirectoryServiceImpl implements MerchantDirectoryService {

    private final MerchantRepository merchantRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public Page<MerchantDirectoryEntry> search(String query, String category,
                                                Double latitude, Double longitude,
                                                Double radius, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Merchant> merchants;

        if (query != null && !query.isBlank()) {
            merchants = merchantRepository.findByBusinessNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                    query, query, pageRequest);
        } else if (category != null && !category.isBlank()) {
            merchants = merchantRepository.findByCategoryContainingIgnoreCase(category, pageRequest);
        } else {
            merchants = merchantRepository.findByStatusOrderByCreatedAtDesc(MerchantStatus.ACTIVE, pageRequest);
        }

        if (latitude != null && longitude != null && radius != null) {
            merchants = merchants.map(m -> {
                if (m.getLatitude() != null && m.getLongitude() != null) {
                    double distance = haversine(latitude, longitude, m.getLatitude(), m.getLongitude());
                    m.setLatitude(distance);
                }
                return m;
            });
        }

        return merchants.map(this::mapToDirectoryEntry);
    }

    @Override
    public MerchantDirectoryEntry getMerchantDetails(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        return mapToDirectoryEntry(merchant);
    }

    @Override
    public Page<MerchantDirectoryEntry> getNearbyMerchants(Double latitude, Double longitude,
                                                             Double radius, int page, int size) {
        double latDelta = Math.toDegrees(radius / EARTH_RADIUS_KM);
        double lngDelta = Math.toDegrees(radius / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(latitude))));

        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLng = longitude - lngDelta;
        double maxLng = longitude + lngDelta;

        List<Merchant> nearby = merchantRepository.findByLocationBounds(minLat, maxLat, minLng, maxLng);

        List<MerchantDirectoryEntry> entries = nearby.stream()
                .filter(m -> {
                    double distance = haversine(latitude, longitude, m.getLatitude(), m.getLongitude());
                    return distance <= radius;
                })
                .map(this::mapToDirectoryEntry)
                .toList();

        int start = Math.min(page * size, entries.size());
        int end = Math.min(start + size, entries.size());
        List<MerchantDirectoryEntry> paged = entries.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(paged, PageRequest.of(page, size), entries.size());
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private MerchantDirectoryEntry mapToDirectoryEntry(Merchant merchant) {
        return MerchantDirectoryEntry.builder()
                .id(merchant.getId())
                .businessName(merchant.getBusinessName())
                .category(merchant.getCategory())
                .address(merchant.getAddress())
                .latitude(merchant.getLatitude())
                .longitude(merchant.getLongitude())
                .rating(4.5)
                .qrStaticUrl(merchant.getQrStaticUrl())
                .build();
    }
}
