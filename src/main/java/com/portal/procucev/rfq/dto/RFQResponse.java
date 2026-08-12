package com.portal.procucev.rfq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RFQResponse {
    private String rfqNumber;
    private String status;
    private String buyerEmail;
    private String message;
    private LocalDateTime createdAt;
}
