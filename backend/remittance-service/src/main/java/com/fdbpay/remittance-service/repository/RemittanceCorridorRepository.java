package com.fdbpay.remittance.service.repository;

import com.fdbpay.remittance.service.model.RemittanceCorridor;
import com.fdbpay.remittance.service.model.enums.RemittanceCorridorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RemittanceCorridorRepository extends JpaRepository<RemittanceCorridor, UUID> {

    Optional<RemittanceCorridor> findByCode(String code);

    List<RemittanceCorridor> findBySourceCountry(String sourceCountry);

    List<RemittanceCorridor> findByStatus(RemittanceCorridorStatus status);
}
