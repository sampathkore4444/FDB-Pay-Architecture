package com.fdbpay.corporate.repository;

import com.fdbpay.corporate.model.PayrollEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollEmployeeRepository extends JpaRepository<PayrollEmployee, UUID> {

    List<PayrollEmployee> findByPayrollRunId(UUID payrollRunId);
}
