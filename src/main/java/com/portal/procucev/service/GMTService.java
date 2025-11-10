package com.portal.procucev.service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;

import com.portal.procucev.Dto.ClientRFQDto;
import com.portal.procucev.Dto.ForwardRfqVendorRequest;
import com.portal.procucev.Dto.GMTRfqVendorDto;
import com.portal.procucev.Dto.RfqDTO;
import com.portal.procucev.Dto.VendorInfoDto;
import com.portal.procucev.Dto.VendorRFQDto;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.customexception.RfqStatusResponse;
import com.portal.procucev.model.CategoryDivision;
import com.portal.procucev.model.EmailRequest;
import com.portal.procucev.model.GmtItems;
import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.RfqStatusRequest;
import com.portal.procucev.model.RfqVendor;
import com.portal.procucev.model.SubscriptionPlan;
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

	boolean createRFQWithNoPr(Rfq rfq);

	boolean forwardRfqForNoPr(Rfq rfq);

	void emailForwarder();

	List<RfqItem> getItemsbyrfqrid(Rfq rfq);

	List<Organization> fetchSelfRegisterClients();

	boolean editUser(User user);

	boolean acceptSelfClient(User user) throws UnsupportedEncodingException;

	boolean ignoreClient(User user);

	boolean disableUser(User user);

	Object getclientusersByClientId(Organization org);

	boolean editAndResendRfq(Rfq rfq) throws MessagingException;

	List<RfqVendor> getVendorsbyRFQ(Rfq rfq);

	List<User> getGmtBuyers();

	Map<String, Object> createRFQByClient(Rfq rfq) throws Exception;

	MessageResponse sendEmail(EmailRequest request);

	List<SubscriptionPlan> getSubscriptionPlans();

	List<RfqStatusResponse> getRfqStatuses(RfqStatusRequest request);

	int getSellerRfqCredits(Organization org);

	Organization getOrgById(Organization organization);

	Map<String, Object> getRfqByItemCategory(Organization org);

	Map<String, Object> forwardRfqsToVendor(ForwardRfqVendorRequest request);

	Organization getOrgByUserId(User user);

	List<Map<String, Object>> getLastOpenRfqsForVendor(String id);

	boolean requestRfqBySellers(List<GmtRfqVendors> rfq);

	VendorInfoDto getVendorInfo(Organization orgRequest);

	void updateVendorClasses();


}
