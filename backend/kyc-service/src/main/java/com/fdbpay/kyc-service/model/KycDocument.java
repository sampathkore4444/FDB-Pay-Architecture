package com.fdbpay.kyc.service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "kyc_documents")
public class KycDocument {

    @Id
    private String id;

    private UUID userId;

    private String tier;

    private List<DocumentEntry> documents;

    private String status;

    private OffsetDateTime submittedAt;

    private OffsetDateTime reviewedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentEntry {
        private String type;
        private String fileUrl;
        private OffsetDateTime uploadedAt;
        private Boolean verified;
        private UUID verifiedBy;
        private String rejectionReason;
    }
}
