package com.portal.procucev.service;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.http.ResponseEntity;

import com.portal.procucev.Dto.ClientRFQDto;
import com.portal.procucev.Dto.GMTRfqVendorDto;
import com.portal.procucev.Dto.RfqDTO;
import com.portal.procucev.Dto.VendorRFQDto;
import com.portal.procucev.model.CategoryDivision;
import com.portal.procucev.model.GmtItems;
import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.User;

import jakarta.mail.MessagingException;

public interface GMTService {

	List<String> getClientRfqIds(User user);

	List<ClientRFQDto> getNoPrRfqByClient(User user);

	List<String> getAllDivision();

	boolean createRFQForNoPrByClient(Rfq rfq) throws Exception;

	boolean editRFQForNoPrByClient(Rfq updatedRfq) throws Exception;

	List<RfqItem> convertRFQBoq(Rfq rfq);

	boolean queryMail(User user) throws UnsupportedEncodingException;

	List<GMTRfqVendorDto> getAllGMTRfq(Organization org);

	List<RfqDTO> fetchAllClientGMTRfqsForCM();

	boolean requestRfqByVendors(List<GmtRfqVendors> rfq);

	/**
	 * Retrieves vendors associated with a given RFQ.
	 * 
	 * @param rfq The RFQ object.
	 * @return List of GMT RFQ vendors.
	 * @throws AppException if no vendors are found for the RFQ.
	 */
	List<GmtRfqVendors> getVendorsByGmtRfq(Rfq rfq);

	boolean approveVendor(GmtRfqVendors gmtRfq) throws MessagingException;

	boolean ignoreRfqByVendor(List<GmtRfqVendors> rfq);

	boolean rejectRfqForVendor(GmtRfqVendors rfq);

	List<RfqDTO> getAllRfqForCM();

	boolean raiseQueryByVendor(GmtRfqVendors rfq);

	List<GmtItems> getAllGmtItems();

	List<String> getCategoryByDivision(CategoryDivision cat);

	/**
	 * Return rfq details based on the ID
	 */
	ResponseEntity<?> fetchRfqById(Rfq rfq);

	List<String> getAllCategory();

	List<RfqDTO> getRFQsForNoPR();

	List<VendorRFQDto> getAllVendors();

	List<VendorRFQDto> getAllVendorsByCategory(Organization organization);

//	boolean createRFQWithNoPr(Rfq rfq);

}
