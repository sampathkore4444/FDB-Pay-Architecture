package com.fdbpay.wallet.service.repository;

import com.fdbpay.wallet.service.model.SavingsPocket;
import com.fdbpay.wallet.service.model.enums.PocketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavingsPocketRepository extends JpaRepository<SavingsPocket, UUID> {

    List<SavingsPocket> findByWalletId(UUID walletId);

    List<SavingsPocket> findByWalletIdAndStatus(UUID walletId, PocketStatus status);

    List<SavingsPocket> findByStatus(PocketStatus status);

    Optional<SavingsPocket> findByIdAndWalletId(UUID id, UUID walletId);
}
