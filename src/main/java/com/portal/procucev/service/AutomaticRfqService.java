package com.portal.procucev.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.User;

@Service
public interface AutomaticRfqService {

	boolean raiseRfq(Rfq rfq);

	String generateRfqId(String company);

	Map<String, String> validateEmail(String email);

	/**
	 * Normalises the RFQ's delivery location in place, filling whatever the caller did not supply
	 * from the pincode master and finally from the buyer's organisation address.
	 *
	 * <p>Shared by the web upload and the email-to-RFQ pipeline so both resolve a partial address
	 * the same way. It previously lived inline in the web controller, which meant an RFQ raised
	 * from an email never got pincode-based city/state resolution at all.
	 */
	void resolveDeliveryLocation(Rfq rfq, User user);

	/**
	 * Converts Base64-encoded document payloads into {@code rfq_documents} rows on the RFQ.
	 *
	 * <p>Each entry is expected to carry a {@code fileName} and a Base64 {@code file}. Entries with
	 * no payload are skipped.
	 *
	 * @throws com.portal.procucev.customexception.RfqDocumentSizeExceededException if any decoded
	 *         document is larger than {@link #getMaxDocumentBytes()}
	 */
	void attachDocuments(Rfq rfq, List<Map<String, String>> documents);

	/** Maximum size in bytes of a single document attached to an RFQ, from configuration. */
	long getMaxDocumentBytes();
}
