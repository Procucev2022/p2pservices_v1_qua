package com.portal.procucev.Dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRfqReportPreviewDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    private int webPortalCount;
    private int whatsappCount;
    private int emailCount;
    private int totalCount;

    private List<String> webPortalRfqIds;
    private List<String> whatsappRfqIds;
    private List<String> emailRfqIds;
    private List<String> rfqIds;

    private String emailText;
    private String subject;
    private String recipient;
}
