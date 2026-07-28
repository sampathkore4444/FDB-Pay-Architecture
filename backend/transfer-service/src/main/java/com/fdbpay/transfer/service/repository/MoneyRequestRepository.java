package com.fdbpay.transfer.service.repository;

import com.fdbpay.transfer.service.model.MoneyRequest;
import com.fdbpay.transfer.service.model.enums.MoneyRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, UUID> {

    Page<MoneyRequest> findByRequesterUserIdOrderByCreatedAtDesc(UUID requesterUserId, Pageable pageable);

    Page<MoneyRequest> findByTargetPhoneOrderByCreatedAtDesc(String targetPhone, Pageable pageable);

    List<MoneyRequest> findByStatus(MoneyRequestStatus status);
}
