package com.fdbpay.repositories;

import com.fdbpay.models.entity.Wallet;
import com.fdbpay.models.enums.WalletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    List<Wallet> findByStatus(WalletStatus status);

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.status = 'ACTIVE'")
    Optional<Wallet> findActiveWalletByUserId(@Param("userId") UUID userId);

    boolean existsByUserId(UUID userId);
}
