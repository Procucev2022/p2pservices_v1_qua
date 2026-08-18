package com.portal.procucev.rfq.model;

/**
 * An image attachment carried into the extraction request as inline data.
 *
 * <p>Requirements are often sent as a screenshot or a photographed purchase note. Those files have
 * no text layer, so every text extractor returns nothing and the RFQ is rejected as detail-less.
 * Sending the image to the multimodal model alongside the prompt is what recovers them.
 *
 * @param fileName   the original attachment name, used only for logging
 * @param mimeType   the MIME type the model is told to interpret the bytes as
 * @param base64Data the file contents, Base64 encoded
 */
public record InlineImage(String fileName, String mimeType, String base64Data) {
}
