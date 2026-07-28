package com.fdbpay.support.service.dto.request;

import com.fdbpay.support.service.model.enums.TicketCategory;
import com.fdbpay.support.service.model.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotNull(message = "Category is required")
    private TicketCategory category;

    @NotNull(message = "Priority is required")
    private TicketPriority priority;

    @NotBlank(message = "Message is required")
    private String message;
}
