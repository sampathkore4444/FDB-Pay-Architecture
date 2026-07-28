package com.fdbpay.support.service.dto.request;

import com.fdbpay.support.service.model.enums.TicketPriority;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalateRequest {

    private TicketPriority newPriority;

    private String reason;
}
