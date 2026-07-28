package com.fdbpay.support.service.repository;

import com.fdbpay.support.service.model.SupportTicket;
import com.fdbpay.support.service.model.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Page<SupportTicket> findByCorporateUserIdOrderByCreatedAtDesc(UUID corporateUserId, Pageable pageable);

    Page<SupportTicket> findByAssignedManagerIdOrderByCreatedAtDesc(UUID assignedManagerId, Pageable pageable);

    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = :status")
    long countByStatus(@Param("status") TicketStatus status);
}
