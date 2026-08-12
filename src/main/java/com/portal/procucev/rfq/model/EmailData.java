package com.portal.procucev.rfq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailData {
    private String messageId;
    private String subject;
    private String senderEmail;
    private String senderName;
    private Date receivedDate;
    private String body;
    private List<File> attachments;
    private String attachmentText;
}
