package com.fdbpay.repositories;

import com.fdbpay.models.entity.KycDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends MongoRepository<KycDocument, String> {

    Optional<KycDocument> findByUserId(UUID userId);

    Optional<KycDocument> findByUserIdAndStatus(UUID userId, String status);
}
