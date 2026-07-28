package com.fdbpay.support.service.repository;

import com.fdbpay.support.service.model.AccountManager;
import com.fdbpay.support.service.model.AccountManager.AccountManagerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountManagerRepository extends JpaRepository<AccountManager, UUID> {

    Optional<AccountManager> findByUserId(UUID userId);

    List<AccountManager> findByStatus(AccountManagerStatus status);

    @Query("SELECT am FROM AccountManager am WHERE am.status = :status AND am.currentClients < am.maxClients ORDER BY am.currentClients ASC LIMIT 1")
    Optional<AccountManager> findAvailableManager(@Param("status") AccountManagerStatus status);
}
