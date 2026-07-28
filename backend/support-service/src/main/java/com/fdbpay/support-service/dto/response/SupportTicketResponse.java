package com.fdbpay.support.service.dto.response;

import com.fdbpay.support.service.model.enums.TicketCategory;
import com.fdbpay.support.service.model.enums.TicketPriority;
import com.fdbpay.support.service.model.enums.TicketStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketResponse {

    private UUID id;
    private UUID corporateUserId;
    private String subject;
    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private UUID assignedManagerId;
    private Integer messageCount;
    private OffsetDateTime lastResponseAt;
    private OffsetDateTime slaDeadline;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
