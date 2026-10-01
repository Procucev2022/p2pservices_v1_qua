package com.portal.procucev.service;

import java.time.LocalDate;
import java.util.List;

import com.portal.procucev.Dto.DailyRfqReportDispatchResponse;
import com.portal.procucev.Dto.DailyRfqReportPreviewDto;
import com.portal.procucev.model.DailyRfqReportLog;

public interface DailyRfqReportService {

    /**
     * Previews the daily RFQ intake report for the specified target date without sending an email.
     *
     * @param targetDate the report date in IST (defaults to yesterday in IST if null)
     * @return preview DTO with counts, RFQ IDs, and generated email text
     */
    DailyRfqReportPreviewDto previewDailyReport(LocalDate targetDate);

    /**
     * Dispatches the daily RFQ intake report email, checks deduplication, and records audit persistence.
     *
     * @param targetDate the report date in IST (defaults to yesterday in IST if null)
     * @param force if true, overrides deduplication check and forces sending
     * @param overrideTo optional override for recipient email address
     * @return dispatch response indicating SUCCESS, SKIPPED, or ERROR
     */
    DailyRfqReportDispatchResponse dispatchDailyReport(LocalDate targetDate, boolean force, String overrideTo);

    /**
     * Returns historical execution log entries from daily_rfq_report_log.
     *
     * @return list of audit logs ordered by report date descending
     */
    List<DailyRfqReportLog> getReportHistory();

    /**
     * Formats the plain text email body according to the intake report specifications.
     *
     * @param webPortalRfqs list of RFQ IDs in Web Portal channel
     * @param whatsappRfqs list of RFQ IDs in WhatsApp channel
     * @param emailRfqs list of RFQ IDs in Email channel
     * @return formatted plain text email body
     */
    String generateEmailBody(List<String> webPortalRfqs, List<String> whatsappRfqs, List<String> emailRfqs);
}
