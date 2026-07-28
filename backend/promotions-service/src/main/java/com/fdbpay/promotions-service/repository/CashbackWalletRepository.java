package com.fdbpay.promotions.service.repository;

import com.fdbpay.promotions.service.model.CashbackWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashbackWalletRepository extends JpaRepository<CashbackWallet, UUID> {

    Optional<CashbackWallet> findByUserId(UUID userId);
}
