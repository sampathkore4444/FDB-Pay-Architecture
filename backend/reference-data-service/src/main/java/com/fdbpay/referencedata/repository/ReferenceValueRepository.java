package com.fdbpay.referencedata.repository;

import com.fdbpay.referencedata.model.ReferenceValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReferenceValueRepository extends JpaRepository<ReferenceValue, UUID> {

    boolean existsByTypeIdAndCode(UUID typeId, String code);

    boolean existsByTypeIdAndCodeAndIdNot(UUID typeId, String code, UUID id);
}
