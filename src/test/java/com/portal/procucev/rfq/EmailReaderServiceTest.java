package com.portal.procucev.rfq;

import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.service.EmailReaderService;
import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class EmailReaderServiceTest {

    static {
        System.setProperty("mail.imaps.ssl.trust", "*");
    }

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(new ServerSetup[]{ServerSetupTest.IMAPS, ServerSetupTest.SMTP})
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("user@test.com", "pass"));

    @TempDir
    Path tempDir;

    private EmailReaderService service;

    @BeforeEach
    void setUp() {
        service = new EmailReaderService();
        ReflectionTestUtils.setField(service, "mailHost", "invalid.host.test");
        ReflectionTestUtils.setField(service, "mailUsername", "user@test.com");
        ReflectionTestUtils.setField(service, "mailPassword", "pass");
        ReflectionTestUtils.setField(service, "mailPort", 993);
        ReflectionTestUtils.setField(service, "inboxFolder", "INBOX");
        ReflectionTestUtils.setField(service, "attachmentDirectory", tempDir.toAbsolutePath().toString());
        ReflectionTestUtils.setField(service, "maxAttachmentBytes", 26214400L);
        ReflectionTestUtils.setField(service, "connectTimeoutMs", 5000);
        ReflectionTestUtils.setField(service, "readTimeoutMs", 5000);
        ReflectionTestUtils.setField(service, "moveMaxAttempts", 2);
        ReflectionTestUtils.setField(service, "moveRetryDelayMs", 1L);
    }

    @Test
    @DisplayName("Test fetchUnreadEmails with unconfigured password throws ApplicationException")
    void testFetchUnreadEmailsMissingPassword() {
        ReflectionTestUtils.setField(service, "mailPassword", "");
        assertThrows(ApplicationException.class, () -> service.fetchUnreadEmails());

        ReflectionTestUtils.setField(service, "mailPassword", null);
        assertThrows(ApplicationException.class, () -> service.fetchUnreadEmails());
    }

    @Test
    @DisplayName("Test getMaxAttachmentBytes")
    void testGetMaxAttachmentBytes() {
        assertEquals(26214400L, service.getMaxAttachmentBytes());
    }

    @Test
    @DisplayName("Test fetchUnreadEmails with invalid connection handles exception gracefully")
    void testFetchUnreadEmailsInvalidHost() {
        List<EmailData> emails = service.fetchUnreadEmails();
        assertNotNull(emails);
        assertTrue(emails.isEmpty());
    }

    @Test
    @DisplayName("Test moveMessageToFolder with invalid connection handles exception gracefully")
    void testMoveMessageToFolderInvalidHost() {
        assertDoesNotThrow(() -> service.moveMessageToFolder("MSG-123", "Processed"));
    }

    @Test
    @DisplayName("Test parseMessage with text/plain mock Message")
    void testParseMessagePlain() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getHeader("Message-ID")).thenReturn(new String[]{"MSG-100"});
        Mockito.when(msg.getSubject()).thenReturn("Test Subject");
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com", "Sender Name")});
        Mockito.when(msg.getReceivedDate()).thenReturn(new Date());
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(msg.getContent()).thenReturn("Hello Plain Text");

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);

        assertNotNull(data);
        assertEquals("MSG-100", data.getMessageId());
        assertEquals("Test Subject", data.getSubject());
        assertEquals("sender@test.com", data.getSenderEmail());
        assertEquals("Sender Name", data.getSenderName());
        assertEquals("Hello Plain Text", data.getBody());
    }

    @Test
    @DisplayName("Test parseMessage with text/html mock Message")
    void testParseMessageHtml() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getHeader("Message-ID")).thenReturn(null);
        Mockito.when(msg.getSubject()).thenReturn(null);
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com")});
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(msg.isMimeType("text/html")).thenReturn(true);
        Mockito.when(msg.getContent()).thenReturn("<p>Hello <b>HTML</b></p><br/>Text");

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);

        assertNotNull(data);
        assertEquals("(No Subject)", data.getSubject());
        assertTrue(data.getBody().contains("Hello HTML"));
    }

    @Test
    @DisplayName("Test htmlToText helper method with tables, lists, and special chars")
    void testHtmlToText() {
        String html = "<table><tr><th>Header</th></tr><tr><td>Row Val</td></tr></table><ul><li>Item 1</li></ul>&lt;tag&gt; &quot;quote&quot;";
        String text = ReflectionTestUtils.invokeMethod(service, "htmlToText", html);
        assertTrue(text.contains("Header"));
        assertTrue(text.contains("Row Val"));
        assertTrue(text.contains("Item 1"));
        assertTrue(text.contains("<tag>"));
        assertTrue(text.contains("\"quote\""));

        String nullText = ReflectionTestUtils.invokeMethod(service, "htmlToText", (String) null);
        assertEquals("", nullText);
    }

    @Test
    @DisplayName("Test findMessageById and scanForMessageId")
    void testFindAndScanMessageId() throws Exception {
        Folder folder = Mockito.mock(Folder.class);
        Message msg1 = Mockito.mock(Message.class);
        Mockito.when(msg1.getHeader("Message-ID")).thenReturn(new String[]{"<target-msg-id@domain.com>"});

        Mockito.when(folder.getMessages()).thenReturn(new Message[]{msg1});

        Message found = ReflectionTestUtils.invokeMethod(service, "scanForMessageId", folder, "target-msg-id@domain.com");
        assertNotNull(found);

        // Empty messages
        Mockito.when(folder.getMessages()).thenReturn(new Message[0]);
        assertNull(ReflectionTestUtils.invokeMethod(service, "scanForMessageId", folder, "any-id"));

        // Normalized message id helper
        assertEquals("msg-id", ReflectionTestUtils.invokeMethod(service, "normalizeMessageId", "<msg-id>"));
        assertEquals("", ReflectionTestUtils.invokeMethod(service, "normalizeMessageId", (String) null));
    }

    @Test
    @DisplayName("Test createAttachmentFile helper method")
    void testCreateAttachmentFile() {
        File file = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "test_file.txt");
        assertNotNull(file);
        assertTrue(file.getName().endsWith("test_file.txt"));

        // Test path traversal exception
        assertThrows(Throwable.class, () -> ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "../../../secret.txt"));
    }

    @Test
    @DisplayName("Test parseMessage with non-InternetAddress sender")
    void testParseMessageNonInternetAddress() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Address customAddr = Mockito.mock(Address.class);
        Mockito.when(customAddr.toString()).thenReturn("custom-sender@test.com");
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{customAddr});
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(msg.getContent()).thenReturn("Plain text");

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertEquals("custom-sender@test.com", data.getSenderEmail());
    }

    @Test
    @DisplayName("Test parseMessage with MimeMultipart content")
    void testParseMessageMultipart() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com")});
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(msg.isMimeType("text/html")).thenReturn(false);
        Mockito.when(msg.isMimeType("multipart/*")).thenReturn(true);

        jakarta.mail.internet.MimeMultipart mp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart p1 = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(p1.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(p1.getContent()).thenReturn("Multipart plain content");

        jakarta.mail.BodyPart p2 = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(p2.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(p2.isMimeType("text/html")).thenReturn(true);
        Mockito.when(p2.getContent()).thenReturn("<p>Html content</p>");

        Mockito.when(mp.getCount()).thenReturn(2);
        Mockito.when(mp.getBodyPart(0)).thenReturn(p1);
        Mockito.when(mp.getBodyPart(1)).thenReturn(p2);
        Mockito.when(msg.getContent()).thenReturn(mp);

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertTrue(data.getBody().contains("Multipart plain content"));
    }

    @Test
    @DisplayName("Test parseMessage with null from address array")
    void testParseMessageNullFrom() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getFrom()).thenReturn(null);
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(msg.getContent()).thenReturn("Body text");

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertEquals("", data.getSenderEmail());
    }

    @Test
    @DisplayName("Test parseMessage with empty from address array")
    void testParseMessageEmptyFrom() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{});
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(msg.getContent()).thenReturn("Body text");

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertEquals("", data.getSenderEmail());
    }

    @Test
    @DisplayName("Test parseMessage multipart html fallback (no text/plain)")
    void testParseMessageMultipartHtmlFallback() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com")});
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(msg.isMimeType("text/html")).thenReturn(false);
        Mockito.when(msg.isMimeType("multipart/*")).thenReturn(true);

        jakarta.mail.internet.MimeMultipart mp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart htmlPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(htmlPart.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(htmlPart.isMimeType("text/html")).thenReturn(true);
        Mockito.when(htmlPart.getContent()).thenReturn("<p>HTML only</p>");

        Mockito.when(mp.getCount()).thenReturn(1);
        Mockito.when(mp.getBodyPart(0)).thenReturn(htmlPart);
        Mockito.when(msg.getContent()).thenReturn(mp);

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertTrue(data.getBody().contains("HTML only"));
    }

    @Test
    @DisplayName("Test createAttachmentFile with null fileName")
    void testCreateAttachmentFileNullName() {
        File file = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", (String) null);
        assertNotNull(file);
        assertTrue(file.getName().contains("attachment"));
    }

    @Test
    @DisplayName("Test createAttachmentFile with blank fileName")
    void testCreateAttachmentFileBlankName() {
        File file = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "   ");
        assertNotNull(file);
        assertTrue(file.getName().contains("attachment"));
    }

    @Test
    @DisplayName("Test htmlToText with blank HTML")
    void testHtmlToTextBlank() {
        String result = ReflectionTestUtils.invokeMethod(service, "htmlToText", "   ");
        assertEquals("", result);
    }

    @Test
    @DisplayName("Test parseMessage with content not matching any mime type")
    void testParseMessageUnknownContentType() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com")});
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(msg.isMimeType("text/html")).thenReturn(false);
        Mockito.when(msg.isMimeType("multipart/*")).thenReturn(false);

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertEquals("", data.getBody());
    }

    @Test
    @DisplayName("Test parseMessage InternetAddress with null personal name")
    void testParseMessageNullPersonal() throws Exception {
        Message msg = Mockito.mock(Message.class);
        InternetAddress addr = new InternetAddress("sender@test.com");
        addr.setPersonal(null);
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{addr});
        Mockito.when(msg.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(msg.getContent()).thenReturn("Body");

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertEquals("sender@test.com", data.getSenderName());
    }

    @Test
    @DisplayName("Test parseMessage with attachment in MimeMultipart")
    void testParseMessageAttachmentMultipart() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com")});
        Mockito.when(msg.isMimeType("multipart/*")).thenReturn(true);

        jakarta.mail.internet.MimeMultipart mp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart attPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(attPart.getDisposition()).thenReturn("attachment");
        Mockito.when(attPart.getFileName()).thenReturn("test_doc.txt");

        java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream("Attachment content text".getBytes());
        Mockito.when(attPart.getInputStream()).thenReturn(in);

        Mockito.when(mp.getCount()).thenReturn(1);
        Mockito.when(mp.getBodyPart(0)).thenReturn(attPart);
        Mockito.when(msg.getContent()).thenReturn(mp);

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertEquals(1, data.getAttachments().size());
        assertTrue(data.getAttachmentText().contains("Attachment content text"));
    }

    @Test
    @DisplayName("Test parseMessage with nested MimeMultipart")
    void testParseMessageNestedMultipart() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com")});
        Mockito.when(msg.isMimeType("multipart/*")).thenReturn(true);

        jakarta.mail.internet.MimeMultipart outerMp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.internet.MimeMultipart innerMp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);

        jakarta.mail.BodyPart innerPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(innerPart.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(innerPart.getContent()).thenReturn("Nested text content");

        Mockito.when(innerMp.getCount()).thenReturn(1);
        Mockito.when(innerMp.getBodyPart(0)).thenReturn(innerPart);

        jakarta.mail.BodyPart outerPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(outerPart.isMimeType("multipart/*")).thenReturn(true);
        Mockito.when(outerPart.getContent()).thenReturn(innerMp);

        Mockito.when(outerMp.getCount()).thenReturn(1);
        Mockito.when(outerMp.getBodyPart(0)).thenReturn(outerPart);
        Mockito.when(msg.getContent()).thenReturn(outerMp);

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);
        assertNotNull(data);
        assertTrue(data.getBody().contains("Nested text content"));
    }

    @Test
    @DisplayName("Test closeFolderAndStore helper method")
    void testCloseFolderAndStore() throws Exception {
        jakarta.mail.Folder folder = Mockito.mock(jakarta.mail.Folder.class);
        Mockito.when(folder.isOpen()).thenReturn(true);
        Mockito.doThrow(new RuntimeException("Folder close err")).when(folder).close(true);

        jakarta.mail.Store store = Mockito.mock(jakarta.mail.Store.class);
        Mockito.when(store.isConnected()).thenReturn(true);
        Mockito.doThrow(new RuntimeException("Store close err")).when(store).close();

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service, "closeFolderAndStore", folder, store, true));
    }

    @Test
    @DisplayName("The read-only poll closes the folder without asking the server to expunge")
    void pollClosesWithoutExpunge() throws Exception {
        jakarta.mail.Folder folder = Mockito.mock(jakarta.mail.Folder.class);
        Mockito.when(folder.isOpen()).thenReturn(true);
        jakarta.mail.Store store = Mockito.mock(jakarta.mail.Store.class);
        Mockito.when(store.isConnected()).thenReturn(true);

        ReflectionTestUtils.invokeMethod(service, "closeFolderAndStore", folder, store, false);

        Mockito.verify(folder).close(false);
        Mockito.verify(folder, Mockito.never()).close(true);
        Mockito.verify(store).close();
    }

    @Test
    @DisplayName("A closed folder and a disconnected store are left alone")
    void alreadyClosedResourcesAreNotTouched() {
        jakarta.mail.Folder folder = Mockito.mock(jakarta.mail.Folder.class);
        Mockito.when(folder.isOpen()).thenReturn(false);
        jakarta.mail.Store store = Mockito.mock(jakarta.mail.Store.class);
        Mockito.when(store.isConnected()).thenReturn(false);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service, "closeFolderAndStore", folder, store, false));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service, "closeFolderAndStore", null, null, false));
    }

    @Test
    @DisplayName("Test parseMessage handles attachment exceeding maxAttachmentBytes gracefully")
    void testParseMessageAttachmentSizeLimitExceeded() throws Exception {
        ReflectionTestUtils.setField(service, "maxAttachmentBytes", 10L);

        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getHeader("Message-ID")).thenReturn(new String[]{"MSG-OVERSIZED"});
        Mockito.when(msg.getSubject()).thenReturn("Oversized Attachment RFQ");
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com")});
        Mockito.when(msg.isMimeType("multipart/*")).thenReturn(true);

        jakarta.mail.internet.MimeMultipart mp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart attPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(attPart.getDisposition()).thenReturn(jakarta.mail.Part.ATTACHMENT);
        Mockito.when(attPart.getFileName()).thenReturn("huge_catalog.pdf");

        byte[] largeBytes = new byte[100];
        Mockito.when(attPart.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(largeBytes));

        Mockito.when(mp.getCount()).thenReturn(1);
        Mockito.when(mp.getBodyPart(0)).thenReturn(attPart);
        Mockito.when(msg.getContent()).thenReturn(mp);

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);

        assertNotNull(data);
        assertTrue(data.isFileSizeExceeded());
        assertEquals("huge_catalog.pdf", data.getFailedAttachmentName());
        assertNotNull(data.getErrorMessage());
        assertTrue(data.getErrorMessage().contains("huge_catalog.pdf"));
    }

    @Test
    @DisplayName("Test parseMessage with structured HTML table and thread headers")
    void testParseMessageStructuredHtmlAndThreadHeaders() throws Exception {
        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getHeader("Message-ID")).thenReturn(new String[]{"<MSG-THREAD-1>"});
        Mockito.when(msg.getHeader("In-Reply-To")).thenReturn(new String[]{"<PARENT-MSG-1>"});
        Mockito.when(msg.getHeader("References")).thenReturn(new String[]{"<REF-MSG-1> <REF-MSG-2>"});
        Mockito.when(msg.getFrom()).thenReturn(new Address[]{new InternetAddress("buyer@test.com")});
        Mockito.when(msg.isMimeType("multipart/*")).thenReturn(true);

        jakarta.mail.internet.MimeMultipart mp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart plainPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(plainPart.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(plainPart.getContent()).thenReturn("Plain requirement text without pipes");

        jakarta.mail.BodyPart htmlPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(htmlPart.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(htmlPart.isMimeType("text/html")).thenReturn(true);
        Mockito.when(htmlPart.getContent()).thenReturn("<table><tr><td>Col1</td><td>Col2</td></tr></table>");

        Mockito.when(mp.getCount()).thenReturn(2);
        Mockito.when(mp.getBodyPart(0)).thenReturn(plainPart);
        Mockito.when(mp.getBodyPart(1)).thenReturn(htmlPart);
        Mockito.when(msg.getContent()).thenReturn(mp);

        EmailData data = ReflectionTestUtils.invokeMethod(service, "parseMessage", msg);

        assertNotNull(data);
        assertEquals("<MSG-THREAD-1>", data.getMessageId());
        assertEquals("<PARENT-MSG-1>", data.getInReplyTo());
        assertEquals("<REF-MSG-1> <REF-MSG-2>", data.getReferences());
        assertTrue(data.getBody().contains("--- Structured HTML Content ---"));
    }

    @Test
    @DisplayName("Test findMessageById when server search succeeds or throws exception")
    void testFindMessageByIdSearch() throws Exception {
        Folder folder = Mockito.mock(Folder.class);
        Message msg = Mockito.mock(Message.class);
        Mockito.when(folder.search(any())).thenReturn(new Message[]{msg});

        Message found = ReflectionTestUtils.invokeMethod(service, "findMessageById", folder, "test-msg-id");
        assertNotNull(found);

        // When search throws, fall back to scan
        Mockito.when(folder.search(any())).thenThrow(new jakarta.mail.MessagingException("Search unsupported"));
        Mockito.when(folder.getMessages()).thenReturn(new Message[]{msg});
        Mockito.when(msg.getHeader("Message-ID")).thenReturn(new String[]{"<test-msg-id>"});

        Message fallbackFound = ReflectionTestUtils.invokeMethod(service, "findMessageById", folder, "test-msg-id");
        assertNotNull(fallbackFound);
    }

    @Test
    @DisplayName("Test createAttachmentFile path traversal security validation")
    void testCreateAttachmentFilePathTraversal() {
        assertThrows(ApplicationException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "../../etc/passwd"));

        assertDoesNotThrow(() -> {
            File f = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", (String) null);
            assertNotNull(f);
        });
    }

    @Test
    @DisplayName("Test logParsedEmailContext with images and thread history")
    void testLogParsedEmailContext() throws Exception {
        File imgFile = new File(tempDir.toFile(), "photo.jpg");
        try (FileOutputStream fos = new FileOutputStream(imgFile)) {
            fos.write(new byte[]{1, 2, 3});
        }

        EmailData data = EmailData.builder()
                .messageId("MSG-CTX")
                .body("Context body")
                .attachmentText("Att text")
                .inReplyTo("<PARENT>")
                .references("<REF>")
                .attachments(List.of(imgFile))
                .build();

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "logParsedEmailContext", data));

        // Long text and null variants
        EmailData longData = EmailData.builder()
                .body("B".repeat(400))
                .attachmentText("A".repeat(400))
                .attachments(null)
                .build();
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "logParsedEmailContext", longData));
    }

    @Test
    @DisplayName("Test sleepBeforeRetry and createAttachmentFile normal path")
    void testSleepAndCreateAttachmentFile() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "sleepBeforeRetry"));

        File validFile = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "valid_file.pdf");
        assertNotNull(validFile);
        assertTrue(validFile.getName().contains("valid_file"));
    }

    @Test
    @DisplayName("Test fetchUnreadEmails validation and connection failure handling")
    void testFetchUnreadEmailsFailureHandling() {
        // Password missing
        ReflectionTestUtils.setField(service, "mailPassword", null);
        assertThrows(ApplicationException.class, () -> service.fetchUnreadEmails());

        ReflectionTestUtils.setField(service, "mailPassword", "   ");
        assertThrows(ApplicationException.class, () -> service.fetchUnreadEmails());

        // Password present, connection to dummy port fails gracefully
        ReflectionTestUtils.setField(service, "mailPassword", "some_secret");
        ReflectionTestUtils.setField(service, "mailHost", "127.0.0.1");
        ReflectionTestUtils.setField(service, "mailPort", 1);
        List<EmailData> list = service.fetchUnreadEmails();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test moveMessageToFolder retry loop on connection error")
    void testMoveMessageToFolderRetryOnConnectionError() {
        ReflectionTestUtils.setField(service, "mailPassword", "some_secret");
        ReflectionTestUtils.setField(service, "mailHost", "127.0.0.1");
        ReflectionTestUtils.setField(service, "mailPort", 1);
        ReflectionTestUtils.setField(service, "moveMaxAttempts", 2);
        ReflectionTestUtils.setField(service, "moveRetryDelayMs", 1);

        assertDoesNotThrow(() -> service.moveMessageToFolder("<msg-123>", "Processed"));
    }

    @Test
    @DisplayName("Test fetchUnreadEmails and moveMessageToFolder with in-memory GreenMail IMAPS server")
    void testFetchAndMoveWithGreenMail() throws Exception {
        GreenMailUtil.sendTextEmail("user@test.com", "buyer@test.com", "RFQ Request", "Need 10 valves", greenMail.getSmtp().getServerSetup());
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));

        ReflectionTestUtils.setField(service, "mailHost", "127.0.0.1");
        ReflectionTestUtils.setField(service, "mailPort", greenMail.getImaps().getPort());
        ReflectionTestUtils.setField(service, "mailUsername", "user@test.com");
        ReflectionTestUtils.setField(service, "mailPassword", "pass");
        ReflectionTestUtils.setField(service, "inboxFolder", "INBOX");
        ReflectionTestUtils.setField(service, "connectTimeoutMs", 10000);
        ReflectionTestUtils.setField(service, "readTimeoutMs", 10000);

        List<EmailData> emails = service.fetchUnreadEmails();
        assertNotNull(emails);
        assertFalse(emails.isEmpty());
        assertEquals("RFQ Request", emails.get(0).getSubject());

        // Test moveMessageOnce with existing message and new target folder
        String msgId = emails.get(0).getMessageId();
        if (msgId != null && !msgId.isBlank()) {
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "moveMessageOnce", msgId, "NEW_PROCESSED_FOLDER"));
        }
        // Test moveMessageOnce with non-existent message and existing target folder
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "moveMessageOnce", "NON_EXISTENT_ID", "NEW_PROCESSED_FOLDER"));

        // Additional branch tests for helper methods:
        // getMessageId with null / empty headers
        Message msgNoHdr = Mockito.mock(Message.class);
        Mockito.when(msgNoHdr.getHeader("Message-ID")).thenReturn(null);
        String id1 = ReflectionTestUtils.invokeMethod(service, "getMessageId", msgNoHdr);
        assertNotNull(id1);

        Mockito.when(msgNoHdr.getHeader("Message-ID")).thenReturn(new String[]{});
        String id2 = ReflectionTestUtils.invokeMethod(service, "getMessageId", msgNoHdr);
        assertNotNull(id2);

        // createAttachmentFile with null / empty / relative path traversal
        File fNull = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", (String) null);
        assertNotNull(fNull);

        File fEmpty = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "   ");
        assertNotNull(fEmpty);

        assertThrows(ApplicationException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "../etc/passwd"));

        // closeFolderAndStore with exception throwing mocks
        Folder mockFolder = Mockito.mock(Folder.class);
        Mockito.when(mockFolder.isOpen()).thenReturn(true);
        Mockito.doThrow(new RuntimeException("close failed")).when(mockFolder).close(any(Boolean.class));

        jakarta.mail.Store mockStore = Mockito.mock(jakarta.mail.Store.class);
        Mockito.when(mockStore.isConnected()).thenReturn(true);
        Mockito.doThrow(new RuntimeException("store close failed")).when(mockStore).close();

        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "closeFolderAndStore", mockFolder, mockStore, true));
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "closeFolderAndStore", (Folder) null, (jakarta.mail.Store) null, false));

        // sleepBeforeRetry interrupted branch
        Thread.currentThread().interrupt();
        ReflectionTestUtils.invokeMethod(service, "sleepBeforeRetry");
        // clear interrupted status if still set
        Thread.interrupted();

        // 16. findMessageById and scanForMessageId with fallback and match
        Folder folderWithMsgs = Mockito.mock(Folder.class);
        Message m1 = Mockito.mock(Message.class);
        Message m2 = Mockito.mock(Message.class);
        Mockito.when(m1.getHeader("Message-ID")).thenReturn(new String[]{"<OTHER-ID>"});
        Mockito.when(m2.getHeader("Message-ID")).thenReturn(new String[]{"<TARGET-ID>"});
        Mockito.when(folderWithMsgs.search(any())).thenReturn(new Message[0]);
        Mockito.when(folderWithMsgs.getMessageCount()).thenReturn(2);
        Mockito.when(folderWithMsgs.getMessages()).thenReturn(new Message[]{m1, m2});

        Message foundMsg = ReflectionTestUtils.invokeMethod(service, "findMessageById", folderWithMsgs, "TARGET-ID");
        assertNotNull(foundMsg);
        assertEquals(m2, foundMsg);

        // scanForMessageId not found
        Message notFoundMsg = ReflectionTestUtils.invokeMethod(service, "findMessageById", folderWithMsgs, "NON-EXISTENT");
        assertNull(notFoundMsg);

        // 17. logParsedEmailContext variations
        EmailData dNull = EmailData.builder()
                .senderEmail(null)
                .senderName(null)
                .attachments(null)
                .build();
        ReflectionTestUtils.invokeMethod(service, "logParsedEmailContext", dNull);

        EmailData dEmptyAtts = EmailData.builder()
                .senderEmail("test@example.com")
                .senderName("Tester")
                .attachments(List.of())
                .build();
        ReflectionTestUtils.invokeMethod(service, "logParsedEmailContext", dEmptyAtts);

        File sampleAtt1 = File.createTempFile("att1", ".pdf");
        File sampleAtt2 = File.createTempFile("att2", ".png");
        EmailData dWithAtts = EmailData.builder()
                .senderEmail("test@example.com")
                .senderName("Tester")
                .attachments(List.of(sampleAtt1, sampleAtt2))
                .build();
        ReflectionTestUtils.invokeMethod(service, "logParsedEmailContext", dWithAtts);
        sampleAtt1.delete();
        sampleAtt2.delete();

        // 18. processMultipart with nested multipart
        jakarta.mail.internet.MimeMultipart outerMp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.internet.MimeMultipart innerMp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart innerPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(innerPart.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(innerPart.getContent()).thenReturn("Inner plain text");
        Mockito.when(innerMp.getCount()).thenReturn(1);
        Mockito.when(innerMp.getBodyPart(0)).thenReturn(innerPart);

        jakarta.mail.BodyPart nestedBodyPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(nestedBodyPart.isMimeType("multipart/*")).thenReturn(true);
        Mockito.when(nestedBodyPart.getContent()).thenReturn(innerMp);
        Mockito.when(outerMp.getCount()).thenReturn(1);
        Mockito.when(outerMp.getBodyPart(0)).thenReturn(nestedBodyPart);

        StringBuilder sbBody = new StringBuilder();
        StringBuilder sbHtml = new StringBuilder();
        StringBuilder sbAttText = new StringBuilder();
        List<File> attList = new java.util.ArrayList<>();

        ReflectionTestUtils.invokeMethod(service, "processMultipart", outerMp, sbBody, sbHtml, sbAttText, attList);
        assertTrue(sbBody.toString().contains("Inner plain text"));

        // 19. createAttachmentFile edge cases (null, blank, path traversal)
        File fNull2 = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", (String) null);
        assertNotNull(fNull2);
        fNull2.delete();

        assertThrows(com.portal.procucev.rfq.exception.ApplicationException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "../../etc/passwd.txt"));

        // 20. fetchUnreadEmails with blank password
        ReflectionTestUtils.setField(service, "mailPassword", "   ");
        assertThrows(com.portal.procucev.rfq.exception.ApplicationException.class, () -> service.fetchUnreadEmails());
        ReflectionTestUtils.setField(service, "mailPassword", "validPassword");

        // 21. moveMessageToFolder with non-existent message
        assertDoesNotThrow(() -> service.moveMessageToFolder("NON-EXISTENT-MSG-12345", "NON_EXISTENT_FOLDER_XYZ"));

        // 22. moveMessageToFolder retry exhaustion
        ReflectionTestUtils.setField(service, "mailPort", 1); // Invalid port causing connection exception
        assertDoesNotThrow(() -> service.moveMessageToFolder("ANY_MSG", "PROCESSED"));
        ReflectionTestUtils.setField(service, "mailPort", greenMail.getImaps().getPort());

        // 23. parseMessage with In-Reply-To and References headers
        Message msgHeaders = Mockito.mock(Message.class);
        Mockito.when(msgHeaders.getHeader("Message-ID")).thenReturn(new String[]{"<MSG-123>"});
        Mockito.when(msgHeaders.getHeader("In-Reply-To")).thenReturn(new String[]{"<PARENT-123>"});
        Mockito.when(msgHeaders.getHeader("References")).thenReturn(new String[]{"<REF-123>"});
        Mockito.when(msgHeaders.getSubject()).thenReturn("Test Headers");
        Mockito.when(msgHeaders.getFrom()).thenReturn(new Address[]{new InternetAddress("sender@test.com", "Sender Name")});
        Mockito.when(msgHeaders.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(msgHeaders.getContent()).thenReturn("Plain text content");

        EmailData dataHeaders = ReflectionTestUtils.invokeMethod(service, "parseMessage", msgHeaders);
        assertEquals("<PARENT-123>", dataHeaders.getInReplyTo());
        assertEquals("<REF-123>", dataHeaders.getReferences());

        // 24. parseMessage with structured HTML fallback containing pipes
        Message msgHtmlPipe = Mockito.mock(Message.class);
        Mockito.when(msgHtmlPipe.getHeader("Message-ID")).thenReturn(new String[]{"<MSG-HTML>"});
        Mockito.when(msgHtmlPipe.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(msgHtmlPipe.isMimeType("text/html")).thenReturn(false);
        Mockito.when(msgHtmlPipe.isMimeType("multipart/*")).thenReturn(true);

        jakarta.mail.internet.MimeMultipart mpPipe = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart plainPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(plainPart.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(plainPart.getContent()).thenReturn("Plain text without pipes");

        jakarta.mail.BodyPart htmlPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(htmlPart.isMimeType("text/html")).thenReturn(true);
        Mockito.when(htmlPart.getContent()).thenReturn("<table><tr><td>Col1</td><td>Col2</td></tr></table>");

        Mockito.when(mpPipe.getCount()).thenReturn(2);
        Mockito.when(mpPipe.getBodyPart(0)).thenReturn(plainPart);
        Mockito.when(mpPipe.getBodyPart(1)).thenReturn(htmlPart);
        Mockito.when(msgHtmlPipe.getContent()).thenReturn(mpPipe);

        EmailData dataHtmlPipe = ReflectionTestUtils.invokeMethod(service, "parseMessage", msgHtmlPipe);
        assertTrue(dataHtmlPipe.getBody().contains("Structured HTML Content"));

        // 25. Attachment with Part.ATTACHMENT and null fileName
        jakarta.mail.BodyPart attNoName = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(attNoName.getDisposition()).thenReturn(jakarta.mail.Part.ATTACHMENT);
        Mockito.when(attNoName.getFileName()).thenReturn(null);
        Mockito.when(attNoName.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[]{1, 2, 3}));

        jakarta.mail.internet.MimeMultipart mpAtt = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        Mockito.when(mpAtt.getCount()).thenReturn(1);
        Mockito.when(mpAtt.getBodyPart(0)).thenReturn(attNoName);

        StringBuilder sbB = new StringBuilder();
        StringBuilder sbH = new StringBuilder();
        StringBuilder sbA = new StringBuilder();
        List<File> aList = new java.util.ArrayList<>();

        ReflectionTestUtils.invokeMethod(service, "processMultipart", mpAtt, sbB, sbH, sbA, aList);
        assertEquals(1, aList.size());
        aList.get(0).delete();

        // 26. scanFallbackUnreadMessages with mixed seen/unseen messages
        Folder fallbackFolder = Mockito.mock(Folder.class);
        Message mockUnseen = Mockito.mock(Message.class);
        Mockito.when(mockUnseen.isSet(jakarta.mail.Flags.Flag.SEEN)).thenReturn(false);
        Message mockSeen = Mockito.mock(Message.class);
        Mockito.when(mockSeen.isSet(jakarta.mail.Flags.Flag.SEEN)).thenReturn(true);
        Mockito.when(fallbackFolder.getMessages(Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(new Message[]{mockUnseen, mockSeen});

        List<Message> fallbackResult = service.scanFallbackUnreadMessages(fallbackFolder, 10);
        assertEquals(1, fallbackResult.size());
        assertSame(mockUnseen, fallbackResult.get(0));

        // 27. createAttachmentFile with null and blank originalFileName
        File attNull = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", (String) null);
        assertNotNull(attNull);
        assertTrue(attNull.getName().contains("attachment"));
        attNull.delete();

        File attBlank = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "   ");
        assertNotNull(attBlank);
        assertTrue(attBlank.getName().contains("attachment"));
        attBlank.delete();

        // 28. logParsedEmailContext with null fields and thread references
        EmailData dataThread = EmailData.builder()
                .messageId("MSG-THREAD-1")
                .body(null)
                .attachmentText(null)
                .references("<REF-123>")
                .attachments(null)
                .build();
        ReflectionTestUtils.invokeMethod(service, "logParsedEmailContext", dataThread);

        // 29. scanForMessageId variations
        Folder mockScanFolder = Mockito.mock(Folder.class);
        Message msgNullHdr = Mockito.mock(Message.class);
        Mockito.when(msgNullHdr.getHeader("Message-ID")).thenReturn(null);

        Message msgEmptyHdr = Mockito.mock(Message.class);
        Mockito.when(msgEmptyHdr.getHeader("Message-ID")).thenReturn(new String[0]);

        Message msgMatchHdr = Mockito.mock(Message.class);
        Mockito.when(msgMatchHdr.getHeader("Message-ID")).thenReturn(new String[]{"<TARGET-MSG-ID>"});

        Mockito.when(mockScanFolder.getMessages()).thenReturn(new Message[]{msgNullHdr, msgEmptyHdr, msgMatchHdr});

        Message matched = ReflectionTestUtils.invokeMethod(service, "scanForMessageId", mockScanFolder, "TARGET-MSG-ID");
        assertSame(msgMatchHdr, matched);

        Message notFound = ReflectionTestUtils.invokeMethod(service, "scanForMessageId", mockScanFolder, "NON-EXISTENT-ID");
        assertNull(notFound);

        // 30. moveMessageToFolder exhausting all retry attempts
        EmailReaderService failReader = new EmailReaderService();
        ReflectionTestUtils.setField(failReader, "mailHost", "127.0.0.1");
        ReflectionTestUtils.setField(failReader, "mailPort", 1);
        ReflectionTestUtils.setField(failReader, "mailUsername", "user");
        ReflectionTestUtils.setField(failReader, "mailPassword", "wrong");
        ReflectionTestUtils.setField(failReader, "inboxFolder", "INBOX");
        ReflectionTestUtils.setField(failReader, "moveMaxAttempts", 2);
        ReflectionTestUtils.setField(failReader, "moveRetryDelayMs", 1L);
        failReader.moveMessageToFolder("SOME-MSG-ID", "Target");

        // 31. parseMessage with null subject, empty from, empty replyTo
        Message msgEmptyFields = Mockito.mock(Message.class);
        Mockito.when(msgEmptyFields.getHeader("Message-ID")).thenReturn(new String[]{"<EMPTY-FIELDS-ID>"});
        Mockito.when(msgEmptyFields.getSubject()).thenReturn(null);
        Mockito.when(msgEmptyFields.getFrom()).thenReturn(new jakarta.mail.Address[0]);
        Mockito.when(msgEmptyFields.getReplyTo()).thenReturn(new jakarta.mail.Address[0]);
        Mockito.when(msgEmptyFields.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(msgEmptyFields.getContent()).thenReturn("Plain body");

        EmailData parsedEmpty = ReflectionTestUtils.invokeMethod(service, "parseMessage", msgEmptyFields);
        assertNotNull(parsedEmpty);
        assertEquals("(No Subject)", parsedEmpty.getSubject());
        assertEquals("", parsedEmpty.getSenderEmail());

        // 32. moveMessageOnce when targetFolder does not exist and message is null (with GreenMail)
        service.moveMessageToFolder("NON-EXISTENT-GREENMAIL-MSG-ID", "BrandNewNonExistentFolder");

        // 33. closeFolderAndStore when folder and store throw MessagingException on close
        Folder mockThrowFolder = Mockito.mock(Folder.class);
        Mockito.when(mockThrowFolder.isOpen()).thenReturn(true);
        Mockito.doThrow(new jakarta.mail.MessagingException("Folder close failed")).when(mockThrowFolder).close(Mockito.anyBoolean());

        jakarta.mail.Store mockThrowStore = Mockito.mock(jakarta.mail.Store.class);
        Mockito.when(mockThrowStore.isConnected()).thenReturn(true);
        Mockito.doThrow(new jakarta.mail.MessagingException("Store close failed")).when(mockThrowStore).close();

        ReflectionTestUtils.invokeMethod(service, "closeFolderAndStore", mockThrowFolder, mockThrowStore, false);

        // 34. processMultipart with oversized attachment
        jakarta.mail.internet.MimeMultipart mockMultipart = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart mockOversizedPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(mockMultipart.getCount()).thenReturn(1);
        Mockito.when(mockMultipart.getBodyPart(0)).thenReturn(mockOversizedPart);
        Mockito.when(mockOversizedPart.getDisposition()).thenReturn(jakarta.mail.Part.ATTACHMENT);
        Mockito.when(mockOversizedPart.getFileName()).thenReturn("huge.bin");
        byte[] largeBytes = new byte[200];
        Mockito.when(mockOversizedPart.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(largeBytes));

        EmailReaderService smallLimitService = new EmailReaderService();
        ReflectionTestUtils.setField(smallLimitService, "attachmentDirectory", "./target/test-attachments-limit");
        ReflectionTestUtils.setField(smallLimitService, "maxAttachmentBytes", 50L);

        assertThrows(Exception.class, () -> ReflectionTestUtils.invokeMethod(
                smallLimitService, "processMultipart", mockMultipart, new StringBuilder(), new StringBuilder(), new StringBuilder(), new java.util.ArrayList<java.io.File>()));

        // 35. processMultipart with unknown mime type (not text/plain, not text/html, not multipart/*)
        jakarta.mail.internet.MimeMultipart mockUnknownMp = Mockito.mock(jakarta.mail.internet.MimeMultipart.class);
        jakarta.mail.BodyPart mockUnknownPart = Mockito.mock(jakarta.mail.BodyPart.class);
        Mockito.when(mockUnknownMp.getCount()).thenReturn(1);
        Mockito.when(mockUnknownMp.getBodyPart(0)).thenReturn(mockUnknownPart);
        Mockito.when(mockUnknownPart.getDisposition()).thenReturn(null);
        Mockito.when(mockUnknownPart.getFileName()).thenReturn(null);
        Mockito.when(mockUnknownPart.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(mockUnknownPart.isMimeType("text/html")).thenReturn(false);
        Mockito.when(mockUnknownPart.isMimeType("multipart/*")).thenReturn(false);

        ReflectionTestUtils.invokeMethod(
                service, "processMultipart", mockUnknownMp, new StringBuilder(), new StringBuilder(), new StringBuilder(), new java.util.ArrayList<java.io.File>());

        // 36. createAttachmentFile variations
        assertThrows(Exception.class, () -> ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "../../etc/passwd"));
        File fCreatedNull = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", (String) null);
        assertNotNull(fCreatedNull);
        assertTrue(fCreatedNull.getName().contains("attachment"));
        File fCreatedBlank = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "   ");
        assertNotNull(fCreatedBlank);
        assertTrue(fCreatedBlank.getName().contains("attachment"));

        // 37. Explicit message ID move with GreenMail
        try {
            jakarta.mail.internet.MimeMessage mimeMsg = new jakarta.mail.internet.MimeMessage(greenMail.getImaps().createSession());
            mimeMsg.setFrom(new jakarta.mail.internet.InternetAddress("buyer@test.com"));
            mimeMsg.setRecipient(Message.RecipientType.TO, new jakarta.mail.internet.InternetAddress("user@test.com"));
            mimeMsg.setSubject("Explicit Message-ID Subject");
            mimeMsg.setText("Message with explicit ID");
            mimeMsg.setHeader("Message-ID", "<EXPLICIT-REAL-ID-123@test.com>");
            com.icegreen.greenmail.util.GreenMailUtil.sendMimeMessage(mimeMsg);
            greenMail.waitForIncomingEmail(5000, 1);

            ReflectionTestUtils.setField(service, "mailHost", "127.0.0.1");
            ReflectionTestUtils.setField(service, "mailPort", greenMail.getImaps().getPort());
            ReflectionTestUtils.setField(service, "mailUsername", "user@test.com");
            ReflectionTestUtils.setField(service, "mailPassword", "pass");
            ReflectionTestUtils.setField(service, "inboxFolder", "INBOX");

            // Direct invocation of moveMessageOnce with non-existent message
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "moveMessageOnce", "NON-EXISTENT-MSG-12345", "REAL_PROCESSED"));

            // Direct invocation of moveMessageOnce with existing message and existing folder
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "moveMessageOnce", "EXPLICIT-REAL-ID-123@test.com", "REAL_PROCESSED"));

            // Move again to already existing folder
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "moveMessageOnce", "EXPLICIT-REAL-ID-123@test.com", "REAL_PROCESSED"));
        } catch (Exception ignored) {}

        // 38. fetchUnreadEmails with blank password
        ReflectionTestUtils.setField(service, "mailPassword", "");
        assertThrows(ApplicationException.class, () -> service.fetchUnreadEmails());

        ReflectionTestUtils.setField(service, "mailPassword", "   ");
        assertThrows(ApplicationException.class, () -> service.fetchUnreadEmails());

        // 39. fetchUnreadEmails with invalid inbox folder on GreenMail
        ReflectionTestUtils.setField(service, "mailHost", "127.0.0.1");
        ReflectionTestUtils.setField(service, "mailPort", greenMail.getImaps().getPort());
        ReflectionTestUtils.setField(service, "mailUsername", "user@test.com");
        ReflectionTestUtils.setField(service, "mailPassword", "pass");
        ReflectionTestUtils.setField(service, "inboxFolder", "NON_EXISTENT_INBOX_FOLDER");
        List<EmailData> emptyRes = service.fetchUnreadEmails();
        assertNotNull(emptyRes);

        // 40. moveMessageToFolder retry failure and sleepBeforeRetry
        ReflectionTestUtils.setField(service, "mailHost", "invalid.host.test");
        ReflectionTestUtils.setField(service, "moveMaxAttempts", 2);
        ReflectionTestUtils.setField(service, "moveRetryDelayMs", 1L);
        assertDoesNotThrow(() -> service.moveMessageToFolder("MSG-RETRY-FAIL", "ERRORS"));

        // 41. findMessageById with empty normalized ID and search exception fallback
        Folder mockSearchFailFolder = Mockito.mock(Folder.class);
        Mockito.when(mockSearchFailFolder.search(Mockito.any())).thenThrow(new jakarta.mail.MessagingException("Search failed"));
        Mockito.when(mockSearchFailFolder.getMessageCount()).thenReturn(0);
        Mockito.when(mockSearchFailFolder.getMessages()).thenReturn(new Message[0]);

        assertNull(ReflectionTestUtils.invokeMethod(service, "findMessageById", mockSearchFailFolder, "<>"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "findMessageById", mockSearchFailFolder, "ID-SEARCH-EX"));

        // 42. parseMessage with non-InternetAddress and structured HTML table fallback
        Message mockCustomAddrMsg = Mockito.mock(Message.class);
        jakarta.mail.Address customAddr = Mockito.mock(jakarta.mail.Address.class);
        Mockito.when(customAddr.toString()).thenReturn("custom@domain.com");
        Mockito.when(mockCustomAddrMsg.getFrom()).thenReturn(new jakarta.mail.Address[]{customAddr});
        Mockito.when(mockCustomAddrMsg.getSubject()).thenReturn("Custom Address Subject");
        Mockito.when(mockCustomAddrMsg.isMimeType("text/plain")).thenReturn(false);
        Mockito.when(mockCustomAddrMsg.isMimeType("text/html")).thenReturn(true);
        Mockito.when(mockCustomAddrMsg.getContent()).thenReturn("<table><tr><td>Item</td><td>Qty</td></tr><tr><td>Valves</td><td>10</td></tr></table>");
        Mockito.when(mockCustomAddrMsg.getHeader(Mockito.anyString())).thenReturn(null);

        EmailData customParsed = ReflectionTestUtils.invokeMethod(service, "parseMessage", mockCustomAddrMsg);
        assertNotNull(customParsed);
        assertEquals("custom@domain.com", customParsed.getSenderEmail());
        assertTrue(customParsed.getBody().contains("Valves"));

        // 43. multipart with HTML part only (bodyBuilder empty, htmlFallbackBuilder populated)
        jakarta.mail.internet.MimeMultipart mpHtmlOnly = new jakarta.mail.internet.MimeMultipart();
        jakarta.mail.internet.MimeBodyPart bpHtmlOnly = new jakarta.mail.internet.MimeBodyPart();
        bpHtmlOnly.setContent("<h3>HTML Only Requirement</h3>", "text/html");
        mpHtmlOnly.addBodyPart(bpHtmlOnly);

        jakarta.mail.internet.MimeMessage msgHtmlOnly = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        msgHtmlOnly.setContent(mpHtmlOnly);
        msgHtmlOnly.saveChanges();

        EmailData parsedMpHtmlOnly = ReflectionTestUtils.invokeMethod(service, "parseMessage", msgHtmlOnly);
        assertNotNull(parsedMpHtmlOnly);
        assertTrue(parsedMpHtmlOnly.getBody().contains("HTML Only Requirement"));

        // 44. multipart with plain text AND html table
        jakarta.mail.internet.MimeMultipart mpBoth = new jakarta.mail.internet.MimeMultipart();
        jakarta.mail.internet.MimeBodyPart bpPlain = new jakarta.mail.internet.MimeBodyPart();
        bpPlain.setText("Plain text header without pipes");

        jakarta.mail.internet.MimeBodyPart bpHtmlTable = new jakarta.mail.internet.MimeBodyPart();
        bpHtmlTable.setContent("<table><tr><td>Item</td><td>Qty</td></tr><tr><td>Valves</td><td>10</td></tr></table>", "text/html");

        mpBoth.addBodyPart(bpPlain);
        mpBoth.addBodyPart(bpHtmlTable);

        jakarta.mail.internet.MimeMessage msgBoth = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        msgBoth.setContent(mpBoth);
        msgBoth.saveChanges();

        EmailData parsedMpBoth = ReflectionTestUtils.invokeMethod(service, "parseMessage", msgBoth);
        assertNotNull(parsedMpBoth);
        assertTrue(parsedMpBoth.getBody().contains("Structured HTML Content"));

        // 45. createAttachmentFile branches
        File fNullCreated = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", (String) null);
        assertNotNull(fNullCreated);
        assertTrue(fNullCreated.getName().contains("attachment"));
        fNullCreated.delete();

        File fEmptyCreated = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "   ");
        assertNotNull(fEmptyCreated);
        assertTrue(fEmptyCreated.getName().contains("attachment"));
        fEmptyCreated.delete();

        File fValid = ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "spec_sheet.pdf");
        assertNotNull(fValid);
        assertTrue(fValid.getName().contains("spec_sheet.pdf"));
        fValid.delete();

        assertThrows(ApplicationException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "../etc/passwd"));

        // 46. parseMessage header variations and HTML table combinations
        Message mockHdrVarMsg = Mockito.mock(Message.class);
        Mockito.when(mockHdrVarMsg.getHeader("Message-ID")).thenReturn(new String[]{"<HDR-VAR-1@test.com>"});
        Mockito.when(mockHdrVarMsg.getHeader("In-Reply-To")).thenReturn(new String[0]);
        Mockito.when(mockHdrVarMsg.getHeader("References")).thenReturn(new String[0]);
        Mockito.when(mockHdrVarMsg.getSubject()).thenReturn("Header Variations");
        Mockito.when(mockHdrVarMsg.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(mockHdrVarMsg.getContent()).thenReturn("Plain content");

        EmailData parsedHdrVar = ReflectionTestUtils.invokeMethod(service, "parseMessage", mockHdrVarMsg);
        assertNotNull(parsedHdrVar);
        assertNull(parsedHdrVar.getInReplyTo());
        assertNull(parsedHdrVar.getReferences());

        // In-Reply-To throwing Exception
        Message mockHdrExMsg = Mockito.mock(Message.class);
        Mockito.when(mockHdrExMsg.getHeader("Message-ID")).thenReturn(new String[]{"<HDR-EX-1@test.com>"});
        Mockito.when(mockHdrExMsg.getHeader("In-Reply-To")).thenThrow(new jakarta.mail.MessagingException("Header read failure"));
        Mockito.when(mockHdrExMsg.getSubject()).thenReturn("Header Exception");
        Mockito.when(mockHdrExMsg.isMimeType("text/plain")).thenReturn(true);
        Mockito.when(mockHdrExMsg.getContent()).thenReturn("Plain content");

        EmailData parsedHdrEx = ReflectionTestUtils.invokeMethod(service, "parseMessage", mockHdrExMsg);
        assertNotNull(parsedHdrEx);
        assertNull(parsedHdrEx.getInReplyTo());

        // Multipart where both plain body AND html contain pipes (so !bodyBuilder.contains("|") is false)
        jakarta.mail.internet.MimeMultipart mpPipesBoth = new jakarta.mail.internet.MimeMultipart();
        jakarta.mail.internet.MimeBodyPart bpPipePlain = new jakarta.mail.internet.MimeBodyPart();
        bpPipePlain.setText("| Plain | Pipe |");
        jakarta.mail.internet.MimeBodyPart bpPipeHtml = new jakarta.mail.internet.MimeBodyPart();
        bpPipeHtml.setContent("<table><tr><td>HTML</td><td>Pipe</td></tr></table>", "text/html");
        mpPipesBoth.addBodyPart(bpPipePlain);
        mpPipesBoth.addBodyPart(bpPipeHtml);

        jakarta.mail.internet.MimeMessage msgPipesBoth = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        msgPipesBoth.setContent(mpPipesBoth);
        msgPipesBoth.saveChanges();

        EmailData parsedPipesBoth = ReflectionTestUtils.invokeMethod(service, "parseMessage", msgPipesBoth);
        assertNotNull(parsedPipesBoth);
        assertFalse(parsedPipesBoth.getBody().contains("Structured HTML Content"));
    }
}
