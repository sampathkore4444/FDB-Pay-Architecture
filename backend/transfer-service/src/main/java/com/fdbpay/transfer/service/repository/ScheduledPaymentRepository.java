package com.fdbpay.transfer.service.repository;

import com.fdbpay.transfer.service.model.ScheduledPayment;
import com.fdbpay.transfer.service.model.enums.ScheduledPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, UUID> {

    Page<ScheduledPayment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<ScheduledPayment> findByStatusAndNextExecutionDateBefore(ScheduledPaymentStatus status, LocalDate date);

    List<ScheduledPayment> findByStatus(ScheduledPaymentStatus status);
}
