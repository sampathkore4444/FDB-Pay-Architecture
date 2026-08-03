package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.enums.MerchantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    List<Merchant> findByUserId(UUID userId);

    Optional<Merchant> findByBusinessLicense(String businessLicense);

    Optional<Merchant> findByTaxId(String taxId);

    boolean existsByBusinessLicenseOrTaxId(String businessLicense, String taxId);

    boolean existsByBusinessLicense(String businessLicense);

    boolean existsByTaxId(String taxId);

    boolean existsByUserIdAndBusinessName(UUID userId, String businessName);

    Page<Merchant> findByStatusOrderByCreatedAtDesc(MerchantStatus status, Pageable pageable);

    Page<Merchant> findByCategoryContainingIgnoreCase(String category, Pageable pageable);

    Page<Merchant> findByBusinessNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String businessName, String category, Pageable pageable);

    @Query("SELECT m FROM Merchant m WHERE m.latitude BETWEEN :minLat AND :maxLat AND m.longitude BETWEEN :minLng AND :maxLng AND m.status = 'ACTIVE'")
    List<Merchant> findByLocationBounds(
            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLng") double minLng, @Param("maxLng") double maxLng);
}
