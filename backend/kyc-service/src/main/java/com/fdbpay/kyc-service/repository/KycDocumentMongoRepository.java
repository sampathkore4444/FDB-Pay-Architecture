package com.fdbpay.kyc.service.repository;

import com.fdbpay.kyc.service.model.KycDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycDocumentMongoRepository extends MongoRepository<KycDocument, String> {

    Optional<KycDocument> findByUserId(UUID userId);

    List<KycDocument> findByStatus(String status);
}
