package com.portal.procucev.rfq.service;

import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.util.FileUtil;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReaderService {

    @Value("${app.mail.host:imap.gmail.com}")
    private String mailHost;

    @Value("${app.mail.username:rfq@procucev.com}")
    private String mailUsername;

    @Value("${app.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.port:993}")
    private int mailPort = 993;

    @Value("${app.mail.inbox-folder:INBOX}")
    private String inboxFolder;

    @Value("${app.mail.attachment-directory:./attachments}")
    private String attachmentDirectory;

    @Value("${app.mail.max-attachment-bytes:26214400}")
    private long maxAttachmentBytes = 26214400L;

    public List<EmailData> fetchUnreadEmails() {
        if (mailPassword == null || mailPassword.isBlank()) {
            throw new ApplicationException(
                    "Mailbox password is not configured. Set the EMAIL_PASSWORD environment variable "
                            + "(or the app.mail.password property) to enable email-to-RFQ processing.");
        }
        log.info("Connecting to IMAP server ({}) for user: {}", mailHost, mailUsername);
        List<EmailData> emailsList = new ArrayList<>();
        Store store = null;
        Folder folder = null;

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", mailHost);
            props.put("mail.imaps.port", String.valueOf(mailPort));
            props.put("mail.imaps.ssl.enable", "true");
            // Bounded waits: an unreachable mail host must not pin a scheduler
            // thread indefinitely.
            props.put("mail.imaps.connectiontimeout", "15000");
            props.put("mail.imaps.timeout", "30000");
            props.put("mail.imaps.writetimeout", "30000");

            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(mailHost, mailUsername, mailPassword);

            folder = store.getFolder(inboxFolder);
            folder.open(Folder.READ_ONLY);

            Message[] messages = folder.search(new jakarta.mail.search.FlagTerm(new Flags(Flags.Flag.SEEN), false));
            int unreadCount = folder.getUnreadMessageCount();
            int totalCount = folder.getMessageCount();
            log.info("IMAP status for folder '{}': TotalMessages={}, UnreadMessages={}, SearchUnseenFound={}",
                    inboxFolder, totalCount, unreadCount, messages.length);

            if (messages.length == 0 && totalCount > 0) {
                int start = Math.max(1, totalCount - 50);
                Message[] recentMessages = folder.getMessages(start, totalCount);
                List<Message> unreadList = new ArrayList<>();
                for (Message msg : recentMessages) {
                    if (!msg.isSet(Flags.Flag.SEEN)) {
                        unreadList.add(msg);
                    }
                }
                if (!unreadList.isEmpty()) {
                    messages = unreadList.toArray(new Message[0]);
                    log.info("Fallback scan detected {} unread message(s) among recent messages.", messages.length);
                }
            }

            for (Message msg : messages) {
                try {
                    EmailData data = parseMessage(msg);
                    log.info("Parsed unread email: Subject='{}', From='{}', ReceivedDate='{}'",
                            data.getSubject(), data.getSenderEmail(), data.getReceivedDate());
                    emailsList.add(data);
                } catch (Exception e) {
                    log.error("Failed to parse message subject '{}': {}", msg.getSubject(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error fetching unread emails from IMAP: {}", e.getMessage(), e);
        } finally {
            closeFolderAndStore(folder, store);
        }

        return emailsList;
    }

    public void moveMessageToFolder(String messageId, String targetFolderName) {
        log.info("Moving message [{}] to folder '{}'", messageId, targetFolderName);
        Store store = null;
        Folder srcFolder = null;

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", mailHost);
            props.put("mail.imaps.port", String.valueOf(mailPort));
            props.put("mail.imaps.ssl.enable", "true");
            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(mailHost, mailUsername, mailPassword);

            srcFolder = store.getFolder(inboxFolder);
            srcFolder.open(Folder.READ_WRITE);

            Folder targetFolder = store.getFolder(targetFolderName);
            if (!targetFolder.exists()) {
                targetFolder.create(Folder.HOLDS_MESSAGES);
            }

            Message[] messages = srcFolder.getMessages();
            for (Message msg : messages) {
                String[] headers = msg.getHeader("Message-ID");
                if (headers != null && headers.length > 0 && headers[0].equals(messageId)) {
                    srcFolder.copyMessages(new Message[]{msg}, targetFolder);
                    msg.setFlag(Flags.Flag.DELETED, true);
                    log.info("Successfully moved message [{}] to '{}'", messageId, targetFolderName);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error moving message [{}] to folder '{}': {}", messageId, targetFolderName, e.getMessage());
        } finally {
            closeFolderAndStore(srcFolder, store);
        }
    }

    private EmailData parseMessage(Message msg) throws Exception {
        String messageId = getMessageId(msg);
        String subject = msg.getSubject() != null ? msg.getSubject() : "(No Subject)";

        String senderEmail = "";
        String senderName = "";
        Address[] froms = msg.getFrom();
        if (froms != null && froms.length > 0) {
            if (froms[0] instanceof InternetAddress ia) {
                senderEmail = ia.getAddress();
                senderName = ia.getPersonal() != null ? ia.getPersonal() : senderEmail;
            } else {
                senderEmail = froms[0].toString();
            }
        }

        StringBuilder bodyBuilder = new StringBuilder();
        StringBuilder htmlFallbackBuilder = new StringBuilder();
        StringBuilder attachmentTextBuilder = new StringBuilder();
        List<File> attachments = new ArrayList<>();

        if (msg.isMimeType("text/plain")) {
            bodyBuilder.append(msg.getContent().toString());
        } else if (msg.isMimeType("text/html")) {
            bodyBuilder.append(htmlToText(msg.getContent().toString()));
        } else if (msg.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) msg.getContent();
            processMultipart(multipart, bodyBuilder, htmlFallbackBuilder, attachmentTextBuilder, attachments);
        }

        if (bodyBuilder.length() == 0 && htmlFallbackBuilder.length() > 0) {
            bodyBuilder.append(htmlFallbackBuilder);
        }

        return EmailData.builder()
                .messageId(messageId)
                .subject(subject)
                .senderEmail(senderEmail)
                .senderName(senderName)
                .receivedDate(msg.getReceivedDate())
                .body(bodyBuilder.toString())
                .attachments(attachments)
                .attachmentText(attachmentTextBuilder.toString())
                .build();
    }

    private void processMultipart(MimeMultipart multipart, StringBuilder bodyBuilder, StringBuilder htmlFallbackBuilder,
                                  StringBuilder attTextBuilder, List<File> attachments) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) || bodyPart.getFileName() != null) {
                String fileName = bodyPart.getFileName();
                File savedFile = createAttachmentFile(fileName);
                try {
                    try (InputStream inputStream = bodyPart.getInputStream();
                         OutputStream outputStream = Files.newOutputStream(savedFile.toPath())) {
                        byte[] buffer = new byte[8192];
                        long totalBytes = 0;
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            totalBytes += bytesRead;
                            if (totalBytes > maxAttachmentBytes) {
                                throw new ApplicationException("Attachment exceeds the configured size limit.");
                            }
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    Files.deleteIfExists(savedFile.toPath());
                    throw e;
                }
                attachments.add(savedFile);

                String text = FileUtil.extractTextFromFile(savedFile);
                if (!text.isBlank()) {
                    attTextBuilder.append("\n--- Attachment: ").append(fileName).append(" ---\n").append(text);
                }
            } else if (bodyPart.isMimeType("text/plain")) {
                bodyBuilder.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {
                htmlFallbackBuilder.append(htmlToText(bodyPart.getContent().toString()));
            } else if (bodyPart.isMimeType("multipart/*")) {
                processMultipart((MimeMultipart) bodyPart.getContent(), bodyBuilder, htmlFallbackBuilder, attTextBuilder, attachments);
            }
        }
    }

    private String getMessageId(Message msg) throws Exception {
        String[] hdrs = msg.getHeader("Message-ID");
        if (hdrs != null && hdrs.length > 0) return hdrs[0];
        return UUID.randomUUID().toString();
    }

    private File createAttachmentFile(String originalFileName) throws Exception {
        if (originalFileName != null && originalFileName.contains("..")) {
            throw new ApplicationException("Invalid attachment file path received.");
        }
        String safeFileName = Paths.get(originalFileName == null || originalFileName.isBlank() ? "attachment" : originalFileName)
                .getFileName()
                .toString();
        Path attachmentDir = Paths.get(attachmentDirectory).toAbsolutePath().normalize();
        Files.createDirectories(attachmentDir);

        Path savedPath = attachmentDir.resolve(System.currentTimeMillis() + "_" + safeFileName).normalize();
        if (!savedPath.startsWith(attachmentDir)) {
            throw new ApplicationException("Invalid attachment file path received.");
        }
        return savedPath.toFile();
    }

    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s+", "\n")
                .trim();
    }

    private void closeFolderAndStore(Folder folder, Store store) {
        if (folder != null && folder.isOpen()) {
            try { folder.close(true); } catch (Exception ignored) {}
        }
        if (store != null && store.isConnected()) {
            try { store.close(); } catch (Exception ignored) {}
        }
    }
}
