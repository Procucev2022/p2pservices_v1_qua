package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.InlineImage;
import com.portal.procucev.rfq.service.AIExtractionService;
import com.portal.procucev.rfq.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Cover for requirements that arrive as an image.
 *
 * <p>On 18-Aug-2026 two RFQs from purmis1@actiontesa.com failed with no quantity for any item. The
 * requirement was in a PNG screenshot, and every text extractor returned nothing, so the buyer was
 * asked for details that were sitting in the attachment. Images are now sent to the multimodal
 * model as inline data.
 */
public class AttachmentVisionExtractionTest {

    /** A minimal but structurally valid response so extraction completes. */
    private static final String OK_RESPONSE =
            "{\"buyerEmail\":\"purmis1@actiontesa.com\","
            + "\"items\":[{\"itemDescription\":\"DSP Network Analog Audio Camera\",\"quantity\":4,\"uom\":\"Nos\"}]}";

    @TempDir
    Path tempDir;

    private GeminiApiClient geminiApiClient;
    private AIExtractionService aiExtractionService;

    @BeforeEach
    void setUp() {
        geminiApiClient = Mockito.mock(GeminiApiClient.class);
        aiExtractionService = new AIExtractionService(geminiApiClient);
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(OK_RESPONSE);
    }

    private File writeFile(String name, byte[] content) throws Exception {
        File file = tempDir.resolve(name).toFile();
        Files.write(file.toPath(), content);
        return file;
    }

    private File writeImage(String name, int sizeBytes) throws Exception {
        byte[] content = new byte[sizeBytes];
        for (int i = 0; i < sizeBytes; i++) {
            content[i] = (byte) (i % 251);
        }
        return writeFile(name, content);
    }

