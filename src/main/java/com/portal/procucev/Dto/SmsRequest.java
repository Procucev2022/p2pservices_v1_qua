package com.portal.procucev.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsRequest {
	 private String username;
	 private String password;
    private boolean allowunicode = false;

    private List<SmsMessage> smstosend;
}
