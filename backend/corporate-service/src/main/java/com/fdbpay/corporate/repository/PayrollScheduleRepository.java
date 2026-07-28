package com.fdbpay.corporate.repository;

import com.fdbpay.corporate.model.PayrollSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollScheduleRepository extends JpaRepository<PayrollSchedule, UUID> {

    List<PayrollSchedule> findByCorporateUserIdOrderByScheduledDateDesc(UUID corporateUserId);
}
