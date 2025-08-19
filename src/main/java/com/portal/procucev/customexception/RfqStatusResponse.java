package com.portal.procucev.customexception;

import lombok.Data;

@Data
public class RfqStatusResponse {
    private String rfqid;
    private String status;
}