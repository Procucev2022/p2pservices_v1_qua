package com.portal.procucev.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.*;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.data.domain.Pageable;
import org.antlr.v4.runtime.atn.SemanticContext.OR;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.portal.procucev.Dto.ClientRFQDto;
import com.portal.procucev.Dto.ForwardRfqVendorRequest;
import com.portal.procucev.Dto.GMTRfqVendorDto;
import com.portal.procucev.Dto.RfqDTO;
import com.portal.procucev.Dto.VendorRFQDto;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.RfqStatusResponse;
import com.portal.procucev.dao.CategoryDivisionDao;
import com.portal.procucev.dao.EmailUserRepo;
import com.portal.procucev.dao.GmtItemsDao;
import com.portal.procucev.dao.GmtRfqVendorDao;
import com.portal.procucev.dao.ItemCategoryDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgCategoryDivisionDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.RFQItemsDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.RfqVendorDao;
import com.portal.procucev.dao.RoleDao;
import com.portal.procucev.dao.SubscriptionPlanDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.CategoryDivision;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.EmailRequest;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.GmtItems;
import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgDivisionCategory;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.RFQDocument;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.RfqStatusRequest;
import com.portal.procucev.model.RfqVendor;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.SubscriptionPlan;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.MailUtility;
import com.portal.procucev.utils.StatusConstants;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.SubjectTerm;

@Service
public class GMTServiceImpl implements GMTService {
	private static final Logger logger = LoggerFactory.getLogger(GMTServiceImpl.class);
	
	@Autowired
	private RfqDao rfqDao;
	
	@Autowired
	private RoleDao roleDao;
	
	@Autowired
	private SubscriptionPlanDao subscriptionPlanDao;
	
	@Autowired
	private CategoryDivisionDao categoryDivisionDao;
	
	@Autowired
	private MasterStatusDao masterStatusDao;
	
	@Autowired
	private SelfRegistrationService selfRegistrationService;
	
	@Autowired
	private GmtItemsDao gmtItemsDao;
	
	@Autowired
	private RfqVendorDao rfqVendorDao;
	
	@Autowired
	private GmtRfqVendorDao gmtRfqVendorDao;
	
	@Autowired
	private ItemCategoryDao itemCategoryDao;
	
	@Autowired
	private OrgDao orgDao;
	
	@Autowired
	private UserDao userDao;
	
	@Autowired
	private OrgTypeDao orgTypeDao;
	
	@Autowired
	private RFQItemsDao rfqItemsDao;

	@Autowired
	private OrgCategoryDivisionDao orgCategoryDivisionDao;
	
	@Value("${quaemail}")
	String mailFom;

	@Value("${host}")
	String host;

	@Value("${mailid}")
	String mail;

	@Value("${mailPassword}")
	String pswd;

	@Value("${toAddress}")
	String toAddress;
	
	@Value("${quapassword}")
	private String emailPassword;
	
	@Autowired
	private JavaMailSender javaMailSender;
	
	@Autowired
	private EmailUserRepo emailUserRepo;
	
