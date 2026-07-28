package com.fdbpay.bill.service.repository;

import com.fdbpay.bill.service.model.AirtimeTopup;
import com.fdbpay.bill.service.model.enums.AirtimeProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AirtimeTopupRepository extends JpaRepository<AirtimeTopup, UUID> {

    Page<AirtimeTopup> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<AirtimeTopup> findByProvider(AirtimeProvider provider);
}
