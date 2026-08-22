package com.portal.procucev.rfq.service;

import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.exception.AttachmentSizeExceededException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.util.FileUtil;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.MessageIDTerm;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReaderService {

    @Value("${app.mail.host:imap.gmail.com}")
    private String mailHost;

    @Value("${app.mail.port:993}")
    private int mailPort;

    @Value("${app.mail.username:rfq@procucev.com}")
    private String mailUsername;

    @Value("${app.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.inbox-folder:INBOX}")
    private String inboxFolder;

    @Value("${app.mail.attachment-directory:./attachments}")
    private String attachmentDirectory;

    /**
     * Per-attachment size limit. Defaults to {@code app.rfq.max-document-bytes} in configuration so
     * the mailbox accepts exactly what the web RFQ upload accepts.
     */
    @Value("${app.mail.max-attachment-bytes:26214400}")
    private long maxAttachmentBytes = 26214400L;

    /** Exposed so callers can report the limit that was breached instead of restating it. */
    public long getMaxAttachmentBytes() {
        return maxAttachmentBytes;
    }

    @Value("${app.mail.connect-timeout-ms:15000}")
    private int connectTimeoutMs = 15000;

    /**
     * IMAP socket read timeout. Downloading a message with attachments is a single read as far as
     * this timeout is concerned, so it needs headroom well beyond a bare protocol exchange.
     */
    @Value("${app.mail.read-timeout-ms:60000}")
    private int readTimeoutMs = 60000;

    @Value("${app.mail.move-max-attempts:3}")
    private int moveMaxAttempts = 3;

    @Value("${app.mail.move-retry-delay-ms:2000}")
    private long moveRetryDelayMs = 2000L;

    /**
     * Connection settings shared by every IMAP operation.
     *
     * <p>Bounded waits: an unreachable or slow mail host must not pin a scheduler thread
     * indefinitely. These were previously duplicated per method with hard-coded values, so raising
     * one timeout silently left the other operation on the old limit.
     */
    private Properties buildImapProperties() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", mailHost);
        props.put("mail.imaps.port", String.valueOf(mailPort));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", "*");
        props.put("mail.imaps.connectiontimeout", String.valueOf(connectTimeoutMs));
        props.put("mail.imaps.timeout", String.valueOf(readTimeoutMs));
        props.put("mail.imaps.writetimeout", String.valueOf(readTimeoutMs));
        return props;
    }

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
        long pollStart = System.currentTimeMillis();

        try {
            Session session = Session.getInstance(buildImapProperties());
            store = session.getStore("imaps");
            store.connect(mailHost, mailPort, mailUsername, mailPassword);
            long connectedAt = System.currentTimeMillis();

            folder = store.getFolder(inboxFolder);
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.search(new jakarta.mail.search.FlagTerm(new Flags(Flags.Flag.SEEN), false));
            int unreadCount = folder.getUnreadMessageCount();
            int totalCount = folder.getMessageCount();
            // Phase timings are logged because an idle poll was taking 13-63 seconds to establish
            // there was nothing to do, and the log only showed the total. Without the split there
            // is no way to tell a slow connect apart from a slow mailbox scan.
            log.info("IMAP status for folder '{}': TotalMessages={}, UnreadMessages={}, SearchUnseenFound={}"
                            + " (connect={} ms, select+search={} ms)",
                    inboxFolder, totalCount, unreadCount, messages.length,
                    connectedAt - pollStart, System.currentTimeMillis() - connectedAt);

            // The fallback exists to cover servers whose SEARCH disagrees with their unread
            // counter, so it is only worth running when the counter actually reports unread mail.
            // Gating it on totalCount > 0 instead meant it ran on every idle poll: it walked the
            // last 51 messages and, with no FetchProfile, each isSet(SEEN) was its own IMAP round
            // trip. That cost 13-175 seconds per run to establish there was nothing to process.
            if (messages.length == 0 && unreadCount > 0) {
                List<Message> unreadList = scanFallbackUnreadMessages(folder, totalCount);
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
                    logParsedEmailContext(data);
                    emailsList.add(data);
                } catch (Exception e) {
                    log.error("Failed to parse message subject '{}': {}", msg.getSubject(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error fetching unread emails from IMAP: {}", e.getMessage(), e);
        } finally {
            // No message is ever flagged DELETED on this path, so there is nothing to expunge.
            long closeStart = System.currentTimeMillis();
            closeFolderAndStore(folder, store, false);
            log.info("IMAP poll finished: parsed={} email(s), close={} ms, total={} ms",
                    emailsList.size(), System.currentTimeMillis() - closeStart,
                    System.currentTimeMillis() - pollStart);
        }

        return emailsList;
    }

    public List<Message> scanFallbackUnreadMessages(Folder folder, int totalCount) throws MessagingException {
        int start = Math.max(1, totalCount - 50);
        Message[] recentMessages = folder.getMessages(start, totalCount);

        FetchProfile flagsOnly = new FetchProfile();
        flagsOnly.add(FetchProfile.Item.FLAGS);
        folder.fetch(recentMessages, flagsOnly);

        List<Message> unreadList = new ArrayList<>();
        for (Message msg : recentMessages) {
            if (!msg.isSet(Flags.Flag.SEEN)) {
                unreadList.add(msg);
            }
        }
        return unreadList;
    }

    /**
     * Files a processed message into the Processed or Error folder, retrying a transient failure.
     *
     * <p>A failed move used to be logged and forgotten, leaving the message sitting in the inbox
     * already flagged SEEN: it was neither filed nor eligible for another poll, so the mailbox
     * silently drifted out of step with the RFQ records.
     */
    public void moveMessageToFolder(String messageId, String targetFolderName) {
        log.info("Moving message [{}] to folder '{}'", messageId, targetFolderName);
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= Math.max(1, moveMaxAttempts); attempt++) {
            try {
                moveMessageOnce(messageId, targetFolderName);
                return;
            } catch (Exception e) {
                lastFailure = e;
                log.warn("Attempt {}/{} to move message [{}] to folder '{}' failed: {}",
                        attempt, moveMaxAttempts, messageId, targetFolderName, e.getMessage());
                if (attempt < moveMaxAttempts) {
                    sleepBeforeRetry();
                }
            }
        }

        log.error("Error moving message [{}] to folder '{}' after {} attempt(s): {}",
                messageId, targetFolderName, moveMaxAttempts,
                lastFailure != null ? lastFailure.getMessage() : "unknown", lastFailure);
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(moveRetryDelayMs);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * One attempt at the move. Throws on a transient IMAP failure so the caller can retry, and
     * returns normally when the message simply is not in the inbox, which no retry would fix.
     */
    private void moveMessageOnce(String messageId, String targetFolderName) throws MessagingException {
        Store store = null;
        Folder srcFolder = null;
        try {
            Session session = Session.getInstance(buildImapProperties());
            store = session.getStore("imaps");
            store.connect(mailHost, mailPort, mailUsername, mailPassword);

            srcFolder = store.getFolder(inboxFolder);
            srcFolder.open(Folder.READ_WRITE);

            Folder targetFolder = store.getFolder(targetFolderName);
            if (!targetFolder.exists()) {
                targetFolder.create(Folder.HOLDS_MESSAGES);
            }

            Message message = findMessageById(srcFolder, messageId);
            if (message == null) {
                log.warn("Could not find message [{}] in folder '{}' to move to '{}'",
                        messageId, inboxFolder, targetFolderName);
                return;
            }

            message.setFlag(Flags.Flag.SEEN, true);
            srcFolder.copyMessages(new Message[]{message}, targetFolder);
            message.setFlag(Flags.Flag.DELETED, true);
            srcFolder.expunge();
            log.info("Successfully marked SEEN and moved message [{}] to folder '{}'", messageId, targetFolderName);
        } finally {
            // Expunge on close here: this path does flag the moved message DELETED, and closing
            // with expunge re-attempts the removal if the explicit expunge above did not land.
            closeFolderAndStore(srcFolder, store, true);
        }
    }

    /**
     * Locates a message by Message-ID using a server-side SEARCH, which costs one round trip
     * whatever the mailbox size.
     *
     * <p>The previous implementation walked every message in the inbox calling
     * {@code getHeader("Message-ID")}. On an IMAP folder that header is not prefetched, so each
     * call was its own round trip: a 50-message inbox meant ~50 sequential round trips per move
     * and regularly exceeded the socket read timeout, which is what produced the
     * "BYE ... Read timed out" failures. This is the same trap the unread scan already avoids
     * with a FetchProfile.
     */
    private Message findMessageById(Folder folder, String messageId) throws MessagingException {
        String normalizedId = normalizeMessageId(messageId);
        if (normalizedId.isEmpty()) {
            return null;
        }

        for (String candidate : new String[]{"<" + normalizedId + ">", normalizedId}) {
            try {
                Message[] found = folder.search(new MessageIDTerm(candidate));
                if (found != null && found.length > 0) {
                    return found[0];
                }
            } catch (MessagingException e) {
                // Fall through to the local scan; some servers reject or mis-handle HEADER SEARCH.
                log.debug("IMAP SEARCH by Message-ID '{}' was not usable: {}", candidate, e.getMessage());
            }
        }

        return scanForMessageId(folder, normalizedId);
    }

    /** Fallback for servers whose SEARCH is unreliable: one bulk FETCH, then a local comparison. */
    private Message scanForMessageId(Folder folder, String normalizedId) throws MessagingException {
        Message[] messages = folder.getMessages();
        if (messages.length == 0) {
            return null;
        }

        FetchProfile headersOnly = new FetchProfile();
        headersOnly.add("Message-ID");
        folder.fetch(messages, headersOnly);

        // Newest first: the message just processed is almost always the most recent arrival.
        for (int i = messages.length - 1; i >= 0; i--) {
            String[] headers = messages[i].getHeader("Message-ID");
            if (headers != null && headers.length > 0
                    && normalizedId.equalsIgnoreCase(normalizeMessageId(headers[0]))) {
                return messages[i];
            }
        }
        return null;
    }

    private String normalizeMessageId(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[<>]", "").trim();
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
        boolean fileSizeExceeded = false;
        String errorMessage = null;
        String failedAttachmentName = null;

        try {
            if (msg.isMimeType("text/plain")) {
                bodyBuilder.append(msg.getContent().toString());
            } else if (msg.isMimeType("text/html")) {
                bodyBuilder.append(htmlToText(msg.getContent().toString()));
            } else if (msg.isMimeType("multipart/*")) {
                MimeMultipart multipart = (MimeMultipart) msg.getContent();
                processMultipart(multipart, bodyBuilder, htmlFallbackBuilder, attachmentTextBuilder, attachments);
            }
        } catch (AttachmentSizeExceededException e) {
            fileSizeExceeded = true;
            failedAttachmentName = e.getFileName();
            errorMessage = e.getMessage();
            log.warn("Attachment size limit exceeded for message [{}]: {}", messageId, errorMessage);
        }

        if (bodyBuilder.length() == 0 && htmlFallbackBuilder.length() > 0) {
            bodyBuilder.append(htmlFallbackBuilder);
        } else if (htmlFallbackBuilder.length() > 0 && htmlFallbackBuilder.toString().contains("|") && !bodyBuilder.toString().contains("|")) {
            bodyBuilder.append("\n--- Structured HTML Content ---\n").append(htmlFallbackBuilder);
        }

        String inReplyTo = null;
        String references = null;
        try {
            String[] inReplyToHeaders = msg.getHeader("In-Reply-To");
            if (inReplyToHeaders != null && inReplyToHeaders.length > 0) {
                inReplyTo = inReplyToHeaders[0];
            }
            String[] refHeaders = msg.getHeader("References");
            if (refHeaders != null && refHeaders.length > 0) {
                references = refHeaders[0];
            }
        } catch (Exception ignored) {}

        return EmailData.builder()
                .messageId(messageId)
                .subject(subject)
                .senderEmail(senderEmail)
                .senderName(senderName)
                .receivedDate(msg.getReceivedDate())
                .body(bodyBuilder.toString().trim())
                .attachments(attachments)
                .attachmentText(attachmentTextBuilder.toString())
                .inReplyTo(inReplyTo)
                .references(references)
                .fileSizeExceeded(fileSizeExceeded)
                .errorMessage(errorMessage)
                .failedAttachmentName(failedAttachmentName)
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
                                throw new AttachmentSizeExceededException(fileName, totalBytes, maxAttachmentBytes);
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

    /**
     * Records everything about a parsed email that later stages branch on.
     *
     * <p>The thread headers matter most. {@code References} decides whether item identity gets
     * inherited from an earlier RFQ, and it was never logged: an email that silently picked up a
     * previous thread's product looked identical in the log to one that did not.
     */
    private void logParsedEmailContext(EmailData data) {
        String body = data.getBody() != null ? data.getBody() : "";
        String attachmentText = data.getAttachmentText() != null ? data.getAttachmentText() : "";
        log.info("EMAIL CONTEXT [{}]: bodyChars={}, attachmentTextChars={}, attachments={}, inReplyTo={}, references={}",
                data.getMessageId(), body.length(), attachmentText.length(),
                data.getAttachments() != null ? data.getAttachments().size() : 0,
                data.getInReplyTo(), data.getReferences());

        if (data.getInReplyTo() != null || data.getReferences() != null) {
            log.info("EMAIL CONTEXT [{}]: this email is part of an existing thread, so item identity may be "
                    + "inherited from an earlier message unless it names its own product.", data.getMessageId());
        }

        if (data.getAttachments() != null) {
            for (File attachment : data.getAttachments()) {
                // Deliberately does not re-extract the text: that would re-parse every PDF and
                // spreadsheet a second time purely to produce a log line.
                log.info("EMAIL CONTEXT [{}]: attachment '{}' ({} bytes), sentToVisionModel={}",
                        data.getMessageId(), attachment.getName(), attachment.length(),
                        FileUtil.isVisionImage(attachment));
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

    public String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = html;
        text = text.replaceAll("(?i)<tr[^>]*>", "\n| ");
        text = text.replaceAll("(?i)</tr>", " |");
        text = text.replaceAll("(?i)</th[^>]*>", " |");
        text = text.replaceAll("(?i)<th[^>]*>", " ");
        text = text.replaceAll("(?i)</td[^>]*>", " |");
        text = text.replaceAll("(?i)<td[^>]*>", " ");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</p>", "\n");
        text = text.replaceAll("(?i)</div>", "\n");
        text = text.replaceAll("(?i)</li>", "\n");
        text = text.replaceAll("(?i)<li[^>]*>", "\n- ");
        text = text.replaceAll("(?i)<[^>]+>", " ");
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");

        String[] lines = text.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.replaceAll("[ \\t\\x0B\\f]+", " ").trim();
            if (!trimmed.isEmpty()) {
                sb.append(trimmed).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Closes the folder and store.
     *
     * <p>{@code expunge} must only be true where the operation actually flagged a message DELETED.
     * Closing with expunge asks the server to permanently remove flagged messages, and Gmail
     * charges real latency for that round trip: on the read-only poll, where nothing is ever
     * flagged, it was costing 8-21 seconds per minute to expunge an empty set.
     */
    private void closeFolderAndStore(Folder folder, Store store, boolean expunge) {
        if (folder != null && folder.isOpen()) {
            try { folder.close(expunge); } catch (Exception ignored) {}
        }
        if (store != null && store.isConnected()) {
            try { store.close(); } catch (Exception ignored) {}
        }
    }
}