    @SuppressWarnings("unchecked")
    private List<InlineImage> capturePassedImages() {
        ArgumentCaptor<List<InlineImage>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(geminiApiClient, Mockito.atLeastOnce()).generateContent(anyString(), captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------------
    // FileUtil: recognising an image and reading it for transport
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Image attachments are recognised and mapped to the MIME type the model accepts")
    void imageTypesAreRecognised() throws Exception {
        assertEquals("image/png", FileUtil.visionImageMimeType(writeFile("1_image1.png", new byte[]{1})));
        assertEquals("image/jpeg", FileUtil.visionImageMimeType(writeFile("scan.JPG", new byte[]{1})));
        assertEquals("image/jpeg", FileUtil.visionImageMimeType(writeFile("scan.jpeg", new byte[]{1})));
        assertEquals("image/webp", FileUtil.visionImageMimeType(writeFile("shot.webp", new byte[]{1})));
        assertEquals("image/heic", FileUtil.visionImageMimeType(writeFile("photo.heic", new byte[]{1})));
        assertEquals("image/heif", FileUtil.visionImageMimeType(writeFile("photo.heif", new byte[]{1})));
        assertTrue(FileUtil.isVisionImage(writeFile("indent.png", new byte[]{1})));
    }

    @Test
    @DisplayName("Non-image and undecidable file names are not sent as images")
    void nonImagesAreNotTreatedAsImages() throws Exception {
        assertNull(FileUtil.visionImageMimeType(writeFile("requirement.pdf", new byte[]{1})));
        assertNull(FileUtil.visionImageMimeType(writeFile("requirement.xlsx", new byte[]{1})));
        // Formats the API does not accept must stay unsupported rather than fail the whole request.
        assertNull(FileUtil.visionImageMimeType(writeFile("logo.gif", new byte[]{1})));
        assertNull(FileUtil.visionImageMimeType(writeFile("plan.tiff", new byte[]{1})));
        assertNull(FileUtil.visionImageMimeType(writeFile("noextension", new byte[]{1})));
        assertNull(FileUtil.visionImageMimeType(writeFile("trailingdot.", new byte[]{1})));
        assertNull(FileUtil.visionImageMimeType(null));
        assertFalse(FileUtil.isVisionImage(null));
    }

    @Test
    @DisplayName("An image yields no extracted text, which is expected rather than an extraction failure")
    void imageYieldsNoTextLayer() throws Exception {
        assertEquals("", FileUtil.extractTextFromFile(writeImage("1_image1.png", 64)));
    }

    @Test
    @DisplayName("An attachment is Base64 encoded byte for byte, and unreadable files yield null")
    void base64EncodingRoundTrips() throws Exception {
        byte[] original = "Quantity required: 4 Nos".getBytes(StandardCharsets.UTF_8);
        String encoded = FileUtil.readAsBase64(writeFile("note.png", original));
        assertNotNull(encoded);
        assertArrayEquals(original, Base64.getDecoder().decode(encoded));

        assertNull(FileUtil.readAsBase64(null));
        assertNull(FileUtil.readAsBase64(tempDir.resolve("does-not-exist.png").toFile()));
        // Exists but is not readable as bytes: the read must fail soft, not abort the extraction.
        assertNull(FileUtil.readAsBase64(makeDirectory("looks-like.png")));
    }

    @Test
    @DisplayName("A missing, empty or genuinely unsupported attachment extracts to nothing without throwing")
    void unreadableAttachmentsExtractToNothing() throws Exception {
        assertEquals("", FileUtil.extractTextFromFile(null));
        assertEquals("", FileUtil.extractTextFromFile(tempDir.resolve("absent.pdf").toFile()));
        // .gif is an image the API does not accept, so it stays reported as unsupported.
        assertEquals("", FileUtil.extractTextFromFile(writeFile("logo.gif", new byte[]{1, 2, 3})));
        assertEquals("", FileUtil.extractTextFromFile(writeFile("archive.zip", new byte[]{1, 2, 3})));
    }

    @Test
    @DisplayName("An image attachment that cannot be read is skipped rather than sent empty")
    void unreadableImageIsSkipped() throws Exception {
        EmailData email = EmailData.builder()
                .subject("Requirement")
                .senderEmail("buyer@test.com")
                .body("PFA")
                .attachments(List.of(makeDirectory("unreadable.png"), writeImage("good.png", 128)))
                .build();

        aiExtractionService.extractRFQFromEmail(email);

        List<InlineImage> sent = capturePassedImages();
        assertEquals(1, sent.size());
        assertEquals("good.png", sent.get(0).fileName());
    }

    private File makeDirectory(String name) {
        File dir = tempDir.resolve(name).toFile();
        assertTrue(dir.mkdirs() || dir.isDirectory());
        return dir;
    }

    // ---------------------------------------------------------------------
    // AIExtractionService: which attachments travel with the prompt
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("A screenshot-only requirement reaches the model as inline image data")
    void screenshotIsSentToTheModel() throws Exception {
        File screenshot = writeImage("1_image1.png", 2048);
        EmailData email = EmailData.builder()
                .subject("Material Required : DSP-NETWORK-CH-ANALOG-AUDIO-QSYS-CAMERA")
                .senderEmail("purmis1@actiontesa.com")
                .body("Dear Team, please quote for the attached requirement.")
                .attachmentText("")
                .attachments(List.of(screenshot))
                .build();

        ExtractedRFQ extracted = aiExtractionService.extractRFQFromEmail(email);
        assertNotNull(extracted);

        List<InlineImage> sent = capturePassedImages();
        assertEquals(1, sent.size());
        assertEquals("1_image1.png", sent.get(0).fileName());
        assertEquals("image/png", sent.get(0).mimeType());
        assertArrayEquals(Files.readAllBytes(screenshot.toPath()),
                Base64.getDecoder().decode(sent.get(0).base64Data()));
    }

    @Test
    @DisplayName("Non-image attachments and emails with no attachments send no inline data")
    void nonImageAttachmentsSendNothingInline() throws Exception {
        EmailData withPdf = EmailData.builder()
                .subject("Requirement")
                .senderEmail("buyer@test.com")
                .body("PFA")
                .attachments(List.of(writeFile("requirement.pdf", new byte[]{1, 2, 3})))
                .build();
        aiExtractionService.extractRFQFromEmail(withPdf);
        assertTrue(capturePassedImages().isEmpty());

        Mockito.reset(geminiApiClient);
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(OK_RESPONSE);

        EmailData noAttachments = EmailData.builder()
                .subject("Requirement")
                .senderEmail("buyer@test.com")
                .body("We require 10 laptops")
                .build();
        aiExtractionService.extractRFQFromEmail(noAttachments);
        assertTrue(capturePassedImages().isEmpty());

        Mockito.reset(geminiApiClient);
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(OK_RESPONSE);

        EmailData emptyAttachmentList = EmailData.builder()
                .subject("Requirement")
                .senderEmail("buyer@test.com")
                .body("We require 10 laptops")
                .attachments(List.of())
                .build();
        aiExtractionService.extractRFQFromEmail(emptyAttachmentList);
        assertTrue(capturePassedImages().isEmpty());
    }

    @Test
    @DisplayName("A missing API key is reported before any request is attempted")
    void missingApiKeyIsReported() {
        GeminiApiClient client = buildClient();
        ReflectionTestUtils.setField(client, "apiKey", "  ");

        assertThrows(ApplicationException.class,
                () -> client.generateContent("Extract the RFQ", List.of()));
        Mockito.verify(stubbedRestTemplate, Mockito.never())
                .postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("An oversized image is skipped so it cannot fail an extraction the body could satisfy")
    void oversizedImageIsSkipped() throws Exception {
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImageBytes", 512L);

        EmailData email = EmailData.builder()
                .subject("Requirement")
                .senderEmail("buyer@test.com")
                .body("PFA")
                .attachments(List.of(writeImage("huge.png", 4096), writeImage("small.png", 128)))
                .build();

        aiExtractionService.extractRFQFromEmail(email);

        List<InlineImage> sent = capturePassedImages();
        assertEquals(1, sent.size());
        assertEquals("small.png", sent.get(0).fileName());
    }

    @Test
    @DisplayName("The combined inline budget is respected")
    void totalInlineBudgetIsRespected() throws Exception {
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImageTotalBytes", 3000L);

        EmailData email = EmailData.builder()
                .subject("Requirement")
                .senderEmail("buyer@test.com")
                .body("PFA")
                .attachments(List.of(writeImage("page1.png", 2000), writeImage("page2.png", 2000)))
                .build();

        aiExtractionService.extractRFQFromEmail(email);

        List<InlineImage> sent = capturePassedImages();
        assertEquals(1, sent.size());
        assertEquals("page1.png", sent.get(0).fileName());
    }

    @Test
    @DisplayName("The image count is capped")
    void imageCountIsCapped() throws Exception {
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImages", 2);

        EmailData email = EmailData.builder()
                .subject("Requirement")
                .senderEmail("buyer@test.com")
                .body("PFA")
                .attachments(List.of(
                        writeImage("p1.png", 64),
                        writeImage("p2.png", 64),
                        writeImage("p3.png", 64)))
                .build();

        aiExtractionService.extractRFQFromEmail(email);
        assertEquals(2, capturePassedImages().size());
    }

    // ---------------------------------------------------------------------
    // GeminiApiClient: what actually goes on the wire
    // ---------------------------------------------------------------------

    private RestTemplate stubbedRestTemplate;

    private GeminiApiClient buildClient() {
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
        stubbedRestTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(builder.setConnectTimeout(any(Duration.class))).thenReturn(builder);
        Mockito.when(builder.setReadTimeout(any(Duration.class))).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(stubbedRestTemplate);

        GeminiApiClient client = new GeminiApiClient(builder, new ObjectMapper());
        ReflectionTestUtils.setField(client, "primaryModel", "gemini-3.5-flash-lite");
        ReflectionTestUtils.setField(client, "fallbackModel", "gemini-3.6-flash");
        ReflectionTestUtils.setField(client, "baseUrl", "https://example.test/models");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.invokeMethod(client, "init");

        Mockito.when(stubbedRestTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}", HttpStatus.OK));
        return client;
    }

    @Test
    @DisplayName("A null image list is treated as no images rather than failing the request")
    void nullImageListIsTreatedAsNone() {
        GeminiApiClient client = buildClient();

        client.generateContent("Extract the RFQ", null);

        String body = captureEntity(stubbedRestTemplate).getValue().getBody();
        assertNotNull(body);
        assertFalse(body.contains("inlineData"));
    }

    @Test
    @DisplayName("Inline images are serialised as inlineData parts, and are absent when none are supplied")
    void requestBodyCarriesInlineData() throws Exception {
        GeminiApiClient client = buildClient();
        RestTemplate restTemplate = stubbedRestTemplate;

        client.generateContent("Extract the RFQ",
                List.of(new InlineImage("1_image1.png", "image/png", "QUJD")));

        ArgumentCaptor<HttpEntity<String>> captor = captureEntity(restTemplate);
        String withImage = captor.getValue().getBody();
        assertNotNull(withImage);
        assertTrue(withImage.contains("\"inlineData\""), "the image must be sent as an inlineData part");
        assertTrue(withImage.contains("\"mimeType\":\"image/png\""));
        assertTrue(withImage.contains("\"data\":\"QUJD\""));
        assertTrue(withImage.contains("Extract the RFQ"), "the prompt must still be sent alongside the image");

        Mockito.reset(restTemplate);
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}", HttpStatus.OK));

        client.generateContent("Extract the RFQ");

        String textOnly = captureEntity(restTemplate).getValue().getBody();
        assertNotNull(textOnly);
        assertFalse(textOnly.contains("inlineData"), "a text-only email must send the request unchanged");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<HttpEntity<String>> captureEntity(RestTemplate restTemplate) {
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        Mockito.verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));
        return captor;
    }
}
