package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.PosTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosTerminalRepository extends JpaRepository<PosTerminal, UUID> {

    List<PosTerminal> findByMerchantId(UUID merchantId);

    Optional<PosTerminal> findBySerialNumber(String serialNumber);
}
