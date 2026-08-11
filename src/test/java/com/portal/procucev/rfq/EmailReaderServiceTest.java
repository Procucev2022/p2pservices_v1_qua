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
        assertThrows(Exception.class, () -> ReflectionTestUtils.invokeMethod(service, "createAttachmentFile", "../../../secret.txt"));
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
}
