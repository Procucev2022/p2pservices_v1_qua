package com.portal.procucev.customexception;

/**
 * Raised by the shared RFQ creation pipeline when a document attached to an RFQ is larger than the
 * configured per-file limit.
 *
 * <p>Lives in {@code customexception} rather than {@code rfq.exception} because the check now runs
 * in the single creation path used by both the web upload and the email-to-RFQ pipeline, and
 * {@code rfq.exception} is the email pipeline's own package.
 *
 * <p>Deliberately a {@code RuntimeException}: it is thrown from inside a {@code @Transactional}
 * creation call, and an unchecked exception is what marks that transaction for rollback so no
 * partially written RFQ is committed.
 */
public class RfqDocumentSizeExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final long fileSizeBytes;
    private final long maxAllowedBytes;

    public RfqDocumentSizeExceededException(String fileName, long fileSizeBytes, long maxAllowedBytes) {
        super("Document '" + fileName + "' (" + fileSizeBytes + " bytes) exceeds the maximum allowed RFQ file size of "
                + maxAllowedBytes + " bytes.");
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.maxAllowedBytes = maxAllowedBytes;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public long getMaxAllowedBytes() {
        return maxAllowedBytes;
    }
}
