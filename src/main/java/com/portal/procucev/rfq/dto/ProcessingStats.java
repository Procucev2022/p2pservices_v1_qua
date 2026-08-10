package com.portal.procucev.rfq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingStats {
    private String status;
    private int emailsProcessed;
    private int rfqsCreated;
    private int errors;
    private String executionTime;
}
