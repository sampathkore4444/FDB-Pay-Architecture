package com.fdbpay.wallet.service.repository;

import com.fdbpay.wallet.service.model.Wallet;
import com.fdbpay.wallet.service.model.enums.WalletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByUserId(UUID userId);

    Optional<Wallet> findActiveWalletByUserIdAndStatus(UUID userId, WalletStatus status);

    boolean existsByUserId(UUID userId);
}
