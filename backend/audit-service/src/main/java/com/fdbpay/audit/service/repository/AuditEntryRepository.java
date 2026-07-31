package com.fdbpay.audit.service.repository;

import com.fdbpay.audit.service.model.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {

    Page<AuditEntry> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    Page<AuditEntry> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(String resourceType, String resourceId, Pageable pageable);

    Page<AuditEntry> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    List<AuditEntry> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime start, OffsetDateTime end);

    Page<AuditEntry> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<AuditEntry> findByResourceTypeOrderByCreatedAtDesc(String resourceType, Pageable pageable);

    long countByAction(String action);

    long countByActorType(String actorType);

    @Query("SELECT a.action, COUNT(a) FROM AuditEntry a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.action")
    List<Object[]> countByActionBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT a.actorType, COUNT(a) FROM AuditEntry a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.actorType")
    List<Object[]> countByActorTypeBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT COUNT(DISTINCT a.actorId) FROM AuditEntry a WHERE a.createdAt BETWEEN :start AND :end")
    long countDistinctActorIdByCreatedAtBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}
