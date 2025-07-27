package com.portal.procucev.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmsMessage {
    private String to;           // recipient mobile number
    private String from;         // sender ID
    private String smstext;      // actual SMS text
    private String smsgid;       // optional internal message ID
    private String peid;         // optional
    private String templateid;   // optional
}
