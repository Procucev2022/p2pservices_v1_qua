package com.portal.procucev.rfq;

import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.service.EmailReaderService;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmailReaderServiceTest {

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
        // Still more than one attempt so the move retry loop is exercised, but without the
        // production back-off holding the suite up.
        ReflectionTestUtils.setField(service, "moveMaxAttempts", 2);
        ReflectionTestUtils.setField(service, "moveRetryDelayMs", 1L);
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
    @DisplayName("Test htmlToText helper method")
    void testHtmlToText() {
        String html = "<p>Line 1</p><br/>Line 2&nbsp;Extra";
        String text = ReflectionTestUtils.invokeMethod(service, "htmlToText", html);
        assertTrue(text.contains("Line 1"));
        assertTrue(text.contains("Line 2 Extra"));

        String nullText = ReflectionTestUtils.invokeMethod(service, "htmlToText", (String) null);
        assertEquals("", nullText);
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

        // Expunging on a folder where nothing was flagged DELETED is a pure round trip to Gmail,
        // and it was costing 8-21 seconds on every idle poll.
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
}


