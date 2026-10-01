package com.portal.procucev.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.DailyRfqReportDispatchResponse;
import com.portal.procucev.Dto.DailyRfqReportPreviewDto;
import com.portal.procucev.Dto.RfqChannel;
import com.portal.procucev.dao.DailyRfqReportLogDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.model.DailyRfqReportLog;
import com.portal.procucev.model.Rfq;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class DailyRfqReportServiceImpl implements DailyRfqReportService {

    private static final Logger log = LoggerFactory.getLogger(DailyRfqReportServiceImpl.class);

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter SUBJECT_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    @Value("${daily.rfq.report.from.email:notifications@procucev.com}")
    private String defaultFromEmail;

    @Value("${daily.rfq.report.to.email:veerababu.v@procucev.com}")
    private String defaultToEmail;

    @Autowired
    private RfqDao rfqDao;

    @Autowired
    private DailyRfqReportLogDao dailyRfqReportLogDao;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public DailyRfqReportPreviewDto previewDailyReport(LocalDate targetDate) {
        LocalDate effectiveDate = resolveTargetDate(targetDate);
        IntakeData intakeData = fetchAndClassifyRfqs(effectiveDate);

        String emailText = generateEmailBody(intakeData.webPortalRfqs, intakeData.whatsappRfqs, intakeData.emailRfqs);
        String subject = "Daily RFQ Report for production - " + effectiveDate.format(SUBJECT_DATE_FORMATTER);

        return DailyRfqReportPreviewDto.builder()
                .reportDate(effectiveDate)
                .webPortalCount(intakeData.webPortalRfqs.size())
                .whatsappCount(intakeData.whatsappRfqs.size())
                .emailCount(intakeData.emailRfqs.size())
                .totalCount(intakeData.allRfqs.size())
                .webPortalRfqIds(intakeData.webPortalRfqs)
                .whatsappRfqIds(intakeData.whatsappRfqs)
                .emailRfqIds(intakeData.emailRfqs)
                .rfqIds(intakeData.allRfqs)
                .emailText(emailText)
                .subject(subject)
                .recipient(defaultToEmail)
                .build();
    }

    @Override
    @Transactional
    public DailyRfqReportDispatchResponse dispatchDailyReport(LocalDate targetDate, boolean force, String overrideTo) {
        LocalDate effectiveDate = resolveTargetDate(targetDate);
        String recipient = resolveRecipient(overrideTo);

        IntakeData intakeData = fetchAndClassifyRfqs(effectiveDate);
        String emailText = generateEmailBody(intakeData.webPortalRfqs, intakeData.whatsappRfqs, intakeData.emailRfqs);
        String subject = "Daily RFQ Report for production - " + effectiveDate.format(SUBJECT_DATE_FORMATTER);

        Optional<DailyRfqReportLog> existingLogOpt = dailyRfqReportLogDao.findByReportDate(effectiveDate);
        if (existingLogOpt.isPresent() && !force) {
            log.info("Daily RFQ report for date {} was already dispatched at {}. Skipping execution (force=false).",
                    effectiveDate, existingLogOpt.get().getDispatchedAt());
            return DailyRfqReportDispatchResponse.builder()
                    .status("SKIPPED")
                    .message("Daily RFQ report for date " + effectiveDate + " already exists. Use force=true to override.")
                    .log(existingLogOpt.get())
                    .emailText(emailText)
                    .build();
        }

        // Send email
        sendEmail(recipient, subject, emailText);

        // Serialize RFQ IDs to JSON
        String rfqIdsJson;
        try {
            rfqIdsJson = objectMapper.writeValueAsString(intakeData.allRfqs);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize RFQ IDs to JSON string, fallback to toString()", e);
            rfqIdsJson = intakeData.allRfqs.toString();
        }

        // Record entry in database
        DailyRfqReportLog reportLog = existingLogOpt.orElseGet(DailyRfqReportLog::new);
        reportLog.setReportDate(effectiveDate);
        reportLog.setWebPortalCount(intakeData.webPortalRfqs.size());
        reportLog.setWhatsappCount(intakeData.whatsappRfqs.size());
        reportLog.setEmailCount(intakeData.emailRfqs.size());
        reportLog.setTotalCount(intakeData.allRfqs.size());
        reportLog.setRfqIdsJson(rfqIdsJson);
        reportLog.setRecipient(recipient);
        reportLog.setDispatchedAt(LocalDateTime.now());

        DailyRfqReportLog savedLog = dailyRfqReportLogDao.save(reportLog);
        log.info("Successfully recorded daily RFQ report log for date {} (ID: {})", effectiveDate, savedLog.getId());

        return DailyRfqReportDispatchResponse.builder()
                .status("SUCCESS")
                .message("Daily RFQ report dispatched and logged successfully for date " + effectiveDate)
                .log(savedLog)
                .emailText(emailText)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyRfqReportLog> getReportHistory() {
        return dailyRfqReportLogDao.findAllByOrderByReportDateDesc();
    }

    @Override
    public String generateEmailBody(List<String> webPortalRfqs, List<String> whatsappRfqs, List<String> emailRfqs) {
        List<String> safeWeb = webPortalRfqs != null ? webPortalRfqs : Collections.emptyList();
        List<String> safeWa = whatsappRfqs != null ? whatsappRfqs : Collections.emptyList();
        List<String> safeEmail = emailRfqs != null ? emailRfqs : Collections.emptyList();
        int totalCount = safeWeb.size() + safeWa.size() + safeEmail.size();

        StringBuilder sb = new StringBuilder();
        sb.append("Daily RFQ Report\n\n");

        sb.append("Web Portal : ").append(safeWeb.size());
        for (String rfqId : safeWeb) {
            sb.append("\n  - ").append(rfqId);
        }
        sb.append("\n\n");

        sb.append("WhatsApp   : ").append(safeWa.size());
        for (String rfqId : safeWa) {
            sb.append("\n  - ").append(rfqId);
        }
        sb.append("\n\n");

        sb.append("Email      : ").append(safeEmail.size());
        for (String rfqId : safeEmail) {
            sb.append("\n  - ").append(rfqId);
        }
        sb.append("\n\n");

        sb.append("Total      : ").append(totalCount);

        return sb.toString();
    }

    private LocalDate resolveTargetDate(LocalDate targetDate) {
        if (targetDate != null) {
            return targetDate;
        }
        return LocalDate.now(IST_ZONE).minusDays(1);
    }

    private String resolveRecipient(String overrideTo) {
        if (overrideTo != null && !overrideTo.isBlank()) {
            return overrideTo.trim();
        }
        return defaultToEmail;
    }

    private IntakeData fetchAndClassifyRfqs(LocalDate targetDate) {
        // Convert local IST midnight boundary (00:00:00 to 23:59:59.999) to UTC timestamps
        ZonedDateTime startIst = targetDate.atStartOfDay(IST_ZONE);
        ZonedDateTime endIst = targetDate.atTime(23, 59, 59, 999_000_000).atZone(IST_ZONE);

        Date startUtc = Date.from(startIst.toInstant());
        Date endUtc = Date.from(endIst.toInstant());

        log.info("Querying RFQs for report date {} (IST [{} to {}] -> UTC [{} to {}])",
                targetDate, startIst, endIst, startUtc, endUtc);

        List<Rfq> rfqs = rfqDao.findDailyIntakeRfqs(startUtc, endUtc);
        log.info("Found {} client RFQs matching criteria for date {}", rfqs.size(), targetDate);

        IntakeData data = new IntakeData();
        for (Rfq rfq : rfqs) {
            String rfqId = (rfq.getRfqId() != null && !rfq.getRfqId().isBlank())
                    ? rfq.getRfqId().trim()
                    : rfq.getId();

            data.allRfqs.add(rfqId);
            RfqChannel channel = RfqChannel.classify(rfq.getSourceType(), rfqId);
            switch (channel) {
                case WHATSAPP -> data.whatsappRfqs.add(rfqId);
                case EMAIL -> data.emailRfqs.add(rfqId);
                case WEB_PORTAL -> data.webPortalRfqs.add(rfqId);
            }
        }

        return data;
    }

    private void sendEmail(String recipient, String subject, String body) {
        try {
            log.info("Sending Daily RFQ Report email from {} to {} with subject: {}",
                    defaultFromEmail, recipient, subject);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(defaultFromEmail));

            String normalizedRecipient = (recipient != null) ? recipient.replace(";", ",") : "";
            if (normalizedRecipient.contains(",")) {
                helper.setTo(InternetAddress.parse(normalizedRecipient));
            } else {
                helper.setTo(normalizedRecipient.trim());
            }

            helper.setSubject(subject);
            helper.setText(body, false);

            javaMailSender.send(mimeMessage);
            log.info("Daily RFQ Report email successfully dispatched to: {}", recipient);
        } catch (Exception e) {
            log.error("Failed to send Daily RFQ Report email to {}: {}", recipient, e.getMessage(), e);
            throw new RuntimeException("Failed to dispatch Daily RFQ Report email: " + e.getMessage(), e);
        }
    }

    private static class IntakeData {
        final List<String> webPortalRfqs = new ArrayList<>();
        final List<String> whatsappRfqs = new ArrayList<>();
        final List<String> emailRfqs = new ArrayList<>();
        final List<String> allRfqs = new ArrayList<>();
    }
}