	@Override
	public List<String> getClientRfqIds(User user) {
		// TODO Auto-generated method stub
		logger.info("API to get Client Ids By User");
		if (user != null) {
			List<String> rfqList = rfqDao.findRFQIdsNoPrRfqByClient(user.getId());
			if (rfqList.isEmpty()) {
				logger.warn("No RFQs found  by client flag True");
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
						ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
			}
			logger.info("Found {} RFQs for PR Flag True", rfqList.size());
			return rfqList;

		} else {
			logger.warn("No RFQs found  by User");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}
	
	@Override
	public List<ClientRFQDto> getNoPrRfqByClient(User user) {
		logger.info("Entered To get no pr rfq by client");
		// TODO Auto-generated method stub
		if (user != null) {
			List<Rfq> rfqList = rfqDao.findNoPrRfqByClient(user.getId());
			if (rfqList.isEmpty()) {
				logger.warn("No RFQs found  by client flag True");
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
						ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
			}

			logger.info("Found {} RFQs for PR Flag True", rfqList.size());

			return rfqList.stream().map(this::mapToClientPrDto).collect(Collectors.toList());
		} else {
			logger.warn("No RFQs found  by User");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}
	private ClientRFQDto mapToClientPrDto(Rfq rfq) {
		ClientRFQDto rfqDto = new ClientRFQDto();
		rfqDto.setId(rfq.getId());
		rfqDto.setCreatedBy(rfq.getCreatedBy());
		rfqDto.setCreatedTS(rfq.getCreatedTS());
		rfqDto.setProjectDesc(rfq.getProjectDesc());
		rfqDto.setDivision(rfq.getDivision());
		rfqDto.setClientStatus(rfq.getClientStatus());
		rfqDto.setRfqId(rfq.getRfqId());
		rfqDto.setQuotationReceived(rfq.isQuotationReceived());
		rfqDto.setUser(rfq.getUser());
		return rfqDto;
	}
	@Override
	public List<String> getAllDivision() {
		// TODO Auto-generated method stub
		logger.info("Entered To Get All Division");
		List<String> divisionList = categoryDivisionDao.getAllDivision();
		if (divisionList != null && !CollectionUtils.isEmpty(divisionList)) {
			return divisionList;

		} else {
			logger.error("No Divsions Found");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);

		}

	}
	

	@Override
	public boolean createRFQForNoPrByClient(Rfq rfq) throws Exception {
		logger.info("Request received for RFQ creation with No PR by client {}", rfq);

		try {
			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);
			MasterStatus newStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW);

			rfq.setByClient(true);
			rfq.setStatus(resultStatus);
			rfq.setClientStatus(newStatus);

			//String rfqId = selfRegistrationService.generateId("RFQ");
			
			 String rfqId = generateRfqId("RFQ");
			logger.info("Generated RFQ Id: {}", rfqId);
			rfq.setRfqId(rfqId);

			List<RfqItem> rfqItems = rfq.getRfqItem();
			List<GmtItems> gmtItems = rfqItems.stream().map(this::mapRfqItemToGmtItem).collect(Collectors.toList());
			gmtItemsDao.saveAll(gmtItems);
			logger.info("Saved RFQ items in GMT Items");
			rfqDao.save(rfq);
			logger.info("Completed Saving RFQ");
			return true;
		} catch (DataAccessException e) {
			logger.error("Error occurred while creating RFQ: {}", e.getMessage());
			return false;
		}
	}

	private GmtItems mapRfqItemToGmtItem(RfqItem rfqItem) {
		GmtItems gmtItem = new GmtItems();
		gmtItem.setBrand(rfqItem.getBrand());
		gmtItem.setDescription(rfqItem.getDescription());
		gmtItem.setQuantity(rfqItem.getQuantity());
		gmtItem.setRemarks(rfqItem.getRemarks());
		gmtItem.setUnitofMeasures(rfqItem.getUnitofMeasures());
		gmtItem.setRfqItemId(rfqItem.getId());
		return gmtItem;
	}

	@Override
	public boolean editRFQForNoPrByClient(Rfq updatedRfq) throws Exception {
		logger.info("Request received for editing RFQ {}", updatedRfq.getRfqId());

		try {
			// Fetch the existing RFQ from the database
			Rfq existingRfq = rfqDao.findById(updatedRfq.getId()).orElse(null);
			if (existingRfq == null) {
				logger.error("RFQ with ID {} not found", updatedRfq.getId());
				return false; // RFQ not found
			}

			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);
			if (updatedRfq.isByClient() == true) {
				logger.info("Is By Client True");
				if (updatedRfq.isFromClient() == true) {
					logger.info("updating Client Status to New as RFQ is edited by Client is From Client True");
					MasterStatus newStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW);
					updatedRfq.setClientStatus(newStatus);
				} else {
					logger.info("updating Client Status to Accepted as RFQ is edited by Categorymanager");
					MasterStatus acceptStatus = masterStatusDao.findByStatus(StatusConstants.CM_RFQ_ACCEPTED);
					updatedRfq.setClientStatus(acceptStatus);
				}
			}

			updatedRfq.setStatus(resultStatus);

			// Set RFQ ID for RFQ items
			List<RfqItem> updatedRfqItems = updatedRfq.getRfqItem();
			List<GmtItems> existingGmtItems = gmtItemsDao
					.findByRfqItemIdIn(updatedRfqItems.stream().map(RfqItem::getId).collect(Collectors.toList()));

			// Map existing GmtItems to their corresponding RfqItem IDs for efficient
			// updates
			Map<String, GmtItems> rfqItemIdToGmtItemMap = existingGmtItems.stream()
					.collect(Collectors.toMap(GmtItems::getRfqItemId, Function.identity()));

			// Iterate over updated RFQ items
			for (RfqItem updatedItem : updatedRfqItems) {
				GmtItems existingGmtItem = rfqItemIdToGmtItemMap.get(updatedItem.getId());
				if (existingGmtItem != null) {
					// Update existing GmtItem with new values from updated RFQ item
					existingGmtItem.setBrand(updatedItem.getBrand());
					existingGmtItem.setDescription(updatedItem.getDescription());
					existingGmtItem.setQuantity(updatedItem.getQuantity());
					existingGmtItem.setRemarks(updatedItem.getRemarks());
					existingGmtItem.setUnitofMeasures(updatedItem.getUnitofMeasures());
					// Save or update the existing GmtItem
					gmtItemsDao.save(existingGmtItem);
				} else {
					// Handle the case where the item in the updated RFQ is new and not in existing
					// GmtItems
					// You may choose to create a new GmtItems entry here if necessary
					logger.info("No New Items To Be Added");

				}
			}
			if (!CollectionUtils.isEmpty(updatedRfqItems) && updatedRfqItems != null) {
				updatedRfqItems.forEach(rfqItem -> rfqItem.setRfq(updatedRfq));
			}

			// Set RFQ ID for documents
			List<RFQDocument> updatedRFQDocuments = updatedRfq.getRfqDocument();
			if (!CollectionUtils.isEmpty(updatedRFQDocuments) && updatedRFQDocuments != null) {
				updatedRFQDocuments.forEach(rfqDocument -> rfqDocument.setRfq(updatedRfq));
			}
			// Set RFQ Delivery Location
			List<ClientDeliveryLocationRfq> updatedDeliveryLocation = updatedRfq.getClientdeliverylocationrfq();
			if (!CollectionUtils.isEmpty(updatedDeliveryLocation) && updatedDeliveryLocation != null) {
				updatedDeliveryLocation.forEach(clientDeliveryLocation -> clientDeliveryLocation.setRfq(updatedRfq));
			}
			// Set RFQ Vendors
			List<RfqVendor> updatedRFQVendors = rfqVendorDao.findDataByRfqId(updatedRfq.getId());
			logger.info("Size of updatedRFQVendors{}" + updatedRFQVendors.size());
			if (!CollectionUtils.isEmpty(updatedRFQVendors) && updatedRFQVendors != null) {
				updatedRFQVendors.forEach(rfqVendor -> rfqVendor.setRfq(updatedRfq));
			}

			// Save the updated RFQ
			updatedRfq.setClientdeliverylocationrfq(updatedDeliveryLocation);
			updatedRfq.setRfqItem(updatedRfqItems); // Set the RFQ items directly to the RFQ object
			updatedRfq.setRfqDocument(updatedRFQDocuments);
			 updatedRfq.setRfqVendor(updatedRFQVendors);
			rfqDao.save(updatedRfq);
			logger.info("Completed Editing RFQ {}", updatedRfq.getRfqId());

			return true;
		} catch (DataAccessException e) {
			logger.error("Error occurred while editing RFQ: {}", e.getMessage());
			return false;
		}
	}
	
	@Override
	public List<RfqItem> convertRFQBoq(Rfq rfq) {
		logger.info("Entered to convert Rfq Boq");
		List<RfqItem> rfqitems = new ArrayList<RfqItem>();
		if (rfq.getBoqfile().length > 0 && rfq.getBoqfile() != null) {
			rfqitems = processBOQFiles(rfq.getBoqfile());

		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		return rfqitems;
	}

	private List<RfqItem> processBOQFiles(byte[] boqBase64Encoded) {

		List<RfqItem> rfqItem = new ArrayList<RfqItem>();

		ByteArrayInputStream bin = new ByteArrayInputStream(boqBase64Encoded);
		XSSFWorkbook myExcelBook = null;
		try {
			myExcelBook = new XSSFWorkbook(bin);
		} catch (IOException e) {
			logger.error("Error while parsing BOQ file " + e.getMessage());
		}
		XSSFSheet myExcelSheet = myExcelBook.getSheetAt(0);

		// HSSFRow row = myExcelSheet.getRow(7);
		Iterator<Row> rowIteratorforRows = myExcelSheet.iterator();
		Iterator<Row> rowIteratorcheckcount = myExcelSheet.iterator();
		Iterable<Row> newIterable = () -> rowIteratorcheckcount;
		long count = StreamSupport.stream(newIterable.spliterator(), false).count();

		// It will throw Exception when File has Empty Rows

		if (count == 0 || count == 1) {

			logger.info("File Has Empty Rows and Closing the workbook");
			try {
				myExcelBook.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ApplicationConstants.FILE_HAS_EMPTY_ROWS,
					ApplicationConstants.SERVICE_LEVEL_EXCEPTION, ApplicationConstants.FAILURE);
		}

		while (rowIteratorforRows.hasNext()) {
			Row row = rowIteratorforRows.next();

			if (isRowEmpty(row)) {
				continue;
			}
			if (row != null && row.getRowNum() >= 1) {

				if (row.getRowNum() == 1) {
					List<String> headers = new ArrayList<String>();
					Iterator<Cell> cells = row.cellIterator();
					while (cells.hasNext()) {
						Cell cell = (Cell) cells.next();
						RichTextString value = cell.getRichStringCellValue();
						headers.add(value.getString());
					}
					logger.info("Row Format " + headers.toString());

					// This method will validate the format expected to process
					validateExcelTemplate(headers);
					continue;
				} else {

					RfqItem rfqItems = new RfqItem();
					DataFormatter formatter = new DataFormatter();
					if (row.getCell(0) != null) {

						rfqItems.setSerialNo((double) row.getCell(0).getNumericCellValue());
					}
					if (row.getCell(1) != null
							&& StringUtils.isNotEmpty(row.getCell(1).getStringCellValue().replaceAll("^\\s+", ""))) {
						if (row.getCell(1).getStringCellValue().replaceAll("^\\s+", "") != null)
							rfqItems.setDescription(row.getCell(1).getStringCellValue().replaceAll("^\\s+", ""));
					}

					if (row.getCell(2) != null
							&& StringUtils.isNotEmpty(row.getCell(2).getStringCellValue().replaceAll("^\\s+", ""))) {
						if (row.getCell(2).getStringCellValue().replaceAll("^\\s+", "") != null)
							rfqItems.setBrand(row.getCell(2).getStringCellValue().replaceAll("^\\s+", ""));
					}
					if (row.getCell(3) != null
							&& StringUtils.isNotEmpty(row.getCell(3).getStringCellValue().replaceAll("^\\s+", ""))) {
						if (row.getCell(3).getStringCellValue().replaceAll("^\\s+", "") != null)
							rfqItems.setUnitofMeasures(row.getCell(3).getStringCellValue().replaceAll("^\\s+", ""));

					}
					// To allow both Numeric and String values

					if (row.getCell(4) != null) {
						rfqItems.setQuantity((double) row.getCell(4).getNumericCellValue());
					}
					if (row.getCell(5) != null) {
						// To allow both Numeric and String values
						String cellValue = formatter.formatCellValue(row.getCell(5));
						// pritem.setItemcode(String.valueOf(row.getCell(2).getNumericCellValue()));
						rfqItems.setRemarks(cellValue);
					}
					rfqItem.add(rfqItems);

				}

			}

		}
		return rfqItem;

	}

	private boolean validateExcelTemplate(List<String> headers) throws AppException {

		logger.info("Validation Initiated for Processing Excel File.");
		boolean valid = false;
		logger.info("Size of Headers " + headers.size());

		List<String> unknownHeader = new ArrayList<String>();

		List<String> constantsformat = Arrays.asList(ApplicationConstants.REQUIRED_FORMAT_RFQITEMFORMAT.split(", "));

		for (String string : constantsformat) {
			if (!headers.contains(string)) {
				unknownHeader.add(string);
			}
		}

		if (unknownHeader.size() > 0) {
			logger.info("Unknown Headers Found for Uploaded File.");
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					String.format(ApplicationConstants.FILE_FORMAT_NOT_MATCHED_DATA,
							unknownHeader.stream().map(String::valueOf).collect(Collectors.joining(",")),
							ApplicationConstants.REQUIRED_FORMAT_RFQITEMFORMAT),
					ApplicationConstants.SERVICE_LEVEL_EXCEPTION, ApplicationConstants.FAILURE);

		}
		//
		if (headers.size() >= 6) {

			if (headers.size() == 6) {

				if ((ApplicationConstants.SerialNO.equalsIgnoreCase(headers.get(0)))
						&& (ApplicationConstants.ItemDescription.equalsIgnoreCase(headers.get(1)))
						&& (ApplicationConstants.Spec.equalsIgnoreCase(headers.get(2)))
						&& (ApplicationConstants.Uom.equalsIgnoreCase(headers.get(3)))
						&& (ApplicationConstants.Quantity.equalsIgnoreCase(headers.get(4)))
						&& (ApplicationConstants.Remarks.equalsIgnoreCase(headers.get(5)))) {
					valid = true;
				}

			}
		} else {
			// It will throw exception when file format is not being matched
			valid = false;

			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					String.format(ApplicationConstants.FILE_FORMAT_NOT_MATCHED_DATA,
							ApplicationConstants.REQUIRED_FORMAT_RFQITEMFORMAT),
					ApplicationConstants.SERVICE_LEVEL_EXCEPTION, ApplicationConstants.FAILURE);

		}

		return valid;
	}

	private static boolean isRowEmpty(Row row) {
		boolean isEmpty = true;
		DataFormatter dataFormatter = new DataFormatter();

		if (row != null) {
			for (Cell cell : row) {
				if (dataFormatter.formatCellValue(cell).trim().length() > 0) {
					isEmpty = false;
					break;
				}
			}
		}

		return isEmpty;
	}
	
	@Override
	public boolean queryMail(User user) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		logger.info("Entered To Send mail To");
		if (user != null) {
			String toEmail = "info@procucev.com";
			InternetAddress add = new InternetAddress(mailFom, "Procucev Notifications");
			logger.info("Sending mail to::" + toEmail);
			MailUtility.mailingGMTClientRFQMailToinfoTeam("Sending GMT Query Successfull", toEmail, javaMailSender, add,
					host, user);

			logger.info("Completed sending email");
			return true;
		} else {
			logger.error("No clients available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}
	@Override
	public List<GMTRfqVendorDto> getAllGMTRfq(Organization org) {
		logger.info("Entered to get all Gmt Rfq's");
		MasterStatus status = masterStatusDao.findByStatus(StatusConstants.CM_RFQ_ACCEPTED);
		List<Rfq> rfqList = rfqDao.findAllRfqNoPr(status);
		// TODO Auto-generated method stub
		List<GMTRfqVendorDto> gmtRfqList = new ArrayList<>();
		if (!CollectionUtils.isEmpty(rfqList)) {
			for (Rfq rfq : rfqList) {
				GMTRfqVendorDto gmtRfqVendorDto = new GMTRfqVendorDto();
				gmtRfqVendorDto.setId(rfq.getId());
				gmtRfqVendorDto.setRfqId(rfq.getRfqId());
				gmtRfqVendorDto.setDesc(rfq.getProjectDesc());
				gmtRfqVendorDto.setClosureDate(rfq.getRfqClosingDate());
				gmtRfqVendorDto.setCreatedTS(rfq.getCreatedTS());
				gmtRfqVendorDto.setDivision(rfq.getDivision());
				gmtRfqVendorDto.setCategory(rfq.getCategory());
				gmtRfqVendorDto.setDeliveryDate(rfq.getDeliveryDate());
				gmtRfqVendorDto.setCategory(rfq.getCategory());
				gmtRfqVendorDto.setUserId(rfq.getUser());
				if (!(rfq.getClientdeliverylocationrfq()).isEmpty()) {
					gmtRfqVendorDto.setDeliveryLocation(rfq.getClientdeliverylocationrfq().get(0).getCity());
				}
				GmtRfqVendors existsByOrgAndRfq = gmtRfqVendorDao.findByVendorAndRfq(org, rfq);
				if (existsByOrgAndRfq != null) {
					logger.info("Checking whether rfq exists in vendor list vendor::", org.getId());
					gmtRfqVendorDto.setStatus(existsByOrgAndRfq.getStatus());
					gmtRfqVendorDto.setRequestedDate(existsByOrgAndRfq.getRequestedDate());
					gmtRfqVendorDto.setAcceptedDate(existsByOrgAndRfq.getAcceptedDate());
					if (existsByOrgAndRfq.getQuery() != null) {
						gmtRfqVendorDto.setQuery(existsByOrgAndRfq.getQuery());
					}
				} else {
					logger.info("No rfq exists in vendor");
					MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.vendorRfqNew);
					gmtRfqVendorDto.setStatus(resultStatus);

				}
				logger.info("Completed and Returning Respone");
				gmtRfqList.add(gmtRfqVendorDto);
			}
			return gmtRfqList;
		} else {
			logger.error("No Rfqs available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}

	@Override
	public boolean requestRfqByVendors(List<GmtRfqVendors> rfq) {
		// TODO Auto-generated method stub

		logger.info("Entered to request Rfqs By Vendors:: ");
		try {
			Date date = new Date();
			logger.info("Date is ==>" + date);
			for (GmtRfqVendors gmtRfqVendors : rfq) {
				MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.vendorRfqRequested);

				GmtRfqVendors gmtRfq = gmtRfqVendorDao.findByVendorAndRfq(gmtRfqVendors.getVendor(),
						gmtRfqVendors.getRfq());
				if (gmtRfq != null) {
					logger.info("Updating Status to Requested as record already exists");
					gmtRfqVendorDao.updateStatus(gmtRfqVendors.getRfq(), gmtRfqVendors.getVendor(), resultStatus);
				} else {
					// Save each GmtRfqVendors object using gmtRfqVendorDao
					logger.info("Saving status to requested First Time it is requesting");
					gmtRfqVendors.setStatus(resultStatus);
					gmtRfqVendors.setRequestedDate(date);
					gmtRfqVendorDao.save(gmtRfqVendors);
				}
				rfqDao.updateCount(gmtRfqVendors.getRfq());

			}
			logger.info("All RFQs requested by vendors are saved successfully.");
			return true;
		} catch (Exception e) {
			logger.error("Error occurred while saving RFQs by vendors: " + e.getMessage());
			return false;
		}

	}


	/**
	 * Retrieves vendors associated with a given RFQ.
	 * 
	 * @param rfq The RFQ object.
	 * @return List of GMT RFQ vendors.
	 * @throws AppException if no vendors are found for the RFQ.
	 */
	@Override
	public List<GmtRfqVendors> getVendorsByGmtRfq(Rfq rfq) {
		logger.info("Entered to get GMT Vendors By RFQ {}", rfq.getId());

		if (rfq == null) {
			logger.error("RFQ object is null.");
			throw new IllegalArgumentException("RFQ object cannot be null.");
		}

		List<GmtRfqVendors> response = gmtRfqVendorDao.findByRfq(rfq);
		List<RfqVendor> rfqVendors = getVendorsbyRFQ(rfq); // Call the method to get RfqVendor
																					// list
		MasterStatus forwardedStatus = masterStatusDao.findByStatus(StatusConstants.RFQ_FORWARDED);
		logger.info("Size of rfqVendors==", rfqVendors.size());
		if (!rfqVendors.isEmpty()) {
			// Add RfqVendor objects to the response
			for (RfqVendor rfqVendor : rfqVendors) {
				GmtRfqVendors gmtRfqVendor = new GmtRfqVendors();
				// gmtRfqVendor.setId(rfqVendor.getId());
				gmtRfqVendor.setVendorName(rfqVendor.getCompanyName());
				gmtRfqVendor.setVendorUuid(rfqVendor.getVendorId());
				gmtRfqVendor.setVendorId(rfqVendor.getCompanyId());
				gmtRfqVendor.setOtherEmails(rfqVendor.getOtherEmails());
				gmtRfqVendor.setStatus(forwardedStatus);
				// Set other fields according to GmtRfqVendors object
				// You may need to adjust fields based on GmtRfqVendors properties
				response.add(gmtRfqVendor);
			}
		}
		if (!response.isEmpty()) {
			logger.info("Found {} vendors for RFQ {}", response.size(), rfq.getId());
			return response;
		} else {
			logger.error("No vendors available in the database for RFQ {}", rfq.getId());
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	@Override
	public boolean approveVendor(GmtRfqVendors gmtRfq) throws MessagingException {
		logger.info("Entered to approve vendor by CM2");
		if (gmtRfq != null) {
			Date date = new Date();
			logger.info("Date is ==>" + date);
			logger.info("Entered to approve Vendor For RFQ");
			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.vendorApproved);
			gmtRfqVendorDao.updateAcceptStatus(gmtRfq.getRfq(), gmtRfq.getVendor(), resultStatus, date);
			rfqDao.updateRfqCount(gmtRfq.getRfq());
			// Add logic to update rfqcredit and rfq used count
			Optional<Rfq> rfq = rfqDao.findById(gmtRfq.getRfq().getId());
			// Organization org = orgdao.findById(gmtRfq.getVendor().getId());
			String email = orgDao.findEmailById(gmtRfq.getVendor().getId());
			String otherEmails = null;
			otherEmails = orgDao.findOtherEmailById(gmtRfq.getVendor().getId());

			if (rfq.isPresent()) {
				String rfqDueDate = buildingRfqDueDate();
				MailUtility.emailNewGMTRfqForNoPR("NewRfq", javaMailSender, rfq.get(), host, email, otherEmails,
						mailFom, emailPassword, rfqDueDate);

			}
			return true;
		}
		else {
			return false;
		}
		

	}

	private String buildingRfqDueDate() {
		logger.info("Entered into building Rfq Due Date");
		LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata")); // Set time zone to IST

		// Add one day
		LocalDate nextDate = currentDate.plusDays(2);

		// Set time to 1:30 PM
		LocalTime time = LocalTime.of(13, 30);

		// Combine date and time
		LocalDateTime nextDateTime = LocalDateTime.of(nextDate, time);

		// Format the date and time
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH);
		String formattedDateTime = nextDateTime.format(formatter);
		logger.info("Rfq Due Date after format: " + formattedDateTime);
		return formattedDateTime;
	}

	@Override
	public boolean ignoreRfqByVendor(List<GmtRfqVendors> rfq) {
		// TODO Auto-generated method stub
		logger.info("Entered to Ignore Rfqs By Vendors:: ");
		try {
			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.VENDOR_RFQ_IGNORED);

			for (GmtRfqVendors gmtRfqVendors : rfq) {
				GmtRfqVendors gmtRfq = gmtRfqVendorDao.findByVendorAndRfq(gmtRfqVendors.getVendor(),
						gmtRfqVendors.getRfq());

				if (gmtRfq != null) {
					logger.info("Updating Ignore Status As Record Already Exists");
					gmtRfqVendorDao.updateStatus(gmtRfqVendors.getRfq(), gmtRfqVendors.getVendor(), resultStatus);
				} else {
					// Save each GmtRfqVendors object using gmtRfqVendorDao

					gmtRfqVendors.setStatus(resultStatus);
					gmtRfqVendorDao.save(gmtRfqVendors);
				}
			}
			logger.info("All RFQs Ignored  by vendors successfully.");
			return true;
		} catch (Exception e) {
			logger.error("Error occurred while Ignoring RFQs by vendors: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean rejectRfqForVendor(GmtRfqVendors rfq) {
		// TODO Auto-generated method stub
		logger.info("Entered to Reject Rfqs By Vendors:: ");
		MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.VENDORREJECTRFQ);
		try {
			GmtRfqVendors gmtRfq = gmtRfqVendorDao.findByVendorAndRfq(rfq.getVendor(), rfq.getRfq());
			if (gmtRfq != null) {
				logger.info("Updating Reject Status As Record Already Exists");
				gmtRfqVendorDao.updateStatus(rfq.getRfq(), rfq.getVendor(), resultStatus);
			} else {
				// Save each GmtRfqVendors object using gmtRfqVendorDao
				logger.info(" Saving Record For First Time ");
				rfq.setStatus(resultStatus);
				gmtRfqVendorDao.save(rfq);
				rfqDao.updateRfqCount(rfq.getRfq());
				logger.info("RFQ Rejected successfully.");
			}
			return true;
		} catch (Exception e) {
			logger.error("Error occurred while rejecting vendor RFQs by CM2: " + e.getMessage());
			return false;
		}
	}

	@Override
	public List<RfqDTO> getAllRfqForCM() {
		logger.info("Entered to Get RFQs For PR Flag True");

		MasterStatus status = masterStatusDao.findByStatus(StatusConstants.CM_RFQ_ACCEPTED);

		List<Rfq> rfqsList = rfqDao.findAllRfqNoPrByCM(status);
		if (rfqsList.isEmpty()) {
			logger.warn("No RFQs found for PR Flag True");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		logger.info("Found {} RFQs for PR Flag True", rfqsList.size());

		return rfqsList.stream().map(this::mapToDto).collect(Collectors.toList());
	}

	/**
	 * Maps an RFQ entity to its corresponding DTO.
	 * 
	 * @param rfq The RFQ entity.
	 * @return The RFQ DTO.
	 */
	private RfqDTO mapToDto(Rfq rfq) {
		RfqDTO rfqDto = new RfqDTO();
		rfqDto.setId(rfq.getId());
		rfqDto.setCreatedBy(rfq.getCreatedBy());
		rfqDto.setCreatedTs(rfq.getCreatedTS());
		rfqDto.setProjectDesc(rfq.getProjectDesc());
		rfqDto.setCategory(rfq.getCategory());
		rfqDto.setRfqId(rfq.getRfqId());
		rfqDto.setDeliveryDate(rfq.getDeliveryDate());
		rfqDto.setByClient(rfq.isByClient());
		rfqDto.setCount(rfq.getCount());
		rfqDto.setQuotationReceived(rfq.isQuotationReceived());
		return rfqDto;
	}

	@Override
	public boolean raiseQueryByVendor(GmtRfqVendors rfq) {
		// TODO Auto-generated method stub
		logger.info("Entered To Raise Query By Vendor For Rfq");
		try {
			if (rfq != null)

			{
				GmtRfqVendors gmtRfq = gmtRfqVendorDao.findByVendorAndRfq(rfq.getVendor(), rfq.getRfq());
				if (gmtRfq != null) {
					logger.info("Updating Query As Record Already Exists");
					gmtRfqVendorDao.updateQuery(rfq.getRfq().getId(), rfq.getVendor().getId(), rfq.getQuery());
				} else {

					logger.info("Saving Query Record For First Time With New Status");
					MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.vendorRfqNew);
					rfq.setStatus(resultStatus);
					gmtRfqVendorDao.save(rfq);
				}
				return true;
			} else {
				logger.warn("No Data Found");
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
						ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Maps an RFQ entity to its corresponding DTO.
	 * 
	 * @param rfq The RFQ entity.
	 * @return The RFQ DTO.
	 */
	private RfqDTO mapToClientDto(Rfq rfq) {
		RfqDTO rfqDto = new RfqDTO();
		rfqDto.setId(rfq.getId());
		rfqDto.setCreatedBy(rfq.getCreatedBy());
		rfqDto.setCreatedTs(rfq.getCreatedTS());
		rfqDto.setProjectDesc(rfq.getProjectDesc());
		rfqDto.setDivision(rfq.getDivision());
		rfqDto.setClientStatus(rfq.getClientStatus());
		rfqDto.setRfqId(rfq.getRfqId());
		String phone = userDao.findPhoneByUser(rfq.getUser());
		if (phone != null) {
			rfqDto.setPhoneNumber(phone);
		}

		String compantName = userDao.findByUser(rfq.getUser());
		if (compantName != null) {
			rfqDto.setCompanyName(compantName);
		}
		return rfqDto;
	}


	@Override
	public List<RfqDTO> fetchAllClientGMTRfqsForCM() {
		logger.info("Entered to fetch all client GMT RFQ for CM");

		List<Rfq> rfqsList = rfqDao.findAllClientRfqNoPr();
		if (rfqsList.isEmpty()) {
			logger.warn("No RFQs found  by client flag True");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		logger.info("Found {} RFQs for PR Flag True", rfqsList.size());

		return rfqsList.stream().map(this::mapToClientDto).collect(Collectors.toList());

	}
	
	@Override
	public List<RfqVendor> getVendorsbyRFQ(Rfq rfq) {
		logger.info("Entered To Get Vendors By RFQ");
		Optional<Rfq> rfqList = rfqDao.findById(rfq.getId());
		List<RfqVendor> rfqvendor = rfqList.get().getRfqVendor();
		List<RfqVendor> rfqvendorresult = new ArrayList<RfqVendor>();
		if (!CollectionUtils.isEmpty(rfqvendor)) {
			for (RfqVendor rfqVendor2 : rfqvendor) {
				RfqVendor response = new RfqVendor();
				response.setCompanyName(rfqVendor2.getOrganization().getCompanyName());
				response.setVendorResponseDate(rfqVendor2.getVendorResponseDate());
				response.setVendorId(rfqVendor2.getOrganization().getId());
				response.setCompanyId(rfqVendor2.getOrganization().getCompanyId());
				response.setId(rfqVendor2.getId());
				if (rfqVendor2.getOrganization().getOtherEmails() != null) {
					response.setOtherEmails(rfqVendor2.getOrganization().getOtherEmails());
				}
				List<User> users = userDao.findByOrg(rfqVendor2.getOrganization());
				// List<Poc> contact = pocdao.findByOrganization(rfqVendor2.getOrganization());
				if (!users.isEmpty()) {
					response.setEmail(users.get(0).getUsername());
					response.setPhone(users.get(0).getPhone());

				} else {
					response.setEmail(rfqVendor2.getOrganization().getEmail());
					response.setPhone(rfqVendor2.getOrganization().getOrganizationPhonenumber());

				}
				response.setVendorStatus(rfqVendor2.getVendorStatus());
				response.setStatus(rfqVendor2.getStatus());
				rfqvendorresult.add(response);
			}
			return rfqvendorresult;
		} else {
			logger.info("No RFQ Vendors");
		}
		return rfqvendorresult;
	}
	@Override
	public List<String> getCategoryByDivision(CategoryDivision cat) {
		// TODO Auto-generated method stub
		logger.info("Entered To Get Category List By Division");
		List<String> categoryList = categoryDivisionDao.getCategoryByDivision(cat.getDivision());
		if (categoryList != null && !CollectionUtils.isEmpty(categoryList)) {
			logger.info("Completed and returning response");
			return categoryList;
		} else {
			logger.error("No categories found for given division");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);

		}
	}

	@Override
	public List<GmtItems> getAllGmtItems() {
		// TODO Auto-generated method stub
		logger.info("Entered to get all Gmt Items");
		List<GmtItems> gmtItemsList = gmtItemsDao.findAll();
		if (!CollectionUtils.isEmpty(gmtItemsList) && gmtItemsList != null) {
			return gmtItemsList;
		} else {
			logger.error("No Gmt Items Found");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);

		}
	}
	/**
	 * Return rfq details based on the ID
	 */
	@Override
	public ResponseEntity<?> fetchRfqById(Rfq rfq) {
		Optional<Rfq> rfqList = rfqDao.findById(rfq.getId());
		if (rfqList.isPresent()) {
			return new ResponseEntity<>(rfqList.get(), HttpStatus.OK);
		} else {
			logger.error("No rfq's available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}
	
	@Override
	public List<String> getAllCategory() {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		logger.info("Entered To Get All Category");
		List<String> categoryList = categoryDivisionDao.getAllCategory();
		if (categoryList != null && !CollectionUtils.isEmpty(categoryList)) {
			logger.info("Completed and returning response");
			return categoryList;
		} else {
			logger.error("No categories found");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);

		}
	}

	@Override
	public List<RfqDTO> getRFQsForNoPR() {
		logger.info("Entered To Get RFQ's For PR Flag True");
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = userDetails.getUsername();
		if (userDetails.getUsername() != null) {
			logger.info("username---" + username);
			String fullName = userDao.getFullName(username);
			logger.info("fullname---" + fullName);
			List<RfqDTO> rfqDtoList = new ArrayList<>();

			List<Rfq> rfqsList = rfqDao.getRfqsByNoPrFlagIsTrue(fullName);

			// Fetch all emails once
//	    Map<String, Integer> replyCounts = fetchReplyCountsForRFQs(rfqsList);

			// Iterate over RFQs and populate DTOs
			for (Rfq rfq : rfqsList) {
				RfqDTO rfqDto = new RfqDTO();
				rfqDto.setId(rfq.getId());
				rfqDto.setCreatedBy(rfq.getCreatedBy());
				rfqDto.setCreatedTs(rfq.getCreatedTS());
				rfqDto.setProjectDesc(rfq.getProjectDesc());
				rfqDto.setCategory(rfq.getCategory());
				rfqDto.setRfqId(rfq.getRfqId());
				logger.info("RfqID--", rfq.getRfqId());
				long vendorsCount = rfqVendorDao.findByVendorsByRfq(rfq.getId());
				rfqDto.setNoOfVendors(vendorsCount);
				// int countReplyEmails = replyCounts.getOrDefault(rfq.getRfqId(), 0); //
				// Retrieve reply count from the map
				// log.info("Count of replies", countReplyEmails);
				// rfqDto.setNoOfQuotes(countReplyEmails);
				rfqDtoList.add(rfqDto);
			}
			logger.info("Completed and Returning Response");
			return rfqDtoList;
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}
	@Override
	public List<VendorRFQDto> getAllVendors() {
		// TODO Auto-generated method stub
		logger.info("Entered To Get All Vendor");
		List<VendorRFQDto> responseList = new ArrayList<>();
		List<Object[]> vendorsList = orgDao.getAllVendor();
		if (!CollectionUtils.isEmpty(vendorsList) && vendorsList != null) {
			vendorsList.stream().forEach(org -> {
				VendorRFQDto vendorRFQDto = new VendorRFQDto();
				vendorRFQDto.setId(String.valueOf(org[0]));
				vendorRFQDto.setCompanyName(String.valueOf(org[1]));
				vendorRFQDto.setVendorId(String.valueOf(org[2]));
				vendorRFQDto.setMobileNo(String.valueOf(org[3]));
				vendorRFQDto.setCity(String.valueOf(org[4]));
				vendorRFQDto.setEmail(String.valueOf(org[5]));

				responseList.add(vendorRFQDto);

			});
			logger.info("Completed and Returning response");
			return responseList;
		} else {
			logger.error("No Vendors available in the Database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	@Override
	public List<VendorRFQDto> getAllVendorsByCategory(Organization organization) {
		// TODO Auto-generated method stub
		logger.info("Entered To Get All Vendor");
		List<VendorRFQDto> responseList = new ArrayList<>();
		List<Object[]> vendorsList = orgDao.getAllVendorByCategory(organization.getVendorcategory());
		if (!CollectionUtils.isEmpty(vendorsList) && vendorsList != null) {
			vendorsList.stream().forEach(org -> {
				VendorRFQDto vendorRFQDto = new VendorRFQDto();
				vendorRFQDto.setId(String.valueOf(org[0]));
				vendorRFQDto.setCompanyName(String.valueOf(org[1]));
				vendorRFQDto.setVendorId(String.valueOf(org[2]));
				vendorRFQDto.setMobileNo(String.valueOf(org[3]));
				vendorRFQDto.setCity(String.valueOf(org[4]));
				vendorRFQDto.setEmail(String.valueOf(org[5]));

				responseList.add(vendorRFQDto);

			});
			logger.info("Completed and Returning response");
			return responseList;
		} else {
			logger.error("No Vendors available in the Database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}
	
	@Override
	public boolean createRFQWithNoPr(Rfq rfq) {

		logger.info("Request recieved  for RFQ creation with No PR " + rfq.toString());
		boolean status = false;

		// Get PR Status for PR Inprogress
		MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);

		// Set Procucev status to RFQ
		rfq.setStatus(resultStatus);
		try {
			// Get Total counts for PR
			//String rfqId = selfRegistrationService.generateId("RFQ");
			String rfqId = generateRfqId("RFQ");
			logger.info("Generated RFQ Id{}", rfqId);
			rfq.setRfqId(rfqId);

//			if (rfq.getBoqfile() != null && rfq.getBoqfile().length > 0) {
//				
//				List<RfqItem> rfqItems = processBOQFiles(rfq.getBoqfile());
//				rfqItemValidation(rfqItems);
//				rfq.setRfqItem(rfqItems);
//			} else {
			List<RfqItem> rfqItem = rfq.getRfqItem();
			//rfqItemValidation(rfqItem);

			Rfq savedRfq = rfqDao.save(rfq);

			status = saveVendorsForRfq(rfq, savedRfq);
		} catch (Exception e) {
			e.printStackTrace();
			return status;
		}
		return status;
	}


	private boolean saveVendorsForRfq(Rfq rfq, Rfq savedRfq) {
		logger.info("Entered to save Vendors For RFQ");
		List<RfqVendor> rfqVendors = new ArrayList<>();
		List<Organization> vendorList = rfq.getVendors();
		boolean status = false;

		if (!CollectionUtils.isEmpty(vendorList)) {
			vendorList.forEach(vendor -> {
				Organization savedVendor;

				if (vendor.getId() == null) {
					// Save the new organization
					OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
					MasterStatus vendorStatus = masterStatusDao.findByStatus(StatusConstants.VENDOR_ADDED);
					MasterStatus evalStatus = masterStatusDao.findByStatus(StatusConstants.EVALUATION_NOT_STARTED);

					if (selfRegistrationService.checkOrgexist(vendor.getCompanyName())) {
						// Exception occurs when User is already associated to an Account/registered
						throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
								"Vendor Already Registered with name " + vendor.getCompanyName(),
								ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
					}

					vendor.setOrgType(orgTypeObject);
					vendor.setVendorStatus(vendorStatus);
					vendor.setStatus(evalStatus);
					vendor.setVendorcategory(savedRfq.getCategory());
					savedVendor = orgDao.save(vendor);
				} else {
					// Use the existing organization
					logger.info("Saving the existing vendor with other email");
					savedVendor = vendor;
					if (vendor.getOtherEmails() != null) {
						orgDao.updateOtherEmail(vendor.getOtherEmails(), vendor.getId());
					}
				}

				// Build and save the RfqVendor object
				// Build the RfqVendor object
				savedVendor = orgDao.findById(savedVendor.getId()).get();
				RfqVendor rfqVendor = new RfqVendor();
				logger.info("setting vendor id to rfqvendor{}", savedVendor.getId());
				rfqVendor.setOrganization(savedVendor);
				rfqVendor.setRfq(savedRfq);

				// Collect RfqVendor object
				rfqVendors.add(rfqVendor);
			});
		}

		// here need to set rfqvendors object
		try {
			sendRfqToVendors(rfqVendors, savedRfq);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// sendRfqToVendors(rfq.getRfqVendors());
		logger.info("RFQ Created Successfully" + rfq.toString());
		status = true;

		return status;
	}
	public boolean sendRfqToVendors(List<RfqVendor> rfq, Rfq rfqData) throws MessagingException {
		logger.info("Entered to sendRfqToVendors()");
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = userDetails.getUsername();
		String[] mailIdWrapper = new String[1]; // Using an array to wrap mailId
		String[] passwordWrapper = new String[1];

		String rfqDueDate = buildingRfqDueDate();
		logger.info("username-->", username);
		EmailUser res = emailUserRepo.findByEmail(username);
		if (res != null) {
			mailIdWrapper[0] = res.getEmail();
			passwordWrapper[0] = res.getPassword();
		} else {
			mailIdWrapper[0] = mailFom;
			passwordWrapper[0] = emailPassword;
		}
		logger.info("Mail is sending from::", mailIdWrapper[0]);
		MasterStatus pcRfqSent = null, vendorRfqNew = null;
		List<String> inputStatus = new ArrayList<>();
		List<RfqVendor> sendVendors = new ArrayList<>();
		inputStatus.add(StatusConstants.pcRfqSent);
		inputStatus.add(StatusConstants.vendorRfqNew);

		List<MasterStatus> statusList = masterStatusDao.findByStatusIn(inputStatus);
		if (!CollectionUtils.isEmpty(statusList)) {
			for (MasterStatus status : statusList) {
				if (status.getStatus().equals(StatusConstants.pcRfqSent)) {
					pcRfqSent = status;
				}
				if (status.getStatus().equals(StatusConstants.vendorRfqNew)) {
					vendorRfqNew = status;
				}
			}
		}

		List<RfqVendor> vendorList = rfq;
		for (RfqVendor vendor : vendorList) {
			Optional<Rfq> rfqs = rfqDao.findById(vendor.getRfq().getId());
			vendor.setRfqId(rfqs.get().getRfqId());
			vendor.setProcucevStatus(pcRfqSent);
			vendor.setVendorStatus(vendorRfqNew);
			vendor.setVendorResponseDate(vendor.getVendorResponseDate());
			sendVendors.add(vendor);
		}
		try {
			logger.info("Saving data in RFQ Vendors");
			rfqVendorDao.saveAll(sendVendors);
			List<Organization> vendors = rfqData.getVendors();
			User users = userDao.findByUsernameAndActive(username, true);
			final String phoneNumber; // Declare phoneNumber as final
			final String fullName;

			if (users != null) {
				phoneNumber = users.getPhone();
				fullName = users.getFullName();
				logger.info("Phone number of user", phoneNumber);
				logger.info("Full Name: ", fullName);
			} else {
				phoneNumber = null; // Initialize phoneNumber
				fullName = null;
			}

			logger.info("Size of vendors List-->" + vendors.size());
			vendors.forEach(vendor -> {
				try {
					MailUtility.emailNewRfqForNoPR("NewRfq", javaMailSender, rfqData, host, vendor.getEmail(), username,
							vendor.getOtherEmails(), phoneNumber, rfqDueDate, fullName, mailIdWrapper[0],
							passwordWrapper[0]);
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			return true;
		} catch (Exception e) {
			return false;
		}

	}
	@Override
	public boolean forwardRfqForNoPr(Rfq rfq) {
		logger.info("Entered to forwardRfqForNoPr{}");
		try {
			if (rfq.getId() != null) {
				Optional<Rfq> rfqData = rfqDao.findById(rfq.getId());
				if (rfqData.isPresent()) {
					Rfq rfqResponse = rfqData.get();
					rfqResponse.setVendors(rfq.getVendors());
					saveVendorsForRfq(rfq, rfqResponse);
				} else {
					logger.info("No RFQ's Found");
				}

			}
			return true;
		} catch (Exception e) {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.No_RFQ_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);

		}
		// TODO Auto-generated method stub

	}
	
	@Override
	public void emailForwarder() {
	    logger.info("Entered into emailForwarder()");
	    String subjectPattern = "You have an Enquiry RFQ No";

	    Properties properties = new Properties();
	    properties.put("mail.store.protocol", "imaps");
	    properties.put("mail.imaps.host", "imap.gmail.com");
	    properties.put("mail.imaps.port", "993");
	    properties.put("mail.imaps.ssl.enable", "true");
	    properties.put("mail.imaps.ssl.trust", "imap.gmail.com");

	    Session emailSession = Session.getInstance(properties);

	    try (Store store = emailSession.getStore("imaps")) {
	        store.connect(mailFom, emailPassword);

	        Folder inbox = store.getFolder("INBOX");
	        inbox.open(Folder.READ_WRITE);

	        Message[] allMessages = inbox.getMessages();
	        logger.info("Total messages in inbox: " + allMessages.length);

	        // Search for messages containing the specified subject pattern
	        Message[] messages = inbox.search(new SubjectTerm(subjectPattern));
	        logger.info("Total messages with Subject Term: " + messages.length);

	        Flags customFlag = new Flags("Processed");

	        // Forward matching messages to the specified email address
	        for (Message message : messages) {
	            // Check if the message has already been processed
	            if (!message.getFlags().contains(customFlag)) {
	            	logger.info("Sending Mail To User Updating Flag");
	                String subject = message.getSubject();
	                String rfqId = extractRfqId(subject);
	                if (rfqId != null) {
	                	logger.info("RFQID===>" + rfqId);
	                    // Forward matching messages to the specified email address
	                    rfqDao.updateRfqByRfqId(rfqId);
	                    // Use the RFQ ID to retrieve client address and forward the message
	                    List<String> users = rfqDao.findRFQByRfQId(rfqId);
	                    if (users != null && !CollectionUtils.isEmpty(users)) {
	                        String userId = users.get(0);
	                        if (userId != null) {
	                            String forwardAddress = userDao.findEmailById(users.get(0));
	                            logger.info("forward address======>" + forwardAddress);
	                            MailUtility.forwardMessage(forwardAddress, javaMailSender, mailFom, message);

	                            // Mark the message as processed
	                            message.setFlags(customFlag, true);
	                        }
	                    } else {
	                    	logger.info("user object is empty");
	                    }
	                }
	            }
	        }

	        // Close the folders and store
	        inbox.close(false);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	private static String extractRfqId(String subject) {
		Pattern pattern = Pattern.compile("RFQ No ([A-Za-z0-9]+)");
		Matcher matcher = pattern.matcher(subject);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
	
	@Override
	public List<RfqItem> getItemsbyrfqrid(Rfq rfq) {
		// Optional<Rfq> rfqList = rfqdao.findById(rfq.getId( ))
		List<RfqItem> rfqItems = rfqItemsDao.findByRfq(rfq);

		if (!CollectionUtils.isEmpty(rfqItems)) {
			return rfqItems;
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}
	
	@Override
	public List<Organization> fetchSelfRegisterClients() {
		logger.info("Entered To Fetch Self Register Clients");

		// Fetch the organization type object for clients
		OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);

		// Fetch the list of clients with the specified org type and self-client flag,
		// sorted by created timestamp
		List<Organization> clientList = orgDao.findByOrgTypeAndSelfClient(orgTypeObject, true,
				Sort.by(Sort.Direction.DESC, "createdTS"));

		if (!CollectionUtils.isEmpty(clientList)) {

			logger.info("Returning Client List Response size: {}", clientList.size());
			return clientList;
		} else {
			logger.error("No clients available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}
	
	@Override
	public boolean editUser(User user) {
		// TODO Auto-generated method stub
		logger.info("Entered To Edit User");
		if (user != null) {
			// User with new email we are changing status to new if it is accepted/ignored
			if (!user.isEmailMatched()) {
				MasterStatus status = masterStatusDao.findByStatus(StatusConstants.CLIENT_NEW);
				userDao.updateUserDetailsAndStatus(user.getId(), user.getUsername(), user.getFullName(),
						user.getPhone(), status);
			} else {
				userDao.updateUserDetails(user.getId(), user.getUsername(), user.getFullName(), user.getPhone());
			}
			String orgId = userDao.findOrgIdByUser(user.getId());
			orgDao.updateEmailByOrg(orgId, user.getUsername());
			logger.info("Updated Client Details Successfully");
			return true;
		} else {
			logger.error("No clients available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	@Override
	public boolean acceptSelfClient(User user) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		logger.info("Entered To Accept Self Client");
		if (user != null) {
			Optional<User> userData = userDao.findById(user.getId());
			Organization org = userData.get().getOrg();
			if (org != null) {
				logger.info("Updating Client Status To Approved");
				MasterStatus status = masterStatusDao.findByStatus(StatusConstants.CLIENT_USER_APPROVED);
				orgDao.updateClientStatus(status, org.getId());
			}
			logger.info("Updating Client User Status To Accepted");
			MasterStatus status = masterStatusDao.findByStatus(StatusConstants.USER_ACCEPTED);
			if (userData.isPresent()) {
				userDao.updateClientStatus(user, status);
				InternetAddress add = new InternetAddress(mail, "<DO-NOT-REPLY>");
				MailUtility.mailingVerificationLinkWithSelfUserLogin(javaMailSender, mail, add, pswd, host,
						userData.get());
			}
			return true;
		} else {
			logger.error("No clients available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}

	@Override
	public boolean ignoreClient(User user) {
		// TODO Auto-generated method stub
		logger.info("Entered To Ignore Self Client");
		if (user != null) {
			Optional<User> userData = userDao.findById(user.getId());
			MasterStatus status = masterStatusDao.findByStatus(StatusConstants.USER_IGNORED);
			if (userData.isPresent()) {
				userDao.updateClientStatus(user, status);
			}
			return true;
		} else {
			logger.error("No clients available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}
	@Override
	public boolean disableUser(User user) {
		// TODO Auto-generated method stub
		logger.info("Entered To Disable User");

		Optional<User> userfound = userDao.findById(user.getId());
		if (userfound.isPresent()) {
			userDao.deactiveUser(user.getId());
			logger.info("Deactivated User");
			String orgId = userDao.findOrgIdByUser(user.getId());
			List<String> emails = userDao.findByOrg(orgId);
			if(!emails.isEmpty() && emails!=null) {
				 orgDao.updateEmailByOrg(orgId,emails.get(0));
			}
			else {
				String email=null;
				orgDao.updateEmailByOrg(orgId,email);
			}
			return true;
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}
	@Override
	public List<User> getclientusersByClientId(Organization org) {
		logger.info("Entered to getclientusersByClientId");
		List<User> usersList = userDao.findByOrgAndActive(org.getId());
		if (usersList != null) {
			logger.info("Eompleted and returning response");
			return usersList;
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}
	@Override
	public boolean editAndResendRfq(Rfq updatedRfq) throws MessagingException {
		// TODO Auto-generated method stub
		logger.info("Request received for editing RFQ {}", updatedRfq.getRfqId());

		try {
			// Fetch the existing RFQ from the database
			Rfq existingRfq = rfqDao.findById(updatedRfq.getId()).orElse(null);
			if (existingRfq == null) {
				logger.error("RFQ with ID {} not found", updatedRfq.getId());
				return false; // RFQ not found
			}
			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);

			MasterStatus acceptStatus = masterStatusDao.findByStatus(StatusConstants.CM_RFQ_ACCEPTED);
			updatedRfq.setClientStatus(acceptStatus);

			updatedRfq.setByClient(true);
			updatedRfq.setStatus(resultStatus);

			// Update RFQ items
			List<RfqItem> updatedRfqItems = updatedRfq.getRfqItem();
			List<GmtItems> existingGmtItems = gmtItemsDao
					.findByRfqItemIdIn(updatedRfqItems.stream().map(RfqItem::getId).collect(Collectors.toList()));

			// Map existing GmtItems to their corresponding RfqItem IDs for efficient
			// updates
			Map<String, GmtItems> rfqItemIdToGmtItemMap = existingGmtItems.stream()
					.collect(Collectors.toMap(GmtItems::getRfqItemId, Function.identity()));

			// Iterate over updated RFQ items
			for (RfqItem updatedItem : updatedRfqItems) {
				GmtItems existingGmtItem = rfqItemIdToGmtItemMap.get(updatedItem.getId());
				if (existingGmtItem != null) {
					// Update existing GmtItem with new values from updated RFQ item
					existingGmtItem.setBrand(updatedItem.getBrand());
					existingGmtItem.setDescription(updatedItem.getDescription());
					existingGmtItem.setQuantity(updatedItem.getQuantity());
					existingGmtItem.setRemarks(updatedItem.getRemarks());
					existingGmtItem.setUnitofMeasures(updatedItem.getUnitofMeasures());
					// Save or update the existing GmtItem
					gmtItemsDao.save(existingGmtItem);
				} else {
					// Handle the case where the item in the updated RFQ is new and not in existing
					// GmtItems
					// You may choose to create a new GmtItems entry here if necessary
					logger.info("No New Items To Be Added");

				}
			}
			// Set RFQ ID for RFQ items

			if (!CollectionUtils.isEmpty(updatedRfqItems) && updatedRfqItems != null) {
				updatedRfqItems.forEach(rfqItem -> rfqItem.setRfq(updatedRfq));
			}

			// Set RFQ ID for documents
			List<RFQDocument> updatedRFQDocuments = updatedRfq.getRfqDocument();
			if (!CollectionUtils.isEmpty(updatedRFQDocuments) && updatedRFQDocuments != null) {
				updatedRFQDocuments.forEach(rfqDocument -> rfqDocument.setRfq(updatedRfq));
			}
			// Set RFQ Delivery Location
			List<ClientDeliveryLocationRfq> updatedDeliveryLocation = updatedRfq.getClientdeliverylocationrfq();
			if (!CollectionUtils.isEmpty(updatedDeliveryLocation) && updatedDeliveryLocation != null) {
				updatedDeliveryLocation.forEach(clientDeliveryLocation -> clientDeliveryLocation.setRfq(updatedRfq));
			}
			// Set RFQ Vendors
			List<RfqVendor> updatedRFQVendors = rfqVendorDao.findDataByRfqId(updatedRfq.getId());
			logger.info("Size of updatedRFQVendors{}" + updatedRFQVendors.size());
			if (!CollectionUtils.isEmpty(updatedRFQVendors) && updatedRFQVendors != null) {
				updatedRFQVendors.forEach(rfqVendor -> rfqVendor.setRfq(updatedRfq));
			}

			// Save the updated RFQ
			updatedRfq.setClientdeliverylocationrfq(updatedDeliveryLocation);
			updatedRfq.setRfqItem(updatedRfqItems); // Set the RFQ items directly to the RFQ object
			updatedRfq.setRfqDocument(updatedRFQDocuments);
			updatedRfq.setRfqVendor(updatedRFQVendors);

			// Save the updated RFQ
			rfqDao.save(updatedRfq);
			logger.info("Getting Vendors By RFQ Id");
			List<GmtRfqVendors> vendors = getVendorsByGmtRfq(updatedRfq);
			if (vendors != null && !CollectionUtils.isEmpty(vendors)) {
				logger.info("Vendors List Size to Resend Emails::", vendors.size());
				resendRfqToVendors(vendors, updatedRfq);
			} else {
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_VENDORS_FOUND,
						ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
			}
			logger.info("Vendors List Size", vendors.size());
			logger.info("Completed Editing RFQ {}", updatedRfq.getRfqId());

			return true;
		} catch (DataAccessException e) {
			logger.error("Error occurred while editing RFQ: {}", e.getMessage());
			return false;
		}
	}
	public boolean resendRfqToVendors(List<GmtRfqVendors> gmtVendors, Rfq rfqData) throws MessagingException {
		logger.info("Entered to sendRfqToVendors()");
		try {
			UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
					.getPrincipal();
			String username = userDetails.getUsername();
			String[] mailIdWrapper = new String[1]; // Using an array to wrap mailId
			String[] passwordWrapper = new String[1];

			String rfqDueDate = buildingRfqDueDate();
			logger.info("username-->", username);
			EmailUser res = emailUserRepo.findByEmail(username);
			if (res != null) {
				mailIdWrapper[0] = res.getEmail();
				passwordWrapper[0] = res.getPassword();
			} else {
				mailIdWrapper[0] = mailFom;
				passwordWrapper[0] = emailPassword;
			}
			logger.info("Mail is sending from::", mailIdWrapper[0]);

			User users = userDao.findByUsernameAndActive(username, true);
			final String phoneNumber; // Declare phoneNumber as final
			final String fullName;

			if (users != null) {
				phoneNumber = users.getPhone();
				fullName = users.getFullName();
				logger.info("Phone number of user", phoneNumber);
				logger.info("Full Name: ", fullName);
			} else {
				phoneNumber = null; // Initialize phoneNumber
				fullName = null;
			}
			for (GmtRfqVendors gmtVendor : gmtVendors) {
				String vendorId = gmtVendor.getVendorUuid();
				logger.info("Getting Users List");
				List<String> usersList = userDao.findByOrg(vendorId);
				if (!CollectionUtils.isEmpty(usersList)) {
					MailUtility.emailNewRfqForNoPR("NewRfq", javaMailSender, rfqData, host, usersList.get(0),
							username, gmtVendor.getOtherEmails(), phoneNumber, rfqDueDate, fullName,
							mailIdWrapper[0], passwordWrapper[0]);
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}

	}

	@Override
    public List<User> getGmtBuyers() {
        logger.info("Fetching GMT users with role: {}", ApplicationConstants.ClientInitiator);

        List<User> usersList = new ArrayList<>();
        try {
            Role role = roleDao.findByRoleNameAndActive(ApplicationConstants.ClientInitiator, true);

            if (role == null) {
                logger.warn("No active role found with name: {}", ApplicationConstants.ClientInitiator);
                return usersList;
            }

            usersList = userDao.getUsersBySelfClientAndRole(role);

            if (usersList == null || usersList.isEmpty()) {
                logger.info("No users found for role: {}", ApplicationConstants.ClientInitiator);
            } else {
                logger.info("Found {} user(s) for role: {}", usersList.size(), ApplicationConstants.ClientInitiator);
            }

        } catch (Exception e) {
            logger.error("Error while fetching GMT users: {}", e.getMessage(), e);
        }

        return usersList;
    }

	@Override
	public Map<String, Object> createRFQByClient(Rfq rfq) throws Exception {
	    logger.info("Request received for RFQ creation with No PR by client {}", rfq);

	    Map<String, Object> result = new HashMap<>();

	    try {
	        MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);
	        MasterStatus newStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW);

	        rfq.setByClient(true);
	        rfq.setStatus(resultStatus);
	        rfq.setClientStatus(newStatus);

	       // String rfqId = selfRegistrationService.generateId("RFQ");
	        String rfqId = generateRfqId("RFQ");
	        logger.info("Generated RFQ Id: {}", rfqId);
	        rfq.setRfqId(rfqId);

	        List<RfqItem> rfqItems = rfq.getRfqItem();
	        List<GmtItems> gmtItems = rfqItems.stream().map(this::mapRfqItemToGmtItem).collect(Collectors.toList());
	        gmtItemsDao.saveAll(gmtItems);
	        logger.info("Saved RFQ items in GMT Items");

	        Rfq saved = rfqDao.save(rfq);
	        logger.info("Completed Saving RFQ with UUID: {}", saved.getId());

	        // return both
	        result.put("rfqId", rfqId);
	        result.put("rfquuid", saved.getId());

	        return result;
	    } catch (DataAccessException e) {
	        logger.error("Error occurred while creating RFQ: {}", e.getMessage());
	        return null;
	    }
	}

//	
//	@Transactional
//	public void uploadExcelToDB(String excelPath) {
//	    try (FileInputStream fis = new FileInputStream(excelPath);
//	         Workbook workbook = new XSSFWorkbook(fis)) {
//
//	        Sheet sheet = workbook.getSheetAt(0);
//
//	        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header row
//	            Row row = sheet.getRow(i);
//
//	            if (row != null) {
//	            	ItemCategory item = new ItemCategory();
//	                item.setId(UUID.randomUUID().toString());
//	                item.setSerialNo(getCellValue(row.getCell(0)));
//	                item.setDivision(getCellValue(row.getCell(1)));
//	                item.setCategory(getCellValue(row.getCell(2)));
//	                item.setItem(getCellValue(row.getCell(3)));
//	                item.setCreatedTS(new Date());
//
//	                itemCategoryDao.save(item);
//	            }
//	        }
//
//	    } catch (Exception e) {
//	        e.printStackTrace();
//	    }
//	}
//
//	private String getCellValue(Cell cell) {
//	    if (cell == null) {
//	        return "";
//	    }
//
//	    switch (cell.getCellType()) {
//	        case Cell.CELL_TYPE_STRING:
//	            return cell.getStringCellValue().trim();
//	        case Cell.CELL_TYPE_NUMERIC:
//	            if (DateUtil.isCellDateFormatted(cell)) {
//	                return cell.getDateCellValue().toString();
//	            } else {
//	                return String.valueOf(cell.getNumericCellValue());
//	            }
//	        case Cell.CELL_TYPE_BOOLEAN:
//	            return String.valueOf(cell.getBooleanCellValue());
//	       
//	        default:
//	            return "";
//	    }
//
//	}

	@Override
	public boolean sendEmail(EmailRequest emailRequest) {
		// TODO Auto-generated method stub
		 try {
	            SimpleMailMessage message = new SimpleMailMessage();
	            
	            // Set recipients
	            if (emailRequest.getTo() != null && !emailRequest.getTo().isEmpty()) {
	                message.setTo(emailRequest.getTo().toArray(new String[0]));
	            }

	            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
	                message.setCc(emailRequest.getCc().toArray(new String[0]));
	            }

	            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
	                message.setBcc(emailRequest.getBcc().toArray(new String[0]));
	            }

	            // Set subject and body
	            message.setSubject(emailRequest.getSubject());
	            message.setText(emailRequest.getBody());

	            // Send email
	            javaMailSender.send(message);

	            return true;
	        } catch (Exception e) {
	            e.printStackTrace();
	            return false;
	        }
	    }

	@Override
	public List<SubscriptionPlan> getSubscriptionPlans() {
		// TODO Auto-generated method stub
		  return subscriptionPlanDao.findAll();
    }

	@Override
	public List<RfqStatusResponse> getRfqStatuses(RfqStatusRequest request) {
	    List<RfqStatusResponse> responses = new ArrayList<>();

	    if (request.getRfqIds() != null && !request.getRfqIds().isEmpty()) {
	        // Fetch matching RFQs from DB
	        List<Rfq> rfqs = rfqDao.findByUserAndRfqIdIn(request.getClientId(), request.getRfqIds());

	        // Map RFQs by ID for quick lookup
	        Map<String, Rfq> rfqMap = rfqs.stream()
	                .collect(Collectors.toMap(Rfq::getRfqId, r -> r));

	        // Always include all requested IDs
	        for (String rfqId : request.getRfqIds()) {
	            RfqStatusResponse dto = new RfqStatusResponse();
	            dto.setRfqid(rfqId);

	            if (rfqMap.containsKey(rfqId)) {
	                Rfq r = rfqMap.get(rfqId);
	                dto.setStatus(r.getStatus() != null ? r.getStatus().getUiDisplay() : "Unknown");
	            } else {
	                dto.setStatus("Invalid RFQID");
	            }

	            responses.add(dto);
	        }
	    } else {
	        // No rfqIds provided → fetch last 3
	        List<Rfq> rfqs = rfqDao.findLast3ByClientId(request.getClientId(), PageRequest.of(0, 3));

	        if (rfqs.isEmpty()) {
	            // Client has no RFQs at all
	            RfqStatusResponse dto = new RfqStatusResponse();
	            dto.setRfqid(null);
	            dto.setStatus("Not Found");
	            responses.add(dto);
	        } else {
	            for (Rfq r : rfqs) {
	                RfqStatusResponse dto = new RfqStatusResponse();
	                dto.setRfqid(r.getRfqId());
	                dto.setStatus(r.getStatus() != null ? r.getStatus().getUiDisplay() : "Unknown");
	                responses.add(dto);
	            }
	        }
	    }

	    return responses;
	}


	@Override
	public int getSellerRfqCredits(Organization org) {
		// TODO Auto-generated method stub
		int credits=0;
		logger.info("entered to get Seller Rfq Credits");
		if(org!=null) {
			 credits=orgDao.findRfqCreditsByOrg(org.getId());
		}
		
		return credits;
	}
	@Override
	public Organization getOrgById(Organization organization) {
	    if (organization == null || organization.getId() == null) {
	        logger.error("Organization or ID is null");
	        throw new AppException(HttpStatus.BAD_REQUEST.value(),
	                               "Organization ID must not be null",
	                               ApplicationConstants.BUSSINESS_EXCEPTION,
	                               ApplicationConstants.FAILURE);
	    }
	    return orgDao.findById(organization.getId())
	                 .orElseThrow(() -> {
	                     logger.error("Organization not found for ID: {}", organization.getId());
	                     return new AppException(HttpStatus.NOT_FOUND.value(),
	                                             ApplicationConstants.VENDOR_DETAILS_DOESNT_EXIST,
	                                             ApplicationConstants.BUSSINESS_EXCEPTION,
	                                             ApplicationConstants.FAILURE);
	                 });
	}

	@Override
	public Map<String, Object> getRfqByItemCategory(Organization org) {
		    Map<String, Object> result = new HashMap<>();

		    if (org == null || org.getId() == null) {
		        logger.error("Invalid organization input");
		        result.put("rfqs", Collections.emptyList());
		        result.put("count", 0L);
		        result.put("categories", Collections.emptyList());
		        return result;
		    }

		    List<String> categoryList = orgCategoryDivisionDao.findCategoryByOrg(org.getId());
		    logger.info("Fetching RFQs by categories: {} for orgId: {}", categoryList, org.getId());

		    List<Rfq> rfqs = rfqDao.findByRfqItemCategory(categoryList);
		    long totalCount = rfqDao.countByRfqItemCategory(categoryList);

		    result.put("rfqs", rfqs);
		    result.put("count", totalCount);

		    return result;
		}
	
	@Override
	public Map<String, Object> forwardRfqsToVendor(ForwardRfqVendorRequest request) {
	    logger.info("Entered to forwardRfqForNoPr");

	    Map<String, Object> response = new HashMap<>();
	    Map<String, List<Map<String, Object>>> results = new HashMap<>();
	    List<Map<String, Object>> successful = new ArrayList<>();
	    List<Map<String, Object>> failed = new ArrayList<>();

	    for (String rfqId : request.getRfqIds()) {
	        Map<String, Object> result = new HashMap<>();
	        result.put("rfq_id", rfqId);
	        result.put("seller_id", request.getSellerId());
	        result.put("seller_email", request.getEmail());

	        try {
	        	 //Optional<Rfq> rfqDataOpt = rfqDao.findById(rfqId);
	           Rfq rfqDataOpt = rfqDao.findByRfqId(rfqId);
	            if (rfqDataOpt==null) {
	                result.put("error", "RFQ not found");
	                failed.add(result);
	                continue;
	            }

	            if (!isValidEmail(request.getEmail())) {
	                result.put("error", "Invalid email format");
	                failed.add(result);
	                continue;
	            }

	            Rfq rfqData = rfqDataOpt;

	            // Build a single RfqVendor for this seller
	            RfqVendor rfqVendor = new RfqVendor();
	            rfqVendor.setRfq(rfqData);
	            rfqVendor.setVendorId(request.getSellerId());
	            rfqVendor.setEmail(request.getEmail());

	            // Call new sendRfqToVendors method
	            boolean emailSent = sendRfqsToVendor(rfqVendor, rfqData);

	            result.put("email_sent", emailSent);
	            if (emailSent) {
	                successful.add(result);
	            } else {
	                result.put("error", "Failed to send email");
	                failed.add(result);
	            }

	        } catch (Exception ex) {
	            result.put("error", ex.getMessage());
	            failed.add(result);
	        }
	    }

	    response.put("success", true);
	    results.put("successful", successful);
	    results.put("failed", failed);
	    response.put("results", results);

	    return response;
	}

	public boolean sendRfqsToVendor(RfqVendor vendor, Rfq rfqData) {
	    logger.info("Entered to sendRfqsToVendor()");

	    try {
	        // Fetch RFQ data again just in case
	        Optional<Rfq> rfqOpt = rfqDao.findById(rfqData.getId());
	        if (rfqOpt.isEmpty()) {
	            logger.warn("RFQ not found with ID: " + rfqData.getId());
	            return false;
	        }

	        // Load statuses
	        MasterStatus pcRfqSent = null, vendorRfqNew = null;
	        List<String> inputStatus = Arrays.asList(StatusConstants.pcRfqSent, StatusConstants.vendorRfqNew);
	        List<MasterStatus> statusList = masterStatusDao.findByStatusIn(inputStatus);

	        for (MasterStatus status : statusList) {
	            if (StatusConstants.pcRfqSent.equals(status.getStatus())) {
	                pcRfqSent = status;
	            }
	            if (StatusConstants.vendorRfqNew.equals(status.getStatus())) {
	                vendorRfqNew = status;
	            }
	        }

	        // Populate vendor object
	        vendor.setRfq(rfqOpt.get());
	        vendor.setRfqId(rfqOpt.get().getRfqId());
	        vendor.setProcucevStatus(pcRfqSent);
	        vendor.setVendorStatus(vendorRfqNew);
	        vendor.setVendorResponseDate(vendor.getVendorResponseDate());

	        // Save vendor
	        rfqVendorDao.save(vendor);

	        // Build due date
	        String rfqDueDate = buildingRfqDueDate();

	        // Send email and return actual status
	        boolean status = MailUtility.emailNewRfqForNoPR(
	            "NewRfq",
	            javaMailSender,
	            rfqData,
	            host,
	            vendor.getEmail(),   // main vendor email
	            mailFom,             // from
	            null,                // other emails
	            null,                // phone number
	            rfqDueDate,
	            null,                // full name
	            mailFom,             // mailId
	            emailPassword        // password
	        );

	        return status;  // ✅ return actual send result

	    } catch (Exception e) {
	        logger.error("Exception in sendRfqsToVendor", e);
	        return false;
	    }
	}
	
	private boolean isValidEmail(String email) {
	    return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
	}

	@Override
	public Organization getOrgByUserId(User user) {
	    logger.info("Fetching organization for user");

	    if (user == null || user.getId() == null) {
	        logger.error("User or ID is null");
	        throw new AppException(
	                HttpStatus.BAD_REQUEST.value(),
	                "User ID must not be null",
	                ApplicationConstants.BUSSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE
	        );
	    }

	    Optional<User> userDataOpt = userDao.findById(user.getId());

	    if (userDataOpt.isEmpty()) {
	        logger.error("No user found with ID: {}", user.getId());
	        throw new AppException(
	                HttpStatus.NOT_FOUND.value(),
	                "User not found with ID: " + user.getId(),
	                ApplicationConstants.BUSSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE
	        );
	    }

	    User userData = userDataOpt.get();
	    Organization org = userData.getOrg();

	    if (org == null) {
	        logger.error("Organization not linked to user ID: {}", user.getId());
	        throw new AppException(
	                HttpStatus.NOT_FOUND.value(),
	                "Organization not linked to user",
	                ApplicationConstants.BUSSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE
	        );
	    }

	    // enrich organization with additional details
	    org.setUserId(userData.getId());
	    org.setEmail(userData.getUsername());
	    org.setOrganizationPhonenumber(userData.getPhone());
	    org.setName(userData.getFullName());
	    List<OrgDivisionCategory> categories = orgCategoryDivisionDao.findbyUser(userData.getId());
	    org.setDivisionCategories(categories);

	    logger.info("Organization fetched successfully for user ID: {}", user.getId());
	    return org;
	}

public String generateRfqId(String company) {
    String companyLetters = "";
    if (company != null && !company.isEmpty()) {
        companyLetters = company.length() >= 3
                ? company.substring(0, 3).toUpperCase()
                : company.toUpperCase();
    }

    // current date in ddMM format
    String datePart = new SimpleDateFormat("yyddMM").format(new Date());

    // milliseconds part
    long millis = System.currentTimeMillis() % 1000000; // last 6 digits to shorten

    // optional random 3-digit suffix
    //int random = (int) (Math.random() * 1000);

   // return  companyLetters + datePart + millis + String.format("%03d", random);
    return  companyLetters + datePart + millis ;
}

@Override
public List<Map<String, Object>> getLastOpenRfqsForVendor(String vendorId) {
    Pageable top3 = PageRequest.of(0, 3);
    List<GmtRfqVendors> rfqVendors = gmtRfqVendorDao.findLastOpenRfqsByVendor(vendorId, top3);

    List<Map<String, Object>> openRfqs = new ArrayList<>();
    for (GmtRfqVendors gv : rfqVendors) {
        Rfq rfq = gv.getRfq();

        Map<String, Object> rfqData = new LinkedHashMap<>();
        rfqData.put("rfq_id", rfq.getRfqId());
        rfqData.put("project_desc", rfq.getProjectDesc());
        rfqData.put("category", rfq.getCategory());
        if(!CollectionUtils.isEmpty(rfq.getClientdeliverylocationrfq())){
        	 rfqData.put("location", rfq.getClientdeliverylocationrfq().get(0).getCity()+ "," + rfq.getClientdeliverylocationrfq().get(0).getState());
        }
        rfqData.put("submission_deadline", rfq.getRfqClosingDate());
        rfqData.put("email_sent_date", gv.getRequestedDate());
        if( rfq.getRfqClosingDate()!=null) {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), 
                                rfq.getRfqClosingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        rfqData.put("days_remaining", daysRemaining);
        }
        
//        rfqData.put("estimated_value", rfq.getDescription()); // replace with your RFQ budget/value field
        rfqData.put("status", "open_for_bidding");

        openRfqs.add(rfqData);
    }

    return openRfqs;
}

}
