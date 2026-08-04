package com.fdbpay.referencedata.repository;

import com.fdbpay.referencedata.model.ReferenceType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceTypeRepository extends JpaRepository<ReferenceType, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @EntityGraph(attributePaths = "values")
    Optional<ReferenceType> findWithValuesByCode(String code);

    Optional<ReferenceType> findByCode(String code);

    @Query("select v.type.id, count(v) from ReferenceValue v group by v.type.id")
    List<Object[]> countValuesGroupedByType();
}
