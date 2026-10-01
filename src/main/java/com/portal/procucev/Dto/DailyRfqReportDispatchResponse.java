package com.portal.procucev.Dto;

import com.portal.procucev.model.DailyRfqReportLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRfqReportDispatchResponse {

    private String status;
    private String message;
    private DailyRfqReportLog log;
    private String emailText;
}
