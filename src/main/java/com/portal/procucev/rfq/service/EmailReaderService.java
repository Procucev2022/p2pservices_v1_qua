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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReaderService {

    @Value("${app.mail.host:imap.gmail.com}")
    private String mailHost;

    @Value("${app.mail.username:rfq@procucev.com}")
    private String mailUsername;

    @Value("${app.mail.password:djds biac asvw ppwq}")
    private String mailPassword;

    @Value("${app.mail.inbox-folder:INBOX}")
    private String inboxFolder;

    @Value("${app.mail.attachment-directory:./attachments}")
    private String attachmentDirectory;

    public List<EmailData> fetchUnreadEmails() {
        log.info("Connecting to IMAP server ({}) for user: {}", mailHost, mailUsername);
        List<EmailData> emailsList = new ArrayList<>();
        Store store = null;
        Folder folder = null;

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", mailHost);
            props.put("mail.imaps.port", "993");
            props.put("mail.imaps.ssl.enable", "true");

            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(mailHost, mailUsername, mailPassword);

            folder = store.getFolder(inboxFolder);
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.search(new jakarta.mail.search.FlagTerm(new Flags(Flags.Flag.SEEN), false));
            log.info("Found {} unread message(s) in inbox folder '{}'.", messages.length, inboxFolder);

            for (Message msg : messages) {
                try {
                    EmailData data = parseMessage(msg);
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
        StringBuilder attachmentTextBuilder = new StringBuilder();
        List<File> attachments = new ArrayList<>();

        if (msg.isMimeType("text/plain")) {
            bodyBuilder.append(msg.getContent().toString());
        } else if (msg.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) msg.getContent();
            processMultipart(multipart, bodyBuilder, attachmentTextBuilder, attachments);
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

    private void processMultipart(MimeMultipart multipart, StringBuilder bodyBuilder, StringBuilder attTextBuilder, List<File> attachments) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) || bodyPart.getFileName() != null) {
                String fileName = bodyPart.getFileName();
                File destDir = new File(attachmentDirectory);
                if (!destDir.exists()) destDir.mkdirs();

                File savedFile = new File(destDir, System.currentTimeMillis() + "_" + fileName);
                bodyPart.getInputStream().transferTo(new java.io.FileOutputStream(savedFile));
                attachments.add(savedFile);

                String text = FileUtil.extractTextFromFile(savedFile);
                if (!text.isBlank()) {
                    attTextBuilder.append("\n--- Attachment: ").append(fileName).append(" ---\n").append(text);
                }
            } else if (bodyPart.isMimeType("text/plain")) {
                bodyBuilder.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("multipart/*")) {
                processMultipart((MimeMultipart) bodyPart.getContent(), bodyBuilder, attTextBuilder, attachments);
            }
        }
    }

    private String getMessageId(Message msg) throws Exception {
        String[] hdrs = msg.getHeader("Message-ID");
        if (hdrs != null && hdrs.length > 0) return hdrs[0];
        return UUID.randomUUID().toString();
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
