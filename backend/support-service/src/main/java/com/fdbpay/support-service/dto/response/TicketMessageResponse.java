package com.fdbpay.support.service.dto.response;

import com.fdbpay.support.service.model.enums.SenderType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessageResponse {

    private UUID id;
    private UUID ticketId;
    private UUID senderId;
    private SenderType senderType;
    private String message;
    private String attachments;
    private OffsetDateTime createdAt;
}
