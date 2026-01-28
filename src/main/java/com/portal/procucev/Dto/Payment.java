package com.portal.procucev.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment {

    private String payment_id;
    private String status;
    private String status_formatted;
    private String amount;
    private Long date;
}
