package com.portal.procucev.rfq.exception;

import lombok.Getter;

@Getter
public class AttachmentSizeExceededException extends ApplicationException {

    private final String fileName;
    private final long fileSizeBytes;
    private final long maxAllowedBytes;

    public AttachmentSizeExceededException(String fileName, long fileSizeBytes, long maxAllowedBytes) {
        super("Attachment '" + fileName + "' (" + fileSizeBytes + " bytes) exceeds the configured size limit of " + maxAllowedBytes + " bytes.");
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.maxAllowedBytes = maxAllowedBytes;
    }
}
