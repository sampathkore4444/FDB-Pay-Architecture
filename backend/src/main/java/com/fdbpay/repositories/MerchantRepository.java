package com.fdbpay.repositories;

import com.fdbpay.models.entity.Merchant;
import com.fdbpay.models.enums.MerchantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByUserId(UUID userId);

    Optional<Merchant> findByBusinessLicense(String businessLicense);

    Page<Merchant> findByStatus(MerchantStatus status, Pageable pageable);

    Page<Merchant> findByCategory(String category, Pageable pageable);

    boolean existsByUserId(UUID userId);
}
