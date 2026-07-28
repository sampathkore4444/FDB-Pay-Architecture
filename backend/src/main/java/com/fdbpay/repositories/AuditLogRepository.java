package com.fdbpay.repositories;

import com.fdbpay.models.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(String resourceType, String resourceId, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
}
