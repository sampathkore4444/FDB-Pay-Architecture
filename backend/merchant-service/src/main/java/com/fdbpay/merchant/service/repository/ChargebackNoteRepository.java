package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.ChargebackNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChargebackNoteRepository extends JpaRepository<ChargebackNote, UUID> {

    List<ChargebackNote> findByChargebackIdOrderByCreatedAtAsc(UUID chargebackId);
}
