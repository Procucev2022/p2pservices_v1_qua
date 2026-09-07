package com.portal.procucev.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.portal.procucev.Dto.BuyerSellerReportDto;
import com.portal.procucev.Dto.ClientRFQDto;
import com.portal.procucev.Dto.DeliveryLocationUpdateRequest;
import com.portal.procucev.Dto.ForwardRfqVendorRequest;
import com.portal.procucev.Dto.GMTRfqVendorDto;
import com.portal.procucev.Dto.GmtRfqSellerDto;
import com.portal.procucev.Dto.RfqAiTokenUsageDTO;
import com.portal.procucev.Dto.RfqDTO;
import com.portal.procucev.Dto.SellerSubscriptionReportDto;
import com.portal.procucev.Dto.SimplePageResponse;
import com.portal.procucev.Dto.VendorInfoDto;
import com.portal.procucev.Dto.VendorRFQDto;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
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
import com.portal.procucev.rfq.entity.RfqAiTokenUsage;
import com.portal.procucev.rfq.repository.RfqAiTokenUsageRepository;
import com.portal.procucev.model.CategoryDivision;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.EmailAttachment;
import com.portal.procucev.model.EmailRequest;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.GmtItems;
import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.ItemCategory;
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
import com.portal.procucev.utils.EmailValidatorUtil;
import com.portal.procucev.utils.MailUtility;
import com.portal.procucev.utils.PhoneNumberUtils;
import com.portal.procucev.utils.StatusConstants;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class GMTServiceImpl implements GMTService {
	private static final Logger logger = LoggerFactory.getLogger(GMTServiceImpl.class);

	@Autowired
	private RfqDao rfqDao;
	
	@Autowired
	private RFQItemsDao rfqItemsDao;

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
	private AutomaticRfqService automaticRfqService;

	@Autowired
	private GmtItemsDao gmtItemsDao;

	@Autowired
	private RfqVendorDao rfqVendorDao;

	@Autowired
	private GmtRfqVendorDao gmtRfqVendorDao;

	@Autowired
	private ItemCategoryDao itemCategoryDao;
	
	@Value("${email.subject.prefix}")
	private String subjectPrefix;

	@Autowired
	private OrgDao orgDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private OrgTypeDao orgTypeDao;

	
	@Autowired
	private OrgCategoryDivisionDao orgCategoryDivisionDao;

	@Autowired
	private RfqAiTokenUsageRepository rfqAiTokenUsageRepository;

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
	
	 @Value("${report.from.email}")
	 private String fromEmail;

	 @Value("${report.to.email}")
	 private String toEmail;

	private final Map<String, String> otpMap = new ConcurrentHashMap<>();
	@Override
	public List<String> getClientRfqIds(User user) {
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
		rfqDto.setQuoteCount(rfq.getQuoteCount());
		rfqDto.setQuoteSubmittedDate(rfq.getQuoteSubmittedDate());
		
		return rfqDto;
	}

	@Override
	public List<String> getAllDivision() {
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
		logger.info("Request received for RFQ creation with No PR by client: rfqId={}, projectDesc='{}', category='{}', itemCount={}",
				rfq.getRfqId(), rfq.getProjectDesc(), rfq.getCategory(),
				rfq.getRfqItem() != null ? rfq.getRfqItem().size() : 0);

		try {
			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);
			MasterStatus newStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW);

			rfq.setByClient(true);
			rfq.setStatus(resultStatus);
			rfq.setClientStatus(newStatus);

			// String rfqId = selfRegistrationService.generateId("RFQ");

			String rfqId = automaticRfqService.generateRfqId("RFQ");
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
		gmtItem.setBrand(rfqItem.getRemarks()+" "+rfqItem.getBrand());
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

			if (updatedRfq.getDeliveryDate() == null && existingRfq.getDeliveryDate() != null) {
				updatedRfq.setDeliveryDate(existingRfq.getDeliveryDate());
			}

			updatedRfq.setStatus(resultStatus);

			// Set RFQ ID for RFQ items
			List<RfqItem> updatedRfqItems = updatedRfq.getRfqItem();
			List<ItemCategory> itemCategoryList = new ArrayList<>();

			for (RfqItem rfqItem : updatedRfqItems) {

			    ItemCategory itemCategory = new ItemCategory();

			     // or generate if needed
			    itemCategory.setItem(rfqItem.getDescription());
			    itemCategory.setCategory(rfqItem.getCategory());
			    itemCategory.setDivision(rfqItem.getDivision());

			    itemCategoryList.add(itemCategory);
			}

			// Bulk insert (recommended)
			itemCategoryDao.saveAll(itemCategoryList);
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
				updatedDeliveryLocation.forEach(clientDeliveryLocation -> {
					clientDeliveryLocation.setCity(sanitizeLocationField(clientDeliveryLocation.getCity()));
					clientDeliveryLocation.setState(sanitizeLocationField(clientDeliveryLocation.getState()));
					clientDeliveryLocation.setPincode(sanitizeLocationField(clientDeliveryLocation.getPincode()));
					clientDeliveryLocation.setAddress(sanitizeLocationField(clientDeliveryLocation.getAddress()));
					clientDeliveryLocation.setRfq(updatedRfq);
				});
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
		logger.info("Entered To Send mail To");
		String email = null;
		String OrgName = null;
		String phone = null;
		String fullName = null;

		if (user != null) {
			String toEmail = "info@procucev.com";
			InternetAddress add = new InternetAddress(mailFom, "Procucev Notifications");
			logger.info("Sending mail to::" + toEmail);
			if (user.getId() != null) {
				User findByID = userDao.findById(user.getId()).get();
				if (findByID != null) {
					email = findByID.getUsername();
					phone = findByID.getPhone();
					fullName = findByID.getFullName();
					OrgName = findByID.getOrg().getCompanyName();
				}
			}
			MailUtility.mailingGMTClientRFQMailToinfoTeam("Sending GMT Query Successfull", toEmail, javaMailSender, add,
					host, user, email, phone, fullName, OrgName);

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

	    // Step 1: statuses
	    List<MasterStatus> statuses = Arrays.asList(
	            masterStatusDao.findByStatus(StatusConstants.CM_RFQ_ACCEPTED),
	            masterStatusDao.findByStatus(StatusConstants.VENDOR_QUOTE_SUBMITTED),
	            masterStatusDao.findByStatus(StatusConstants.VENDOR_RFQ_QUERIED)
	    );

	    MasterStatus defaultStatus =
	            masterStatusDao.findByStatus(StatusConstants.vendorRfqNew);

	    // Step 2: RFQs (IMPORTANT: should NOT load items lazily)
	    List<Rfq> rfqList = rfqDao.findAllRfqNoPrInStatuses(statuses);

	    if (rfqList == null || rfqList.isEmpty()) {
	        throw new AppException(
	                HttpStatus.NO_CONTENT.value(),
	                ApplicationConstants.NO_DATA_FOUND,
	                ApplicationConstants.BUSSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE
	        );
	    }

	    logger.info("Total RFQs fetched: {}", rfqList.size());

	    // Step 3: RFQ IDs
	    List<String> rfqIds = rfqList.stream()
	            .map(Rfq::getId)
	            .collect(Collectors.toList());

	    // Step 4: Vendor mapping (1 query)
	    List<GmtRfqVendors> vendorMappings =
	            gmtRfqVendorDao.findByVendorAndRfqIn(org, rfqIds);

	    Map<String, GmtRfqVendors> vendorMap = vendorMappings.stream()
	            .collect(Collectors.toMap(
	                    v -> v.getRfq().getId(),
	                    v -> v,
	                    (a, b) -> a
	            ));

	    // Step 5: CATEGORY mapping (batched, safe)
	    Map<String, Set<String>> categoryMap = new HashMap<>();

	    int batchSize = 500;

	    for (int i = 0; i < rfqIds.size(); i += batchSize) {

	        List<String> batch = rfqIds.subList(i, Math.min(i + batchSize, rfqIds.size()));

	        List<Object[]> categoryData =
	                rfqItemsDao.findTopCategoriesByRfqIds(batch);

	        for (Object[] row : categoryData) {
	            String rfqId = (String) row[0];
	            String category = (String) row[1];

	            if (category == null) continue;

	            categoryMap
	                    .computeIfAbsent(rfqId, k -> new LinkedHashSet<>())
	                    .add(category);
	        }
	    }

	    // ⭐ STEP 6 (CRITICAL FIX): FETCH ALL RFQ ITEMS IN ONE QUERY
	    List<RfqItem> allItems = rfqItemsDao.findAllByRfqIds(rfqIds);

	    Map<String, List<RfqItem>> itemMap = allItems.stream()
	            .collect(Collectors.groupingBy(i -> i.getRfq().getId()));

	    // Step 7: build response
	    List<GMTRfqVendorDto> response = new ArrayList<>();

	    for (Rfq rfq : rfqList) {

	        GMTRfqVendorDto dto = new GMTRfqVendorDto();

	        dto.setId(rfq.getId());
	        dto.setRfqId(rfq.getRfqId());
	        dto.setDesc(rfq.getProjectDesc());
	        dto.setClosureDate(rfq.getRfqClosingDate());
	        dto.setCreatedTS(rfq.getCreatedTS());
	        dto.setDivision(rfq.getDivision());
	        dto.setDeliveryDate(rfq.getDeliveryDate());
	        dto.setUserId(rfq.getUser());

	        // Categories
	        Set<String> cats = categoryMap.get(rfq.getId());
	        if (cats != null && !cats.isEmpty()) {
	            dto.setCategory(String.join(", ", cats));
	        }

	        // Delivery location (no lazy N+1)
	        if (rfq.getClientdeliverylocationrfq() != null
	                && !rfq.getClientdeliverylocationrfq().isEmpty()) {
	            dto.setDeliveryLocation(
	                    rfq.getClientdeliverylocationrfq().get(0).getCity()
	            );
	        }

	        // Vendor data
	        GmtRfqVendors vendorData = vendorMap.get(rfq.getId());

	        if (vendorData != null) {

	            dto.setStatus(
	                    vendorData.getStatus() != null
	                            ? vendorData.getStatus()
	                            : defaultStatus
	            );

	            dto.setRequestedDate(vendorData.getRequestedDate());
	            dto.setAcceptedDate(vendorData.getAcceptedDate());
	            dto.setQuoteSubmittedDate(vendorData.getQuoteSubmittedDate());
	            dto.setQuery(vendorData.getQuery());

	        } else {
	            dto.setStatus(defaultStatus);
	        }

	        // ⭐ RFQ ITEMS (NO MORE LAZY LOADING)
	        List<RfqItem> items = itemMap.get(rfq.getId());

//	        if (items != null) {
//	            // you can map items if needed OR skip if not required in DTO
//	            dto.setItemCount(items.size());
//	        } else {
//	            dto.setItemCount(0);
//	        }

	        response.add(dto);
	    }

	    logger.info("Returning {} GMT RFQ records", response.size());

	    return response;
	}
	
	@Override
	public boolean requestRfqByVendors(List<GmtRfqVendors> rfq) {

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
	public List<GmtRfqSellerDto> getVendorsByGmtRfq(Rfq rfq) {

	    logger.info("Entered to get GMT Vendors By RFQ {}", rfq.getId());

	    if (rfq == null) {
	        throw new IllegalArgumentException("RFQ object cannot be null.");
	    }

	    List<GmtRfqVendors> entities = gmtRfqVendorDao.findByRfq(rfq);

	    if (entities == null || entities.isEmpty()) {
	        throw new AppException(HttpStatus.NO_CONTENT.value(),
	                ApplicationConstants.NO_DATA_FOUND,
	                ApplicationConstants.BUSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE);
	    }

	    List<GmtRfqSellerDto> response = new ArrayList<>();

	    for (GmtRfqVendors g : entities) {

	    	GmtRfqSellerDto dto = new GmtRfqSellerDto();

	        dto.setId(g.getId());
	        dto.setVendorUuid(g.getVendor() != null ? g.getVendor().getId() : null);
	        dto.setVendorName(g.getVendor() != null ? g.getVendor().getCompanyName() : null);
	        dto.setVendorId(g.getVendor() != null ? g.getVendor().getCompanyId() : null);
	        dto.setOtherEmails(g.getVendor() != null ? g.getVendor().getOtherEmails() : null);
	        dto.setQuery(g.getQuery());
	        dto.setQuoteSubmittedDate(g.getQuoteSubmittedDate());

	        // ✅ STATUS HANDLING (your requirement)
	        if (g.getStatus() != null) {
	            dto.setStatus(g.getStatus());
	        } else {
	        	MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.RFQ_NOTIFIED);
	            dto.setStatus(resultStatus);
	        }

	        response.add(dto);
	    }

	    return response;
	}
//	@Override
//	public List<GmtRfqVendors> getVendorsByGmtRfq(Rfq rfq) {
//		logger.info("Entered to get GMT Vendors By RFQ {}", rfq.getId());
//		if (rfq == null) {
//			logger.error("RFQ object is null.");
//			throw new IllegalArgumentException("RFQ object cannot be null.");
//		}
//
//		List<GmtRfqVendors> response = gmtRfqVendorDao.findByRfq(rfq);
//		//List<RfqVendor> rfqVendors = getVendorsbyRFQ(rfq); // Call the method to get RfqVendor													// list
////		MasterStatus forwardedStatus = masterStatusDao.findByStatus(StatusConstants.RFQ_FORWARDED);
////		logger.info("Size of rfqVendors==", rfqVendors.size());
////		if (!rfqVendors.isEmpty()) {
////			// Add RfqVendor objects to the response
////			for (RfqVendor rfqVendor : rfqVendors) {
////				GmtRfqVendors gmtRfqVendor = new GmtRfqVendors();
////				// gmtRfqVendor.setId(rfqVendor.getId());
////				gmtRfqVendor.setVendorName(rfqVendor.getCompanyName());
////				gmtRfqVendor.setVendorUuid(rfqVendor.getVendorId());
////				gmtRfqVendor.setVendorId(rfqVendor.getCompanyId());
////				gmtRfqVendor.setOtherEmails(rfqVendor.getOtherEmails());
////				gmtRfqVendor.setStatus(forwardedStatus);
////				// Set other fields according to GmtRfqVendors object
////				// You may need to adjust fields based on GmtRfqVendors properties
////				response.add(gmtRfqVendor);
////			}
////		}
//		if (!response.isEmpty()) {
//			logger.info("Found {} vendors for RFQ {}", response.size(), rfq.getId());
//			return response;
//		} else {
//			logger.error("No vendors available in the database for RFQ {}", rfq.getId());
//			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
//					ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
//		}
//	}

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
				String rfqDueDate = buildingRfqDueDate(rfq.get().getDeliveryDate());
				MailUtility.emailNewGMTRfqForNoPR("NewRfq",subjectPrefix,javaMailSender, rfq.get(), host, email, otherEmails,
						mailFom, emailPassword, rfqDueDate, gmtRfq.getVendor().getId());

			}
			return true;
		} else {
			return false;
		}

	}

	private String buildingRfqDueDate(Date date) {
	    logger.info("Entered into building Rfq Due Date");

	    ZoneId zone = ZoneId.of("Asia/Kolkata");

	    // Current time in IST
	    LocalDateTime now = LocalDateTime.now(zone);

	    // Convert given Date to LocalDateTime
	    LocalDateTime givenDate = date.toInstant()
	            .atZone(zone)
	            .toLocalDateTime();

	    // Calculate difference
	    Duration duration = Duration.between(now, givenDate);

	    LocalDateTime finalDateTime;

	    if (duration.toHours() > 48) {
	        // If more than 48 hrs → set to now + 48 hrs
	        finalDateTime = now.plusHours(48);
	        logger.info("Given date > 48 hrs, setting to 48 hrs from now");
	    } else {
	        // Else → use given date
	        finalDateTime = givenDate;
	        logger.info("Given date within 48 hrs, using provided date");
	    }

	    // Format
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH);
	    String formattedDateTime = finalDateTime.format(formatter);

	    logger.info("Rfq Due Date after format: " + formattedDateTime);
	    return formattedDateTime;
	}
	@Override
	public boolean ignoreRfqByVendor(List<GmtRfqVendors> rfq) {
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
		List<String> inputStatus = new ArrayList<>();
	
		inputStatus.add(StatusConstants.CM_RFQ_ACCEPTED);
		inputStatus.add(StatusConstants.VENDOR_QUOTE_SUBMITTED);

		List<MasterStatus> statusList = masterStatusDao.findByStatusIn(inputStatus);
		

		
		List<Rfq> rfqsList = rfqDao.findAllRfqNoPrByCM(statusList);
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
		rfqDto.setNoOfQuotes(rfq.getQuoteCount());
		//Updateing Source Type
		rfqDto.setSourceType(rfq.getSourceType());
		//rfqDto.setNoOfVendors(rfqVendorDao.findByVendorsByRfq(rfq.getId()));
		rfqDto.setNoOfVendors(gmtRfqVendorDao.findByVendorsByRfq(rfq.getId()));
		rfqDto.setQuoteSubmittedDate(rfq.getQuoteSubmittedDate());
		if (rfq.getClientStatus() != null) {
			rfqDto.setClientStatus(rfq.getClientStatus());
			rfqDto.setClientStatusId(rfq.getClientStatus().getId());
			rfqDto.setClientStatusName(rfq.getClientStatus().getStatus());
		}
		rfqDto.setNewCommentAvailableVendor(rfq.isNewCommentAvailableVendor());

		String companyName = userDao.findByUser(rfq.getUser());
		if (companyName != null) {
			rfqDto.setCompanyName(companyName);
		}
		String companyId = userDao.findOrgIdByUser(rfq.getUser());
		if (companyId != null) {
			rfqDto.setCompanyId(companyId);
		}
		String phone = userDao.findPhoneByUser(rfq.getUser());
		if (phone != null) {
			rfqDto.setPhoneNumber(phone);
		}
		return rfqDto;
	}

	@Override
	public boolean raiseQueryByVendor(GmtRfqVendors rfq) {

	    logger.info("Entered To Raise Query By Vendor For Rfq");

	    try {

	        if (rfq == null) {
	            logger.warn("No Data Found");
	            throw new AppException(HttpStatus.NO_CONTENT.value(),
	                    ApplicationConstants.NO_DATA_FOUND,
	                    ApplicationConstants.BUSINESS_EXCEPTION,
	                    ApplicationConstants.FAILURE);
	        }

	        MasterStatus queryStatus = masterStatusDao
	                .findByStatus(StatusConstants.VENDOR_RFQ_QUERIED);

	        MasterStatus quoteSubmittedStatus = masterStatusDao
	                .findByStatus(StatusConstants.VENDOR_QUOTE_SUBMITTED);

	        GmtRfqVendors gmtRfq = gmtRfqVendorDao
	                .findByVendorAndRfq(rfq.getVendor(), rfq.getRfq());

	        if (gmtRfq != null) {

	            logger.info("Updating Query As Record Already Exists");

	            gmtRfqVendorDao.updateQuery(
	                    rfq.getRfq().getId(),
	                    rfq.getVendor().getId(),
	                    rfq.getQuery(),
	                    queryStatus
	            );

	        } else {
	        	rfq.setStatus(queryStatus);
	            logger.info("Saving Query Record For First Time With New Status");
	            gmtRfqVendorDao.save(rfq);
	        }
	        rfqDao.updateRfqCommentFlag(rfq.getRfq().getId());

	        // 🔹 Update RFQ status only if current status is NOT QUOTE_SUBMITTED
	     //   Rfq existingRfq = rfqDao.findById(rfq.getRfq().getId()).orElse(null);

//	        if (existingRfq != null
//	                && existingRfq.getStatus() != null
//	                && quoteSubmittedStatus != null
//	                && !quoteSubmittedStatus.getId()
//	                        .equals(existingRfq.getStatus().getId())) {
//
//	            logger.info("Updating RFQ status to VENDOR_RFQ_QUERIED");
//	            rfqDao.updateRfqStatus(rfq.getRfq(), queryStatus);
//
//	        } else {
//	            logger.info("RFQ status not updated (either null or already QUOTE_SUBMITTED)");
//	        }

	        return true;

	    } catch (Exception e) {
	        logger.error("Error while raising query by vendor", e);
	        return false;
	    }
	}

	private RfqDTO mapToClientDto(
	        Rfq rfq,
	        Map<String, User> userMap,
	        Map<String, Long> vendorCountMap) {

	    RfqDTO dto = new RfqDTO();

	    dto.setId(rfq.getId());
	    dto.setCreatedBy(rfq.getCreatedBy());
	    dto.setCreatedTs(rfq.getCreatedTS());

	    dto.setProjectDesc(rfq.getProjectDesc());

	    dto.setDivision(rfq.getDivision());

	    dto.setRfqId(rfq.getRfqId());

	    dto.setNoOfQuotes(rfq.getQuoteCount());

	    dto.setQuoteSubmittedDate(
	            rfq.getQuoteSubmittedDate());

	    dto.setNewCommentAvailableVendor(
	            rfq.isNewCommentAvailableVendor());

	    dto.setSourceType(rfq.getSourceType());

	    // client status
	    if (rfq.getClientStatus() != null) {
	        dto.setClientStatus(rfq.getClientStatus());
	        dto.setClientStatusId(
	                rfq.getClientStatus().getId());

	        dto.setClientStatusName(
	                rfq.getClientStatus().getStatus());
	    }

	    // user details from hashmap
	    User user = userMap.get(rfq.getUser());

	    if (user != null) {

	        dto.setPhoneNumber(user.getPhone());

	        if (user.getOrg() != null) {

	            dto.setCompanyName(
	                    user.getOrg().getCompanyName());

	            dto.setCompanyId(
	                    user.getOrg().getId());
	        }
	    }

	    // vendor count from hashmap
	    dto.setNoOfVendors(
	            vendorCountMap.getOrDefault(
	                    rfq.getId(),
	                    0L
	            ));

	    return dto;
	}

	/**
	 * Maps a batch of client RFQs to DTOs using a fixed number of queries.
	 *
	 * <p>The per-row alternative issued one vendor-count query plus three user lookups (phone,
	 * company name, org id) for every RFQ, so a 788-row result produced roughly 3,900 statements.
	 * Here the user rows and the vendor counts are each fetched once for the whole batch and the
	 * mapping itself touches no repository, which keeps the cost at two queries regardless of how
	 * many RFQs are returned.
	 *
	 * @param rfqs the RFQs to map; must already be loaded
	 * @return the mapped DTOs, in the order of {@code rfqs}
	 */
	private List<RfqDTO> mapClientRfqsInBatch(List<Rfq> rfqs) {
		if (rfqs.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> userIds = rfqs.stream().map(Rfq::getUser).filter(Objects::nonNull).distinct().toList();

		List<String> rfqIds = rfqs.stream().map(Rfq::getId).toList();

		Map<String, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
				: userDao.findUsersByIds(userIds).stream()
						.collect(Collectors.toMap(User::getId, Function.identity(), (first, second) -> first));

		Map<String, Long> vendorCountMap = new HashMap<>();
		for (Object[] row : gmtRfqVendorDao.countVendorsByRfqIds(rfqIds)) {
			vendorCountMap.put((String) row[0], (Long) row[1]);
		}

		return rfqs.stream().map(rfq -> mapToClientDto(rfq, userMap, vendorCountMap)).collect(Collectors.toList());
	}

	@Override
	public List<RfqDTO> fetchAllClientGMTRfqsForCM() {
		logger.info("Entered to fetch all client GMT RFQ for  CM");

		List<Rfq> rfqsList = rfqDao.findAllClientRfqNoPr();
		if (rfqsList.isEmpty()) {
			logger.warn("No RFQs found  by client flag True");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		logger.info("Found {} RFQs for PR Flag True", rfqsList.size());

		return mapClientRfqsInBatch(rfqsList);

	}
	

	@Override
	public SimplePageResponse<RfqDTO> fetchAllClientGMTRfqsForCM(Pageable pageable) {

	    Page<Rfq> rfqPage =
	            rfqDao.findAllClientRfqNoPr(pageable);

	    List<Rfq> rfqs = rfqPage.getContent();

	    if (rfqs.isEmpty()) {
	        throw new AppException(
	                HttpStatus.NO_CONTENT.value(),
	                ApplicationConstants.NO_DATA_FOUND,
	                ApplicationConstants.BUSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE
	        );
	    }

	    return new SimplePageResponse<>(
	    		rfqPage.getTotalElements(),
	            mapClientRfqsInBatch(rfqs)
	    );
	}
	
	@Override
	public List<RfqDTO> fetchAllClientGMTRfqsForCMSearch(String searchType, String searchValue) {

	    logger.info("fetchAllClientGMTRfqsForCMSearch | searchType: {}, searchValue: {}", searchType, searchValue);

	    List<Rfq> rfqList;
	    List<User> matchedUsers = new ArrayList<>();

	    switch (searchType.toLowerCase()) {

	        case "rfqid":
	        case "description":
	            // Search directly on rfq_header
	            rfqList = rfqDao.findAllClientRfqByRfqIdOrDescription(searchType, searchValue);
	            break;

	        case "companyname":
	            // Find users whose org name matches, then fetch their RFQs
	            matchedUsers = userDao.findUsersByOrgCompanyName(searchValue);
	            if (matchedUsers.isEmpty()) {
	                return Collections.emptyList();
	            }
	            List<String> userIdsByCompany = matchedUsers.stream()
	                    .map(User::getId)
	                    .distinct()
	                    .toList();
	            rfqList = rfqDao.findAllClientRfqByUserIds(userIdsByCompany);
	            break;

	        case "contactnumber":
	            // Find users whose phone matches, then fetch their RFQs
	            matchedUsers = userDao.findUsersByPhone(searchValue);
	            if (matchedUsers.isEmpty()) {
	                return Collections.emptyList();
	            }
	            List<String> userIdsByPhone = matchedUsers.stream()
	                    .map(User::getId)
	                    .distinct()
	                    .toList();
	            rfqList = rfqDao.findAllClientRfqByUserIds(userIdsByPhone);
	            break;

	        default:
	            logger.warn("Unknown searchType: {}", searchType);
	            return Collections.emptyList();
	    }

	    logger.info("RFQs found: {}", rfqList.size());

	    return mapClientRfqsInBatch(rfqList);
	}
	
	@Override
	public List<RfqVendor> getVendorsbyRFQ(Rfq rfq) {
		logger.info("Entered To Get Vefq.setndors By RFQ");
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
			Rfq loadedRfq = rfqList.get();
			if (!CollectionUtils.isEmpty(loadedRfq.getClientdeliverylocationrfq())) {
				loadedRfq.getClientdeliverylocationrfq().forEach(loc -> {
					loc.setCity(sanitizeLocationField(loc.getCity()));
					loc.setState(sanitizeLocationField(loc.getState()));
					loc.setPincode(sanitizeLocationField(loc.getPincode()));
					loc.setAddress(sanitizeLocationField(loc.getAddress()));
				});
			}
			if ("EMAIL".equalsIgnoreCase(loadedRfq.getSourceType()) && isAuthorizedCategoryManager()) {
				loadedRfq.setAiTokenUsage(buildAiTokenUsageDto(loadedRfq));
			}
			return new ResponseEntity<>(loadedRfq, HttpStatus.OK);
		} else {
			logger.error("No rfq's available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	@Override
	public List<String> getAllCategory() {
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
				rfqDto.setSourceType(rfq.getSourceType());
				if (rfq.getClientStatus() != null) {
					rfqDto.setClientStatus(rfq.getClientStatus());
					rfqDto.setClientStatusId(rfq.getClientStatus().getId());
					rfqDto.setClientStatusName(rfq.getClientStatus().getStatus());
				}
				logger.info("RfqID--", rfq.getRfqId());
				//long vendorsCount = rfqVendorDao.findByVendorsByRfq(rfq.getId());
				rfqDto.setNoOfVendors(gmtRfqVendorDao.findByVendorsByRfq(rfq.getId()));
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
		logger.info("Entered To Get All Vendor");
		List<VendorRFQDto> responseList = new ArrayList<>();
		OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
		List<Object[]> vendorsList = orgDao.getAllVendor(orgTypeObject);
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
	public SimplePageResponse<VendorRFQDto> getAllVendors(Pageable pageable) {

	    logger.info("Entered To Get All Vendor");
	    
	    OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);

	    Page<VendorRFQDto> responsePage =
	            orgDao.getAllVendor(orgTypeObject,pageable);
	    
	    List<VendorRFQDto> vendorList = responsePage.getContent();
	    
	    

	    if (CollectionUtils.isEmpty(vendorList)) {

	        logger.error("No Vendors available in the Database");

	        throw new AppException(
	                HttpStatus.NO_CONTENT.value(),
	                ApplicationConstants.NO_DATA_FOUND,
	                ApplicationConstants.BUSSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE);
	    }

	    logger.info("Completed and Returning response");
	    

	    return new SimplePageResponse<>(
	    		responsePage.getTotalElements(),
	    		vendorList
	    );
	}
	
	@Override
	public List<VendorRFQDto> getAllVendorsSearch(String searchType, String searchValue) {
		return getAllVendorsSearch(searchType, searchValue, null, null);
	}

	@Override
	public List<VendorRFQDto> getAllVendorsSearch(String searchType, String searchValue, String city) {
		return getAllVendorsSearch(searchType, searchValue, city, null);
	}

	@Override
	public List<VendorRFQDto> getAllVendorsSearch(String searchType, String searchValue, String city, String state) {
		 OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
		 String cleanSearchType = searchType != null ? searchType.trim() : "";
		 String cleanSearchValue = searchValue != null ? searchValue.trim() : "";
		 String cleanCity = city != null ? city.trim() : "";
		 String cleanState = state != null ? state.trim() : "";
		 if ("ALL".equalsIgnoreCase(cleanCity) || "Select All City".equalsIgnoreCase(cleanCity) || "All Cities".equalsIgnoreCase(cleanCity)) {
			 cleanCity = "";
		 }
		 if ("ALL".equalsIgnoreCase(cleanState) || "Select All State".equalsIgnoreCase(cleanState) || "All States".equalsIgnoreCase(cleanState)) {
			 cleanState = "";
		 }
		
		 List<VendorRFQDto> vendorList = new ArrayList<>();
		 if (("vendorcategory".equalsIgnoreCase(cleanSearchType) || "category".equalsIgnoreCase(cleanSearchType)) && !cleanSearchValue.isEmpty()) {
			 if (!cleanState.isEmpty() || !cleanCity.isEmpty()) {
				 vendorList = orgDao.searchVendorByCategoryStateAndCity(orgTypeObject, cleanSearchValue, cleanState, cleanCity);
				 if (vendorList == null || vendorList.isEmpty()) {
					 if (!cleanState.isEmpty()) {
						 vendorList = orgDao.searchVendorByCategoryStateAndCity(orgTypeObject, cleanSearchValue, cleanState, "");
					 }
				 }
				 if (vendorList == null || vendorList.isEmpty()) {
					 vendorList = orgDao.searchVendorByType(orgTypeObject, cleanSearchType, cleanSearchValue);
				 }
			 } else {
				 vendorList = orgDao.searchVendorByType(orgTypeObject, cleanSearchType, cleanSearchValue);
			 }
		 } else if ("email".equalsIgnoreCase(cleanSearchType) && !cleanSearchValue.isEmpty()) {
			 String[] rawTokens = cleanSearchValue.split("[\\r\\n,;]+|\\s+");
			 List<String> emailList = new ArrayList<>();
			 for (String token : rawTokens) {
				 String trimmed = token.trim().toLowerCase();
				 if (!trimmed.isEmpty() && !emailList.contains(trimmed)) {
					 emailList.add(trimmed);
				 }
			 }
			 if (emailList.size() > 20) {
				 emailList = emailList.subList(0, 20);
			 }

			 if (emailList.size() > 1) {
				 vendorList = orgDao.searchVendorByEmails(orgTypeObject, emailList);
			 } else if (emailList.size() == 1 && (cleanSearchValue.contains(",") || cleanSearchValue.contains("\n") || cleanSearchValue.contains(";"))) {
				 vendorList = orgDao.searchVendorByEmails(orgTypeObject, emailList);
			 } else {
				 vendorList = orgDao.searchVendorByType(orgTypeObject, cleanSearchType, cleanSearchValue);
			 }
		 } else if (isCompanyNameSearchType(cleanSearchType) && !cleanSearchValue.isEmpty() && (cleanSearchValue.contains(",") || cleanSearchValue.contains("\n") || cleanSearchValue.contains(";"))) {
			 String[] rawTokens = cleanSearchValue.split("[\\r\\n,;]+");
			 List<String> nameTokens = new ArrayList<>();
			 for (String token : rawTokens) {
				 String trimmed = token.trim();
				 if (!trimmed.isEmpty() && !nameTokens.contains(trimmed)) {
					 nameTokens.add(trimmed);
				 }
			 }
			 if (nameTokens.size() > 20) {
				 nameTokens = nameTokens.subList(0, 20);
			 }
			 Set<String> addedVendorIds = new HashSet<>();
			 for (String token : nameTokens) {
				 List<VendorRFQDto> matches = orgDao.searchVendorByType(orgTypeObject, cleanSearchType, token);
				 if (matches != null) {
					 for (VendorRFQDto dto : matches) {
						 if (dto != null && dto.getId() != null && !addedVendorIds.contains(dto.getId())) {
							 addedVendorIds.add(dto.getId());
							 vendorList.add(dto);
						 }
					 }
				 }
			 }
		 } else {
			 vendorList = orgDao.searchVendorByType(orgTypeObject, cleanSearchType, cleanSearchValue);
		 }
	    
	    if (CollectionUtils.isEmpty(vendorList)) {
	        logger.error("No Vendors available in the Database");
	        return Collections.emptyList();
	    }

	    logger.info("Completed and Returning response");
	    return vendorList;
	}

	@Override
	public List<String> getCitiesByVendorCategory(String category) {
		OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
		if (category == null || category.trim().isEmpty()) {
			return Collections.emptyList();
		}
		return orgDao.findCitiesByVendorCategory(orgTypeObject, category.trim());
	}

	private boolean isCompanyNameSearchType(String searchType) {
		if (searchType == null) return false;
		String lower = searchType.trim().toLowerCase();
		return lower.contains("company") || lower.contains("vendor") || lower.contains("seller") || lower.equals("name");
	}
	
	

//	@Override
//	public List<VendorRFQDto> getAllVendorsByCategory(Organization organization) {
//		logger.info("Entered To Get All Vendor");
//		List<VendorRFQDto> responseList = new ArrayList<>();
//		List<Object[]> vendorsList = orgDao.getAllVendorByCategory(organization.getVendorcategory());
//		if (!CollectionUtils.isEmpty(vendorsList) && vendorsList != null) {
//			vendorsList.stream().forEach(org -> {
//				VendorRFQDto vendorRFQDto = new VendorRFQDto();
//				vendorRFQDto.setId(String.valueOf(org[0]));
//				vendorRFQDto.setCompanyName(String.valueOf(org[1]));
//				vendorRFQDto.setVendorId(String.valueOf(org[2]));
//				vendorRFQDto.setMobileNo(String.valueOf(org[3]));
//				vendorRFQDto.setCity(String.valueOf(org[4]));
//				vendorRFQDto.setEmail(String.valueOf(org[5]));
//
//				responseList.add(vendorRFQDto);
//
//			});
//			logger.info("Completed and Returning response");
//			return responseList;
//		} else {
//			logger.error("No Vendors available in the Database");
//			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
//					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
//		}
//	}
	
	@Override
	public List<VendorRFQDto> getAllVendorsByCategory(Organization organization) {
		logger.info("Entered To Get All Vendor");
		List<VendorRFQDto> responseList = orgDao.getAllVendorByCategory(organization.getVendorcategory());
		if (CollectionUtils.isEmpty(responseList)) {
			logger.error("No Vendors available in the Database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		} 
		
		logger.info("Completed and Returning response");
		return responseList;
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
			// String rfqId = selfRegistrationService.generateId("RFQ");
			String rfqId = automaticRfqService.generateRfqId("RFQ");
			logger.info("Generated RFQ Id{}", rfqId);
			rfq.setRfqId(rfqId);

//			if (rfq.getBoqfile() != null && rfq.getBoqfile().length > 0) {
//				
//				List<RfqItem> rfqItems = processBOQFiles(rfq.getBoqfile());
//				rfqItemValidation(rfqItems);
//				rfq.setRfqItem(rfqItems);
//			} else {
			List<RfqItem> rfqItem = rfq.getRfqItem();
			// rfqItemValidation(rfqItem);

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
				rfqVendor.setRequestType(vendor.getRequestType());

				// Collect RfqVendor object
				rfqVendors.add(rfqVendor);
				// ✅ STEP 2: Get status
				MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.vendorApproved);

//				Organization org = orgDao.findById(savedVendor.getId()).get();
				// ✅ STEP 3: Check if record exists
				GmtRfqVendors existing = gmtRfqVendorDao.findByVendorAndRfq(savedVendor,
						savedRfq);
				GmtRfqVendors gmtRfqVendors= new GmtRfqVendors();
				

				// ✅ STEP 4: Insert or update
				if (existing != null) {
					logger.info("Updating status to Requested as record already exists");
					
				} else {
					logger.info("Saving status to requested for the first time");
					gmtRfqVendors.setStatus(resultStatus);
					gmtRfqVendors.setRfq(savedRfq);
					gmtRfqVendors.setVendor(savedVendor);
					gmtRfqVendors.setRequestedDate(new Date());
					gmtRfqVendorDao.save(gmtRfqVendors);
				}

				// ✅ STEP 5: Update RFQ count
				rfqDao.updateCount(savedRfq);

				// ✅ STEP 6: Deduct one RFQ credit
				orgDao.updateRfqCreditsAndUsage(savedVendor.getId());
			});
		}

		// here need to set rfqvendors object
		try {
			sendRfqToVendors(rfqVendors, savedRfq);
		} catch (MessagingException e) {
			e.printStackTrace();
		}

		// sendRfqToVendors(rfq.getRfqVendors());
		logger.info("RFQ Created Successfully" + rfq.toString());
		status = true;

		return status;
	}

	

//	private boolean saveVendorsForForwardRfq(Rfq rfq, Rfq savedRfq) {
//		logger.info("Entered to save Vendors For RFQ");
//		List<RfqVendor> rfqVendors = new ArrayList<>();
//		List<Organization> vendorList = rfq.getVendors();
//		User userDetails =null;
//		
//		boolean status = false;
//
//		if (!CollectionUtils.isEmpty(vendorList)) {
//			vendorList.forEach(vendor -> {
//				Organization savedVendor;
//				User savedUserDetails;
//				if (vendor.getId() == null) {
//					// Save the new organization
//					OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
//					MasterStatus vendorStatus = masterStatusDao.findByStatus(StatusConstants.VENDOR_ADDED);
//					MasterStatus evalStatus = masterStatusDao.findByStatus(StatusConstants.EVALUATION_NOT_STARTED);
//
//					if (selfRegistrationService.checkOrgexist(vendor.getCompanyName())) {
//						// Exception occurs when User is already associated to an Account/registered
//						throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
//								"Vendor Already Registered with name " + vendor.getCompanyName(),
//								ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
//					}
//
//					vendor.setOrgType(orgTypeObject);
//					vendor.setVendorStatus(vendorStatus);
//					vendor.setStatus(evalStatus);
//					vendor.setSourceType(ApplicationConstants.TOOL);
//					vendor.setVendorcategory(savedRfq.getCategory());
//					savedVendor = orgDao.save(vendor);
//				} else {
//					// Use the existing organization
//					logger.info("Saving the existing vendor with other email");
//					savedVendor = vendor;
//					if (vendor.getOtherEmails() != null) {
//						orgDao.updateOtherEmail(vendor.getOtherEmails(), vendor.getId());
//					}
//				}
//				
//				// Create user for the organization
//				User user = new User();
//				user.setOrg(savedVendor);
//				savedUserDetails = setUserDetails(vendor, user); // throws AppException if duplicate or failure
//				
//				// Build and save the RfqVendor object
//				// Build the RfqVendor object
//				savedVendor = orgDao.findById(savedVendor.getId()).get();
//				RfqVendor rfqVendor = new RfqVendor();
//				logger.info("setting vendor id to rfqvendor{}", savedVendor.getId());
//				rfqVendor.setOrganization(savedVendor);
//				rfqVendor.setRfq(savedRfq);
//				rfqVendor.setRequestType(vendor.getRequestType());
//
//				// Collect RfqVendor object
//				rfqVendors.add(rfqVendor);
//				// ✅ STEP 2: Get status
//				MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.RFQ_FORWARDED);
//
//				MasterStatus inviteStatus = masterStatusDao.findByStatus(StatusConstants.RFQ_INVITED);
//
//				Organization org = orgDao.findById(savedVendor.getId()).get();
//				 //✅ STEP 3: Check if record exists
//				GmtRfqVendors existing = gmtRfqVendorDao.findByVendorAndRfq(savedVendor,
//						savedRfq);
//				GmtRfqVendors gmtRfqVendors= new GmtRfqVendors();
//				
//
//				// ✅ STEP 4: Insert or update
//				if (existing != null) {
//					logger.info("Updating status to Requested as record already exists");
//					
//				} else {
//					logger.info("Saving status to requested for the first time");
//					if(vendor.getRequestType().equalsIgnoreCase(ApplicationConstants.Invite))
//					{
//						gmtRfqVendors.setStatus(inviteStatus);
//					}
//					else {
//						gmtRfqVendors.setStatus(resultStatus);
//					}
//					
//					gmtRfqVendors.setRfq(savedRfq);
//					gmtRfqVendors.setVendor(savedVendor);
//					gmtRfqVendors.setRequestedDate(new Date());
//					gmtRfqVendorDao.save(gmtRfqVendors);
//				}
////
////				// ✅ STEP 5: Update RFQ count
////				rfqDao.updateCount(savedRfq);
////
////				// ✅ STEP 6: Deduct one RFQ credit
////				orgDao.updateRfqCreditsAndUsage(savedVendor.getId());
//		});
//		}
//
//		// here need to set rfqvendors object
//		try {
//			sendRfqToVendors(rfqVendors, savedRfq);
//		} catch (MessagingException e) {
//			e.printStackTrace();
//		}
//
//		// sendRfqToVendors(rfq.getRfqVendors());
//		logger.info("RFQ Created Successfully" + rfq.toString());
//		status = true;
//
//		return status;
//	}
	
	private boolean saveVendorsForForwardRfq(Rfq rfq, Rfq savedRfq) {
		logger.info("Entered to save Vendors For RFQ");
		List<RfqVendor> rfqVendors = new ArrayList<>();
		List<Organization> vendorList = rfq.getVendors();
		
		boolean status = false;

		if (!CollectionUtils.isEmpty(vendorList)) {
			for (Organization vendor : vendorList) {
				logger.info("Vendor List Size : {} ",vendorList.size());			    
				Organization savedVendor;
			    User savedUserDetails;
			    Organization existingVendor=null;
			    if(vendor.getId()!=null) {
			    	existingVendor = orgDao.findById(vendor.getId()).orElse(null);
			    }
			   
			    if (existingVendor == null) {
			        // Save the new organization
			        OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
			        MasterStatus vendorStatus = masterStatusDao.findByStatus(StatusConstants.VENDOR_ADDED);
			        MasterStatus evalStatus = masterStatusDao.findByStatus(StatusConstants.EVALUATION_NOT_STARTED);

			        if (selfRegistrationService.checkOrgexist(vendor.getCompanyName())) {
			            throw new AppException(
			                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
			                    "Vendor Already Registered with name " + vendor.getCompanyName(),
			                    ApplicationConstants.BUSSINESS_EXCEPTION,
			                    ApplicationConstants.FAILURE);
			        }

			        vendor.setOrgType(orgTypeObject);
			        vendor.setVendorStatus(vendorStatus);
			        vendor.setStatus(evalStatus);
			        vendor.setSourceType(ApplicationConstants.TOOL);
			        vendor.setVendorcategory(savedRfq.getCategory());
			        vendor.setGmtName("GMT Basic");
					vendor.setBfsName(StatusConstants.BFS_PRO);
					vendor.setOrganizationPhonenumber(vendor.getOrganizationPhonenumber());

			        savedVendor = orgDao.save(vendor);

			    } else {
			        logger.info("Saving the existing vendor with other email");
			        logger.info("Vendor org UUid : {} ",vendor.getId());
                   vendor.setOrganizationPhonenumber(existingVendor.getOrganizationPhonenumber());
			        savedVendor = vendor;

			        if (vendor.getOtherEmails() != null) {
			            orgDao.updateOtherEmail(vendor.getOtherEmails(), vendor.getId());
			        }
			    }

			    User user = new User();
			    user.setOrg(savedVendor);
			    savedUserDetails = setUserDetails(vendor, user);

			    savedVendor = orgDao.findById(savedVendor.getId()).get();

			    RfqVendor rfqVendor = new RfqVendor();
			    rfqVendor.setOrganization(savedVendor);
			    rfqVendor.setRfq(savedRfq);
			    rfqVendor.setRequestType(vendor.getRequestType());

			    rfqVendors.add(rfqVendor);

			    MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.RFQ_FORWARDED);
			    MasterStatus inviteStatus = masterStatusDao.findByStatus(StatusConstants.RFQ_INVITED);

			    GmtRfqVendors existing = gmtRfqVendorDao.findByVendorAndRfq(savedVendor, savedRfq);
			    GmtRfqVendors gmtRfqVendors = new GmtRfqVendors();

			    if (existing == null) {
			        if (vendor.getRequestType().equalsIgnoreCase(ApplicationConstants.Invite)) {
			            gmtRfqVendors.setStatus(inviteStatus);
			        } else {
			            gmtRfqVendors.setStatus(resultStatus);
			        }

			        gmtRfqVendors.setRfq(savedRfq);
			        gmtRfqVendors.setVendor(savedVendor);
			        gmtRfqVendors.setRequestedDate(new Date());

			        gmtRfqVendorDao.save(gmtRfqVendors);
			    } else {
			        logger.info("Updating status to Requested as record already exists");
			    }
			}
		}

		// here need to set rfqvendors object
		try {
			sendRfqToVendors(rfqVendors, savedRfq);
		} catch (MessagingException e) {
			e.printStackTrace();
		}

		// sendRfqToVendors(rfq.getRfqVendors());
		logger.info("RFQ Created Successfully" + rfq.toString());
		status = true;

		return status;
	}

	public User setUserDetails(Organization organization, User user) throws AppException {
		logger.info("Setting user details for email: {} and phone: {}", organization.getEmail(),
				organization.getOrganizationPhonenumber());
		
		String mobile ="";
		if(!organization.getOrganizationPhonenumber().startsWith("+91")) {
			mobile= "+91"+organization.getOrganizationPhonenumber();
		}else {
			mobile=organization.getOrganizationPhonenumber();
		}
		User savedUser;

		// Check for duplicate user
	    User existingUsers = userDao.findByUsernameAndPhoneAndActive(
	            organization.getEmail(),mobile
	           ,true
	    );
	    logger.info("User from DB : {}",existingUsers);

	    if (existingUsers!=null) {
	    	logger.info("Existing User..");
	    	return existingUsers;
	      
	    }
	   

		// Fetch client status
		MasterStatus status = masterStatusDao.findByStatus(StatusConstants.SELF_REGISTER_VC_ACCEPTED);
		if (status == null) {
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"Default client status not configured. Contact admin.", null, null, LocalDateTime.now());
		}

		// Fetch role
		Role initiatorRole = roleDao.findByRoleNameAndActive(StatusConstants.VENDOR, true);
		if (initiatorRole == null) {
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"Client initiator role not configured. Contact admin.", null, null, LocalDateTime.now());
		}

		// Populate user
		user.setUsername(organization.getEmail());
		user.setFullName(organization.getName());
		user.setPhone(PhoneNumberUtils.normalize(organization.getOrganizationPhonenumber()));
		user.setResetPassword(true);
		user.setActive(true);
	//	user.setSelfClient(true);
	//	user.setClientStatus(status);
		user.setRole(initiatorRole);
		user.setUniqueId(generateUserId(organization.getOrganizationPhonenumber()));
		user.setSourceType(organization.getSourceType());
		user.setPassword(new String("Welcome@123"));
	//	user.setApproved(true);
		user.setVerificationStatus(StatusConstants.EMAIL_VERIFIED);
		

		// Save user
		try {
			savedUser = userDao.save(user);
			logger.info("User saved successfully with ID: {}", user.getId());
		} catch (Exception e) {
			logger.error("Failed to save user", e);
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to save user. Please try again.",
					null, null, LocalDateTime.now());
		}
		logger.info("End of Save User Details Method..");
		return savedUser;
	}

	public String generateUserId(String mobileNumber) {
		// Validate mobile number length
		if (mobileNumber == null || mobileNumber.length() < 4) {
			throw new IllegalArgumentException("Invalid mobile number");
		}

		// Get last 4 digits
		String last4Digits = mobileNumber.substring(mobileNumber.length() - 4);

		// Get current timestamp
		String timestamp = String.valueOf(System.currentTimeMillis());

		// Generate 3-digit random number
		int randomSuffix = new Random().nextInt(900) + 100; // Range: 100–999

		// Concatenate to form user ID
		String userId = "USR" + timestamp + last4Digits + randomSuffix;

		return userId;
	}

	public boolean sendRfqToVendors(List<RfqVendor> rfq, Rfq rfqData) throws MessagingException {
		logger.info("Entered to sendRfqToVendors()");
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = userDetails.getUsername();
		//phone=userDetails.get check to get phone number 
		String[] mailIdWrapper = new String[1]; // Using an array to wrap mailId
		String[] passwordWrapper = new String[1];
		//Updated Delivery Date
		String rfqDueDate = buildingRfqDueDate(rfqData.getDeliveryDate());
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
			for(Organization vendor : vendors) {
				
				boolean isExistingUser=true;

				try {
					
					String mobile ="";
					if(!vendor.getOrganizationPhonenumber().startsWith("+91")) {
						mobile= "+91"+vendor.getOrganizationPhonenumber();
					}else {
						mobile=vendor.getOrganizationPhonenumber();
					}
					logger.info("Inside RFQ Forwarding mail Block...");
					logger.info("email : {}  phone : {}", vendor.getEmail(),vendor.getOrganizationPhonenumber());
					User user = userDao.findByUsernameAndPhoneAndActive(
				            vendor.getEmail(),
				            mobile,true
				    );
					logger.info("User: {}",user);
					
					Date createdTS = user.getCreatedTS();
					

					LocalDate createdDate = createdTS.toInstant()
					        .atZone(ZoneId.systemDefault())
					        .toLocalDate();
					logger.info("CreatedTs : {} ",createdDate);

					LocalDate today = LocalDate.now();
					logger.info("Todays Date : {} ",today);

					if (createdDate.equals(today)) {
						 logger.info("created today");
						 isExistingUser=false;
						
					} else {
						isExistingUser=true;
					}
					
					String requestType = vendor.getRequestType(); // assuming you have this field in RFQ
					logger.info("Request Type : {}",requestType);

					if ("Forward".equalsIgnoreCase(requestType)) {
						if(isExistingUser==false) {
							long count = rfqVendorDao.countCredentialEmailsSent(vendor.getId());
							RfqVendor rfqVendor = rfqVendorDao.findLatestByOrganizationUuid(vendor.getId());
							logger.info("id : {}",rfqVendor.getId());
							logger.info("notification status : {}",rfqVendor.getIsRfqNotified());
		                       if (count==0) {
		                         rfqVendor.setIsRfqNotified((byte) 1);
		                          rfqVendorDao.save(rfqVendor);
		                          logger.info("New User Forward Email with credentials...");
									MailUtility.emailNewRfqForNoPR(subjectPrefix,"NewRfq", javaMailSender, rfqData, host, vendor.getEmail(),
											username, vendor.getOtherEmails(), phoneNumber, rfqDueDate, fullName, mailIdWrapper[0],
											passwordWrapper[0], vendor.getId(),vendor.getOrganizationPhonenumber());
		                         }else {
		                        	 logger.info("New User Forward Email without credentials...");
		                        	 MailUtility.emailNewRfqForNoPRForExistingUsers(subjectPrefix,"NewRfq", javaMailSender, rfqData, host, vendor.getEmail(),
		 									username, vendor.getOtherEmails(), phoneNumber, rfqDueDate, fullName, mailIdWrapper[0],
		 									passwordWrapper[0], vendor.getId(),vendor.getOrganizationPhonenumber());
		                         }
							
							
						}else {
							logger.info("Existing User Forward Email...");
							MailUtility.emailNewRfqForNoPRForExistingUsers(subjectPrefix,"NewRfq", javaMailSender, rfqData, host, vendor.getEmail(),
									username, vendor.getOtherEmails(), phoneNumber, rfqDueDate, fullName, mailIdWrapper[0],
									passwordWrapper[0], vendor.getId(),vendor.getOrganizationPhonenumber());
						}
						
					} else {
						// Send the new “invite” email
						if(isExistingUser==false) {
							
							RfqVendor rfqVendor = rfqVendorDao.findLatestByOrganizationUuid(vendor.getId());
							long count = rfqVendorDao.countCredentialEmailsSent(vendor.getId());
		                       if (count==0) {
		                         rfqVendor.setIsRfqNotified((byte) 1);
		                          rfqVendorDao.save(rfqVendor);
		                          logger.info("New User Invite Email with credentials...");
							        MailUtility.emailInviteRfq(javaMailSender, rfqData, host, vendor.getEmail(), username,
											vendor.getOtherEmails(), phoneNumber, fullName, mailIdWrapper[0], passwordWrapper[0],vendor.getOrganizationPhonenumber());
									MailUtility.emailSendVendorLoginCredentials(javaMailSender, host, vendor.getEmail(), username,
											vendor.getOtherEmails(), phoneNumber, fullName, mailIdWrapper[0], passwordWrapper[0],vendor.getOrganizationPhonenumber());
		                         }else {
		                        	 logger.info("New User Invite Email without credentials...");
		                        	 MailUtility.emailInviteRfqForExistingUsers(javaMailSender, rfqData, host, vendor.getEmail(), username,
												vendor.getOtherEmails(), phoneNumber, fullName, mailIdWrapper[0], passwordWrapper[0],vendor.getOrganizationPhonenumber());
		                         }
					       
						}else {
							
							 logger.info("Existing User Invite Email...");
						        MailUtility.emailInviteRfqForExistingUsers(javaMailSender, rfqData, host, vendor.getEmail(), username,
										vendor.getOtherEmails(), phoneNumber, fullName, mailIdWrapper[0], passwordWrapper[0],vendor.getOrganizationPhonenumber());
						}
						
					}
					
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}

			return true;
		} catch (Exception e) {
			 logger.error("Exception in sendRfqToVendors()", e);
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
					saveVendorsForForwardRfq(rfq, rfqResponse);
				} else {
					logger.info("No RFQ's Found");
				}

			}
			return true;
		} catch (Exception e) {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.No_RFQ_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);

		}

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
		logger.info("Entered To Disable User");

		Optional<User> userfound = userDao.findById(user.getId());
		if (userfound.isPresent()) {
			userDao.deactiveUser(user.getId());
			logger.info("Deactivated User");
			String orgId = userDao.findOrgIdByUser(user.getId());
			List<String> emails = userDao.findByOrg(orgId);
			if (!emails.isEmpty() && emails != null) {
				orgDao.updateEmailByOrg(orgId, emails.get(0));
			} else {
				String email = null;
				orgDao.updateEmailByOrg(orgId, email);
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

			if (updatedRfq.getDeliveryDate() == null && existingRfq.getDeliveryDate() != null) {
				updatedRfq.setDeliveryDate(existingRfq.getDeliveryDate());
			}

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
				updatedDeliveryLocation.forEach(clientDeliveryLocation -> {
					clientDeliveryLocation.setCity(sanitizeLocationField(clientDeliveryLocation.getCity()));
					clientDeliveryLocation.setState(sanitizeLocationField(clientDeliveryLocation.getState()));
					clientDeliveryLocation.setPincode(sanitizeLocationField(clientDeliveryLocation.getPincode()));
					clientDeliveryLocation.setAddress(sanitizeLocationField(clientDeliveryLocation.getAddress()));
					clientDeliveryLocation.setRfq(updatedRfq);
				});
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
			List<GmtRfqSellerDto> vendors = getVendorsByGmtRfq(updatedRfq);
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

	public boolean resendRfqToVendors(List<GmtRfqSellerDto> vendors, Rfq rfqData) throws MessagingException {
		logger.info("Entered to sendRfqToVendors()");
		try {
			UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
					.getPrincipal();
			String username = userDetails.getUsername();
			String[] mailIdWrapper = new String[1]; // Using an array to wrap mailId
			String[] passwordWrapper = new String[1];

			String rfqDueDate = buildingRfqDueDate(rfqData.getDeliveryDate());
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
			for (GmtRfqSellerDto gmtVendor : vendors) {
				String vendorId = gmtVendor.getVendorUuid();
				Organization orgData = orgDao.findById(vendorId).get();
				String vendorPhoneNumber = orgData.getOrganizationPhonenumber();
				logger.info("Getting Users List");
				List<String> usersList = userDao.findByOrg(vendorId);
				if (!CollectionUtils.isEmpty(usersList)) {
					MailUtility.emailNewRfqForNoPR(subjectPrefix,"NewRfq", javaMailSender, rfqData, host, usersList.get(0), username,
							gmtVendor.getOtherEmails(), phoneNumber, rfqDueDate, fullName, mailIdWrapper[0],
							passwordWrapper[0], vendorId,vendorPhoneNumber);
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
		logger.info("Request received for RFQ creation by client: rfqId={}, projectDesc='{}', category='{}', itemCount={}",
				rfq.getRfqId(), rfq.getProjectDesc(), rfq.getCategory(),
				rfq.getRfqItem() != null ? rfq.getRfqItem().size() : 0);

		Map<String, Object> result = new HashMap<>();

		try {
			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);
			MasterStatus newStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW);

			rfq.setByClient(true);
			rfq.setStatus(resultStatus);
			rfq.setClientStatus(newStatus);

			// String rfqId = selfRegistrationService.generateId("RFQ");
			String rfqId = automaticRfqService.generateRfqId("RFQ");
			logger.info("Generated RFQ Id: {}", rfqId);
			rfq.setRfqId(rfqId);

			List<RfqItem> rfqItems = rfq.getRfqItem();
			List<GmtItems> gmtItems = rfqItems.stream().map(this::mapRfqItemToGmtItem).collect(Collectors.toList());
			gmtItemsDao.saveAll(gmtItems);
			logger.info("Saved RFQ items in GMT Items");
			// add rfqitems categories here
			rfqItems.forEach(rfqItem -> {
			    String brand = Stream.of(rfqItem.getRemarks(), rfqItem.getBrand())
			            .filter(Objects::nonNull)
			            .collect(Collectors.joining(" "));
			    rfqItem.setBrand(brand);
			});

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

//	@Override
//	public boolean sendEmail(EmailRequest emailRequest) {
//		 try {
//	            SimpleMailMessage message = new SimpleMailMessage();
//	            
//	            // Set recipients
//	            if (emailRequest.getTo() != null && !emailRequest.getTo().isEmpty()) {
//	                message.setTo(emailRequest.getTo().toArray(new String[0]));
//	            }
//
//	            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
//	                message.setCc(emailRequest.getCc().toArray(new String[0]));
//	            }
//
//	            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
//	                message.setBcc(emailRequest.getBcc().toArray(new String[0]));
//	            }
//
//	            // Set subject and body
//	            message.setSubject(emailRequest.getSubject());
//	            message.setText(emailRequest.getBody());
//
//	            // Send email
//	            javaMailSender.send(message);
//
//	            return true;
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	            return false;
//	        }
//	    }

//	public MessageResponse sendEmail(EmailRequest emailRequest) {
//		List<String> invalidEmails = new ArrayList<>();
//
//		// Validate format + MX record
//		EmailValidatorUtil.validateEmails(emailRequest.getTo(), invalidEmails);
//		EmailValidatorUtil.validateEmails(emailRequest.getCc(), invalidEmails);
//		EmailValidatorUtil.validateEmails(emailRequest.getBcc(), invalidEmails);
//
//		if (!invalidEmails.isEmpty()) {
//			return new MessageResponse("206", "Invalid email addresses found", invalidEmails, new Date(),
//					"Partial Failure", null);
//		}
//
//		// Attempt SMTP verification
//		List<String> smtpInvalidEmails = verifyRecipientsSMTP(emailRequest);
//		if (!smtpInvalidEmails.isEmpty()) {
//			return new MessageResponse("206", "Some recipients could not be verified", smtpInvalidEmails, new Date(),
//					"Partial Failure", null);
//		}
//
//		// Prepare and send message
//		SimpleMailMessage message = new SimpleMailMessage();
//		if (emailRequest.getTo() != null)
//			message.setTo(emailRequest.getTo().toArray(new String[0]));
//		if (emailRequest.getCc() != null)
//			message.setCc(emailRequest.getCc().toArray(new String[0]));
//		if (emailRequest.getBcc() != null)
//			message.setBcc(emailRequest.getBcc().toArray(new String[0]));
//		message.setSubject(emailRequest.getSubject());
//		message.setText(emailRequest.getBody());
//
//		try {
//			javaMailSender.send(message);
//			return new MessageResponse("200", "Email sent successfully", null, new Date(), "Success", null);
//		} catch (MailSendException e) {
//			return new MessageResponse("500", "Failed to send email", List.of(e.getMessage()), new Date(), "Error",
//					null);
//		}
//	}
	

	 public MessageResponse sendEmail(EmailRequest emailRequest) {

	        List<String> invalidEmails = new ArrayList<>();

	        EmailValidatorUtil.validateEmails(emailRequest.getTo(), invalidEmails);
	        EmailValidatorUtil.validateEmails(emailRequest.getCc(), invalidEmails);
	        EmailValidatorUtil.validateEmails(emailRequest.getBcc(), invalidEmails);

	        if (!invalidEmails.isEmpty()) {
	            return new MessageResponse(
	                "206", "Invalid email addresses found",
	                invalidEmails, new Date(), "Partial Failure", null
	            );
	        }

	        boolean hasAttachments =
	            emailRequest.getAttachments() != null &&
	            !emailRequest.getAttachments().isEmpty();

	        try {
	            if (!hasAttachments) {

	                // ===== SIMPLE EMAIL =====
	                SimpleMailMessage message = new SimpleMailMessage();

	                if (emailRequest.getTo() != null)
	                    message.setTo(emailRequest.getTo().toArray(new String[0]));
	                if (emailRequest.getCc() != null)
	                    message.setCc(emailRequest.getCc().toArray(new String[0]));
	                if (emailRequest.getBcc() != null)
	                    message.setBcc(emailRequest.getBcc().toArray(new String[0]));

	                message.setSubject(emailRequest.getSubject());
	                message.setText(emailRequest.getBody());

	                javaMailSender.send(message);

	            } else {

	                // ===== EMAIL WITH ATTACHMENTS =====
	                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
	                MimeMessageHelper helper =
	                    new MimeMessageHelper(mimeMessage, true);

	                if (emailRequest.getTo() != null)
	                    helper.setTo(emailRequest.getTo().toArray(new String[0]));
	                if (emailRequest.getCc() != null)
	                    helper.setCc(emailRequest.getCc().toArray(new String[0]));
	                if (emailRequest.getBcc() != null)
	                    helper.setBcc(emailRequest.getBcc().toArray(new String[0]));

	                helper.setSubject(emailRequest.getSubject());
	                helper.setText(emailRequest.getBody(), false);

	                // 🔥 BASE64 → byte[] (CORRECT)
	                for (EmailAttachment attachment : emailRequest.getAttachments()) {

	                    if (attachment.getFileData() == null || attachment.getFileData().isBlank()) {
	                        throw new IllegalArgumentException(
	                            "Base64 data missing for file: " + attachment.getFileName()
	                        );
	                    }

	                    // Remove whitespace / line breaks
	                    String base64 =
	                        attachment.getFileData().replaceAll("\\s+", "");

	                    byte[] decodedBytes;
	                    try {
	                        decodedBytes = Base64.getMimeDecoder().decode(base64);
	                    } catch (IllegalArgumentException ex) {
	                        throw new IllegalArgumentException(
	                            "Invalid Base64 content for file: " + attachment.getFileName()
	                        );
	                    }

	                    attachment.setDecodedFileData(decodedBytes);

	                    helper.addAttachment(
	                        attachment.getFileName(),
	                        new ByteArrayResource(decodedBytes),
	                        attachment.getContentType()
	                    );
	                }

	                javaMailSender.send(mimeMessage);
	            }

	            return new MessageResponse(
	                "200", "Email sent successfully",
	                null, new Date(), "Success", null
	            );

	        } catch (MailSendException | MessagingException | IllegalArgumentException e) {
	            return new MessageResponse(
	                "500", "Failed to send email",
	                List.of(e.getMessage()), new Date(), "Error", null
	            );
	        }
	    }
	

	private List<String> verifyRecipientsSMTP(EmailRequest emailRequest) {
		List<String> invalid = new ArrayList<>();

		// Setup mail session for verification (no auth, just SMTP check)
		Properties props = new Properties();
		props.put("mail.smtp.host", "your.smtp.server"); // Replace with your SMTP host
		props.put("mail.smtp.port", "25"); // Replace if needed
		props.put("mail.smtp.timeout", "5000");
		props.put("mail.smtp.connectiontimeout", "5000");

		Session session = Session.getInstance(props, null);

		// Combine all recipients
		List<String> allRecipients = new ArrayList<>();
		if (emailRequest.getTo() != null)
			allRecipients.addAll(emailRequest.getTo());
		if (emailRequest.getCc() != null)
			allRecipients.addAll(emailRequest.getCc());
		if (emailRequest.getBcc() != null)
			allRecipients.addAll(emailRequest.getBcc());

		for (String recipient : allRecipients) {
			try {
				// Attempt RCPT TO command using Transport (SMTP)
				MimeMessage msg = new MimeMessage(session);
				msg.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
				msg.setFrom(new InternetAddress("no-reply@yourdomain.com"));
				msg.setSubject("Verification");
				msg.setText("Testing recipient existence");

				Transport transport = session.getTransport("smtp");
				transport.connect(); // no auth if allowed
				transport.sendMessage(msg, msg.getAllRecipients());
				transport.close();
			} catch (SendFailedException e) {
				// Failed recipient
				invalid.add(recipient);
			} catch (Exception e) {
				// Ignore connection issues for public providers
			}
		}

		return invalid;
	}

	@Override
	public List<SubscriptionPlan> getSubscriptionPlans() {
		return subscriptionPlanDao.findAll();
	}

//	@Override
//	public List<RfqStatusResponse> getRfqStatuses(RfqStatusRequest request) {
//		List<RfqStatusResponse> responses = new ArrayList<>();
//
//		if (request.getRfqIds() != null && !request.getRfqIds().isEmpty()) {
//			// Normalize input: ensure every rfqId starts with "RFQ"
//			List<String> normalizedIds = request.getRfqIds().stream()
//					.map(id -> id != null && id.startsWith("RFQ") ? id : "RFQ" + id).collect(Collectors.toList());
//			// Fetch matching RFQs from DB
//			List<Rfq> rfqs = rfqDao.findByUserAndRfqIdIn(request.getClientId(), normalizedIds);
//
//			// Map RFQs by ID for quick lookup
//			Map<String, Rfq> rfqMap = rfqs.stream().collect(Collectors.toMap(Rfq::getRfqId, r -> r));
//
//			// Always include all requested IDs
//			for (String rfqId : normalizedIds) {
//				RfqStatusResponse dto = new RfqStatusResponse();
//				dto.setRfqid(rfqId);
//
//				if (rfqMap.containsKey(rfqId)) {
//					Rfq r = rfqMap.get(rfqId);
//					dto.setStatus(r.getStatus() != null ? r.getStatus().getUiDisplay() : "Unknown");
//				} else {
//					dto.setStatus("Invalid RFQID");
//				}
//
//				responses.add(dto);
//			}
//		} else {
//			// No rfqIds provided → fetch last 3
//			List<Rfq> rfqs = rfqDao.findLast3ByClientId(request.getClientId(), PageRequest.of(0, 3));
//
//			if (rfqs.isEmpty()) {
//				// Client has no RFQs at all
//				RfqStatusResponse dto = new RfqStatusResponse();
//				dto.setRfqid(null);
//				dto.setStatus("Not Found");
//				responses.add(dto);
//			} else {
//				for (Rfq r : rfqs) {
//					RfqStatusResponse dto = new RfqStatusResponse();
//					dto.setRfqid(r.getRfqId());
//					dto.setStatus(r.getStatus() != null ? r.getStatus().getUiDisplay() : "Unknown");
//					responses.add(dto);
//				}
//			}
//		}
//
//		return responses;
//	}
	
	  @Override
	    public List<RfqStatusResponse> getRfqStatuses(RfqStatusRequest request) {

	        List<RfqStatusResponse> responses = new ArrayList<>();

	        if (request.getRfqIds() != null && !request.getRfqIds().isEmpty()) {

	            // Normalize RFQ IDs → ensure RFQ prefix
	            List<String> normalizedIds = request.getRfqIds().stream()
	                    .map(id -> id != null && id.startsWith("RFQ") ? id : "RFQ" + id)
	                    .collect(Collectors.toList());

	            // Fetch RFQs
	            List<Rfq> rfqs = rfqDao.findByUserAndRfqIdIn(
	                    request.getClientId(), normalizedIds);

	            // Map for quick lookup
	            Map<String, Rfq> rfqMap = rfqs.stream()
	                    .collect(Collectors.toMap(Rfq::getRfqId, r -> r));

	            // Always return all requested RFQs
	            for (String rfqId : normalizedIds) {
	                RfqStatusResponse dto = new RfqStatusResponse();
	                dto.setRfqid(rfqId);

	                if (rfqMap.containsKey(rfqId)) {
	                    dto.setStatus(resolveRfqStatus(rfqMap.get(rfqId)));
	                } else {
	                    dto.setStatus("Invalid RFQID");
	                }

	                responses.add(dto);
	            }

	        } else {
	            // No RFQ IDs → fetch last 3
	            List<Rfq> rfqs = rfqDao.findLast3ByClientId(
	                    request.getClientId(), PageRequest.of(0, 3));

	            if (rfqs.isEmpty()) {
	                RfqStatusResponse dto = new RfqStatusResponse();
	                dto.setRfqid(null);
	                dto.setStatus("Not Found");
	                responses.add(dto);
	            } else {
	                for (Rfq r : rfqs) {
	                    RfqStatusResponse dto = new RfqStatusResponse();
	                    dto.setRfqid(r.getRfqId());
	                    dto.setStatus(resolveRfqStatus(r));
	                    responses.add(dto);
	                }
	            }
	        }

	        return responses;
	    }

	    /**
	     * Centralized RFQ status resolution logic
	     */
	    private String resolveRfqStatus(Rfq rfq) {

	        // 1️⃣ Highest priority
	        if (Boolean.TRUE.equals(rfq.isQuotationReceived())) {
	            return "Quotation_Received";
	        }

	        // 2️⃣ IN_PROGRESS mapping
	        if (rfq.getStatus() != null) {
	            String statusCode = rfq.getStatus().getUiDisplay(); // or getName()

	            if ("InProgress".equalsIgnoreCase(statusCode)) {
	                return "Published To Multiple Sellers";
	            }

	            return rfq.getStatus().getUiDisplay();
	        }

	        return "Unknown";
	    }
	@Override
	public List<RfqStatusResponse> getRfqSellerStatuses(RfqStatusRequest request) {

	    List<RfqStatusResponse> responses = new ArrayList<>();

	    if (request.getRfqIds() != null && !request.getRfqIds().isEmpty()) {

	        // Normalize RFQ IDs
	        List<String> normalizedIds = request.getRfqIds().stream()
	                .map(id -> id != null && id.startsWith("RFQ") ? id : "RFQ" + id)
	                .toList();

	        List<Rfq> rfqs =
	                rfqDao.findRfqsByIdsExcludingSellerRequested(
	                        	                        normalizedIds,request.getClientId()
	                );

	        Map<String, Rfq> rfqMap = rfqs.stream()
	                .collect(Collectors.toMap(Rfq::getRfqId, r -> r));

	        for (String rfqId : normalizedIds) {
	            RfqStatusResponse dto = new RfqStatusResponse();
	            dto.setRfqid(rfqId);

	            if (rfqMap.containsKey(rfqId)) {
	                Rfq r = rfqMap.get(rfqId);
	                dto.setStatus(
	                        r.getStatus() != null
	                                ? r.getStatus().getUiDisplay()
	                                : "Unknown"
	                );
	            } else {
	                // Either seller-requested RFQ or invalid RFQ
	                dto.setStatus("Invalid RFQID");
	            }
	            responses.add(dto);
	        }

	    } else {

	        // No RFQ IDs → fetch latest 5 buyer RFQs
	        List<Rfq> rfqs =
	                rfqDao.findLatest5RfqsExcludingSellerRequested(
	                        request.getClientId(),
	                        PageRequest.of(0, 5)
	                );

	        if (rfqs.isEmpty()) {
	            RfqStatusResponse dto = new RfqStatusResponse();
	            dto.setRfqid(null);
	            dto.setStatus("Not Found");
	            responses.add(dto);
	        } else {
	            for (Rfq r : rfqs) {
	                RfqStatusResponse dto = new RfqStatusResponse();
	                dto.setRfqid(r.getRfqId());
	                dto.setStatus(
	                        r.getStatus() != null
	                                ? r.getStatus().getUiDisplay()
	                                : "Unknown"
	                );
	                responses.add(dto);
	            }
	        }
	    }

	    return responses;
	}


	@Override
	public int getSellerRfqCredits(Organization org) {
		int credits = 0;
		logger.info("entered to get Seller Rfq Credits");
		if (org != null) {
			credits = orgDao.findRfqCreditsByOrg(org.getId());
		}

		return credits;
	}

	@Override
	public Organization getOrgById(Organization organization) {
		if (organization == null || organization.getId() == null) {
			logger.error("Organization or ID is null");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "Organization ID must not be null",
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		return orgDao.findById(organization.getId()).orElseThrow(() -> {
			logger.error("Organization not found for ID: {}", organization.getId());
			return new AppException(HttpStatus.NOT_FOUND.value(), ApplicationConstants.VENDOR_DETAILS_DOESNT_EXIST,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		});
	}

	@Override
	public Map<String, Object> getRfqByItemCategory(Organization org) {
	    Map<String, Object> result = new HashMap<>();

	    if (org == null || org.getId() == null) {
	        logger.error("Invalid organization input");
	        result.put("rfqs", Collections.emptyList());
	        result.put("count", 0L);
	        return result;
	    }

	    List<String> categoryList = orgCategoryDivisionDao.findCategoryByOrg(org.getId());
	    logger.info("Fetching RFQs by categories: {} for orgId: {}", categoryList, org.getId());

	    // Last 7 days filter
	    Calendar calendar = Calendar.getInstance();
	    calendar.add(Calendar.DAY_OF_MONTH, -7);
	    Date fromDate = calendar.getTime();

	    Pageable topFive = PageRequest.of(0, 5);

	    List<Rfq> rfqs = rfqDao.findTopRfqsByCategory(
	            categoryList,
	            org.getId(),
	            fromDate,
	            topFive
	    );

	    long totalCount = rfqDao.countByRfqItemCategory(categoryList, org.getId());

	    result.put("rfqs", rfqs);
	    result.put("count", totalCount);

	    return result;
	}
//	@Override
//	public Map<String, Object> forwardRfqsToVendor(ForwardRfqVendorRequest request) {
//	    logger.info("Entered to forwardRfqForNoPr");
//
//	    Map<String, Object> response = new HashMap<>();
//	    Map<String, List<Map<String, Object>>> results = new HashMap<>();
//	    List<Map<String, Object>> successful = new ArrayList<>();
//	    List<Map<String, Object>> failed = new ArrayList<>();
//
//	    for (String rfqId : request.getRfqIds()) {
//	        Map<String, Object> result = new HashMap<>();
//	        result.put("rfq_id", rfqId);
//	        result.put("seller_id", request.getSellerId());
//	        result.put("seller_email", request.getEmail());
//
//	        try {
//	        	 //Optional<Rfq> rfqDataOpt = rfqDao.findById(rfqId);
//	           Rfq rfqDataOpt = rfqDao.findByRfqId(rfqId);
//	            if (rfqDataOpt==null) {
//	                result.put("error", "RFQ not found");
//	                failed.add(result);
//	                continue;
//	            }
//
//	            if (!isValidEmail(request.getEmail())) {
//	                result.put("error", "Invalid email format");
//	                failed.add(result);
//	                continue;
//	            }
//
//	            Rfq rfqData = rfqDataOpt;
//
//	            // Build a single RfqVendor for this seller
//	            RfqVendor rfqVendor = new RfqVendor();
//	            rfqVendor.setRfq(rfqData);
//	            rfqVendor.setVendorId(request.getSellerId());
//	            rfqVendor.setEmail(request.getEmail());
//
//	            // Call new sendRfqToVendors method
//	            boolean emailSent = sendRfqsToVendor(rfqVendor, rfqData);
//
//	            result.put("email_sent", emailSent);
//	            if (emailSent) {
//	                successful.add(result);
//	            } else {
//	                result.put("error", "Failed to send email");
//	                failed.add(result);
//	            }
//
//	        } catch (Exception ex) {
//	            result.put("error", ex.getMessage());
//	            failed.add(result);
//	        }
//	    }
//
//	    response.put("success", true);
//	    results.put("successful", successful);
//	    results.put("failed", failed);
//	    response.put("results", results);
//
//	    return response;
//	}

	@Override
	public Map<String, Object> forwardRfqsToVendor(ForwardRfqVendorRequest request) {
		logger.info("Entered to forwardRfqsToVendor");

		Map<String, Object> response = new HashMap<>();
		Map<String, List<Map<String, Object>>> results = new HashMap<>();
		List<Map<String, Object>> successful = new ArrayList<>();
		List<Map<String, Object>> failed = new ArrayList<>();

		// Fetch organization once
		Integer availableCredits = orgDao.findRfqCreditsDataByOrg(request.getSellerId());
		if (availableCredits == null) {
			Map<String, Object> errorResult = new HashMap<>();
			for (String rfqId : request.getRfqIds()) {
				errorResult.put("rfq_id", rfqId);
				errorResult.put("email_sent", false);
				errorResult.put("error_code", "ORG_NOT_FOUND");
				errorResult.put("error", "Organization not found");
				failed.add(new HashMap<>(errorResult));
			}
			response.put("success", true);
			results.put("successful", successful);
			results.put("failed", failed);
			response.put("results", results);

			Map<String, Object> summary = new HashMap<>();
			summary.put("total_requests", request.getRfqIds().size());
			summary.put("successful", successful.size());
			summary.put("failed", failed.size());
			response.put("summary", summary);
			return response;
		}

		for (String rfqId : request.getRfqIds()) {
			Map<String, Object> result = new HashMap<>();
			result.put("rfq_id", rfqId);

			try {
				Rfq rfqData = rfqDao.findByRfqId(rfqId);
				if (rfqData == null) {
					result.put("email_sent", false);
					result.put("error_code", "RFQ_NOT_FOUND");
					result.put("error", "RFQ is not found");
					failed.add(result);
					continue;
				}

				if (!isValidEmail(request.getEmail())) {
					result.put("email_sent", false);
					result.put("error_code", "INVALID_EMAIL");
					result.put("error", "Invalid email format");
					failed.add(result);
					continue;
				}

				if (availableCredits <= 0) {
					result.put("email_sent", false);
					result.put("error_code", "NO_CREDITS");
					result.put("error", "Organization has no RFQ credits");
					failed.add(result);
					continue;
				}

				// Build RfqVendor object
				RfqVendor rfqVendor = new RfqVendor();
				rfqVendor.setRfq(rfqData);
				rfqVendor.setVendorId(request.getSellerId());
				rfqVendor.setEmail(request.getEmail());
				
				// ✅ STEP 2: Get status
				MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.vendorApproved);

				Organization org = orgDao.findById(request.getSellerId()).get();
				// ✅ STEP 3: Check if record exists
				GmtRfqVendors existing = gmtRfqVendorDao.findByVendorAndRfq(org,
						rfqData);
				GmtRfqVendors gmtRfqVendors= new GmtRfqVendors();
				

				// ✅ STEP 4: Insert or update
				if (existing != null) {
					logger.info("Updating status to Requested as record already exists");
					
				} else {
					logger.info("Saving status to requested for the first time");
					gmtRfqVendors.setStatus(resultStatus);
					gmtRfqVendors.setRfq(rfqData);
					gmtRfqVendors.setVendor(org);
					gmtRfqVendors.setRequestedDate(new Date());
					gmtRfqVendorDao.save(gmtRfqVendors);
				}

			

				// Send email
				boolean emailSent = sendRfqsToVendor(rfqVendor, rfqData);
				result.put("email_sent", emailSent);

				if (emailSent) {
					result.put("message", "Email sent successfully");
					successful.add(result);
					// ✅ STEP 5: Update RFQ count
					rfqDao.updateCount(rfqData);

					// ✅ STEP 6: Deduct one RFQ credit
					orgDao.updateRfqCreditsAndUsage(request.getSellerId());
					// Decrement available credits in memory
					//availableCredits--;

				} else {
					result.put("error_code", "SYSTEM_ERROR");
					result.put("error", "Failed to send email or added to queue");
					failed.add(result);
				}

			} catch (Exception ex) {
				result.put("email_sent", false);
				result.put("error_code", "SYSTEM_ERROR");
				result.put("error", ex.getMessage());
				failed.add(result);
			}
		}

		// Update organization credits once at the end
		//orgDao.updateRfqCredits(request.getSellerId(), availableCredits);

		results.put("successful", successful);
		results.put("failed", failed);
		response.put("success", true);
		response.put("results", results);

		Map<String, Object> summary = new HashMap<>();
		summary.put("total_requests", request.getRfqIds().size());
		summary.put("successful", successful.size());
		summary.put("failed", failed.size());
		response.put("summary", summary);

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
			
			String vendorPhoneNumber = vendor.getPhone();

			// Save vendor
			rfqVendorDao.save(vendor);

			// Build due date
			String rfqDueDate = buildingRfqDueDate(rfqData.getDeliveryDate());

			// Send email and return actual status
			boolean status = MailUtility.emailNewRfqForNoPR(subjectPrefix,"NewRfq", javaMailSender, rfqData, host, vendor.getEmail(), // main
																														// vendor
																														// email
					mailFom, // from
					null, // other emails
					null, // phone number
					rfqDueDate, null, // full name
					mailFom, // mailId
					emailPassword, // password
					vendor.getVendorId(),vendorPhoneNumber);

			return status; // ✅ return actual send result

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
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "User ID must not be null",
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		} 

		Optional<User> userDataOpt = userDao.findById(user.getId());

		if (userDataOpt.isEmpty()) {
			logger.error("No user found with ID: {}", user.getId());
			throw new AppException(HttpStatus.NOT_FOUND.value(), "User not found with ID: " + user.getId(),
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		User userData = userDataOpt.get();
		Organization org = userData.getOrg();

		if (org == null) {
			logger.error("Organization not linked to user ID: {}", user.getId());
			throw new AppException(HttpStatus.NOT_FOUND.value(), "Organization not linked to user",
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
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
			if (!CollectionUtils.isEmpty(rfq.getClientdeliverylocationrfq())) {
				rfqData.put("location", rfq.getClientdeliverylocationrfq().get(0).getCity() + ","
						+ rfq.getClientdeliverylocationrfq().get(0).getState());
			}
			rfqData.put("submission_deadline",  Date.from(
					rfq.getCreatedTS().toInstant().plus(5, ChronoUnit.DAYS)));
			rfqData.put("email_sent_date", gv.getRequestedDate());
			if (rfq.getRfqClosingDate() != null) {
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

	@Transactional
	@Override
	public boolean requestRfqBySellers(List<GmtRfqVendors> rfqList) {
		logger.info("Entered to request RFQs By Sellers");

		try {
			Date date = new Date();

			for (GmtRfqVendors gmtRfqVendors : rfqList) {
				String vendorId = gmtRfqVendors.getVendor().getId();

				// ✅ STEP 1: Check credits BEFORE updates
				int availableCredits = orgDao.findRfqCreditsByOrg(vendorId);
				if (availableCredits <= 0) {
					logger.warn("Vendor {} has no RFQ credits left!", vendorId);
					throw new AppException(HttpStatus.BAD_REQUEST.value(), "No RFQ credits left for this vendor", null,
							null, LocalDateTime.now());
				}

				// ✅ STEP 2: Get status
				MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.vendorApproved);

				// ✅ STEP 3: Check if record exists
				GmtRfqVendors existing = gmtRfqVendorDao.findByVendorAndRfq(gmtRfqVendors.getVendor(),
						gmtRfqVendors.getRfq());

				// ✅ STEP 4: Insert or update
				if (existing != null) {
					logger.info("Updating status to Requested as record already exists");
					gmtRfqVendorDao.updateStatus(gmtRfqVendors.getRfq(), gmtRfqVendors.getVendor(), resultStatus);
				} else {
					logger.info("Saving status to requested for the first time");
					gmtRfqVendors.setStatus(resultStatus);
					gmtRfqVendors.setRequestedDate(date);
					gmtRfqVendorDao.save(gmtRfqVendors);
				}

				// ✅ STEP 5: Update RFQ count
				rfqDao.updateCount(gmtRfqVendors.getRfq());

				// ✅ STEP 6: Deduct one RFQ credit
				orgDao.updateRfqCreditsAndUsage(vendorId);

				// ✅ STEP 7: Send email notification
				Optional<Rfq> rfqData = rfqDao.findById(gmtRfqVendors.getRfq().getId());
				if (rfqData.isPresent()) {
					String email = orgDao.findEmailById(vendorId);
					String otherEmails = orgDao.findOtherEmailById(vendorId);
					String rfqDueDate = buildingRfqDueDate(rfqData.get().getDeliveryDate());

					MailUtility.emailNewGMTRfqForNoPR("NewRfq",subjectPrefix, javaMailSender, rfqData.get(), host, email, otherEmails,
							mailFom, emailPassword, rfqDueDate, vendorId);
				}

				logger.info("RFQ requested by vendor {} processed successfully.", vendorId);
			}

			return true;

		} catch (AppException ae) {
			logger.error("Business validation error: {}", ae.getMessage());
			throw ae;
		} catch (Exception e) {
			logger.error("Error occurred while processing RFQs by vendors: {}", e.getMessage(), e);
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"Something went wrong while processing RFQ request", null, null, LocalDateTime.now());
		}
	}

	@Override
	public VendorInfoDto getVendorInfo(Organization orgRequest) {
		Optional<Organization> optOrg = orgDao.findById(orgRequest.getId());

		if (optOrg.isEmpty()) {
			return null; // Org not found
		}

		Organization org = optOrg.get();

		// Build Response
		VendorInfoDto dto = new VendorInfoDto();
		dto.setCompanyName(org.getCompanyName());
		dto.setOrganizationPhonenumber(org.getOrganizationPhonenumber());
		dto.setEmail(org.getEmail());
		dto.setGstin(org.getGstin());
		dto.setDetails(org.getDetails());
		dto.setIndia(org.isIndia());
		dto.setZipCode(org.getZipCode());
		dto.setSourceType(org.getSourceType());
		dto.setCity(org.getCity());
		// Convert entities -> List<String>
		dto.setCategories(
				org.getDivisionCategories() != null
						? org.getDivisionCategories().stream().map(OrgDivisionCategory::getCategory) // change to your
																										// actual field
								.collect(Collectors.toList())
						: List.of());

		return dto;
	}

//	@Override
//	public void emailForwarder() {
//		logger.info("Entered into emailForwarder()");
//		final String subjectPattern = "You have an Enquiry RFQ No";
//
//		// IMAPS (Gmail)
//		Properties props = new Properties();
//		props.put("mail.store.protocol", "imaps");
//		props.put("mail.imaps.host", "imap.gmail.com");
//		props.put("mail.imaps.port", "993");
//		props.put("mail.imaps.ssl.enable", "true");
//		props.put("mail.imaps.ssl.trust", "imap.gmail.com");
//
//		Session session = Session.getInstance(props);
//
//		try (Store store = session.getStore("imaps")) {
//			// mailFrom / emailPassword are your existing fields/configs
//			store.connect(mailFom, emailPassword);
//
//			IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
//			inbox.open(Folder.READ_WRITE);
//
//			// Build search: Subject AND (UNSEEN) AND (NOT already 'Processed')
//			Flags processedFlag = new Flags("Processed");
//			SearchTerm term = new AndTerm(new SubjectTerm(subjectPattern),
//					new AndTerm(new FlagTerm(new Flags(Flags.Flag.SEEN), false), new FlagTerm(processedFlag, false)));
//
//			Message[] messages = inbox.search(term);
//			logger.info("Messages to process: {}", messages.length);
//
//			for (Message message : messages) {
//				String subject = message.getSubject();
//				if (subject == null) {
//					continue;
//				}
//
//				String rfqId = extractRfqId(subject);
//				String vendorId = extractVendorId(subject);
//				if (rfqId == null || vendorId == null) {
//					logger.info("Skipping message due to missing rfqId/vendorId. Subject={}", subject);
//					// Mark seen to avoid re-hitting on next run if you want:
//					message.setFlag(Flags.Flag.SEEN, true);
//					continue;
//				}
//
//				logger.info("Processing RFQID={}, vendorId={}", rfqId, vendorId);
//				MasterStatus quoteStatus = masterStatusDao.findByStatus(StatusConstants.VENDOR_QUOTE_SUBMITTED);
//				// Your business updates
//				
//                //rfqDao.updateQuoteSubmissionDate(rfqId);
//				rfqDao.updateRfqByRfqId(rfqId,quoteStatus);
//				rfqVendorDao.updateQuotationReceived(rfqId, vendorId, quoteStatus);
//				List<String> rfqIds = rfqDao.getIdbyRfqId(rfqId);
//				if (!CollectionUtils.isEmpty(rfqIds)) {
//					gmtRfqVendorDao.updateQuotationReceived(rfqIds.get(0), vendorId, quoteStatus);
//				}
//				orgDao.updateQuoteCount(vendorId);
//
//				// Forward to the first matched user email
//				List<String> users = rfqDao.findRFQByRfQId(rfqId);
//				if (users != null && !users.isEmpty()) {
//					String forwardAddress = userDao.findEmailById(users.get(0));
//					logger.info("Forwarding to: {}", forwardAddress);
//					MailUtility.forwardMessage(forwardAddress, javaMailSender, mailFom, message, emailPassword);
//
//					// Mark as processed so we won't forward again
//					message.setFlags(processedFlag, true);
//					message.setFlag(Flags.Flag.SEEN, true);
//				} else {
//					logger.info("No users found for RFQID={}", rfqId);
//					// Optionally mark SEEN to avoid re-processing
//					message.setFlag(Flags.Flag.SEEN, true);
//				}
//			}
//
//			inbox.close(false); // don't expunge
//		} catch (Exception e) {
//			logger.error("emailForwarder failed", e);
//		}
//	}
	
	@Override
	public void emailForwarder() {
	    logger.info("Entered into emailForwarder()");

	    final String subjectPattern = subjectPrefix + " You have an Enquiry RFQ No";

	    Properties props = new Properties();
	    props.put("mail.store.protocol", "imaps");
	    props.put("mail.imaps.host", "imap.gmail.com");
	    props.put("mail.imaps.port", "993");
	    props.put("mail.imaps.ssl.enable", "true");
	    props.put("mail.imaps.ssl.trust", "imap.gmail.com");
	    // Without these, jakarta.mail waits forever on connect/read. Spring's
	    // default scheduler pool holds a single thread, so one stalled IMAP call
	    // would silently stop every other @Scheduled job in the application.
	    props.put("mail.imaps.connectiontimeout", "15000");
	    props.put("mail.imaps.timeout", "30000");
	    props.put("mail.imaps.writetimeout", "30000");

	    Session session = Session.getInstance(props);

	    try (Store store = session.getStore("imaps")) {

	        store.connect(mailFom, emailPassword);

	        IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
	        inbox.open(Folder.READ_WRITE);

	        Flags processedFlag = new Flags("Processed");

	        SearchTerm term = new AndTerm(
	                new SubjectTerm(subjectPattern),
	                new AndTerm(
	                        new FlagTerm(new Flags(Flags.Flag.SEEN), false),
	                        new FlagTerm(processedFlag, false)
	                )
	        );

	        Message[] messages = inbox.search(term);
	        logger.info("Messages found: {}", messages.length);

	        for (Message message : messages) {

	            String subject = message.getSubject();
	            if (subject == null) continue;

	            String rfqId = extractRfqId(subject);
	            String vendorId = extractVendorId(subject);

	            if (rfqId == null || vendorId == null) {
	                logger.warn("Skipping due to missing RFQ or Vendor. Subject={}", subject);
	                message.setFlag(Flags.Flag.SEEN, true);
	                continue;
	            }

	            logger.info("Processing RFQID={}, vendorId={}", rfqId, vendorId);

	            List<String> users = rfqDao.findRFQByRfQId(rfqId);
	            if (users == null || users.isEmpty()) {
	                logger.warn("No buyer user found for RFQ {}", rfqId);
	                message.setFlag(Flags.Flag.SEEN, true);
	                continue;
	            }

	            String forwardAddress = userDao.findEmailById(users.get(0));
	            logger.info("Forwarding to buyer: {}", forwardAddress);

	            boolean sent = MailUtility.forwardMessage(
	                    forwardAddress,
	                    javaMailSender,
	                    mailFom,
	                    message,
	                    emailPassword
	            );

	            if (!sent) {
	                logger.error("Forward FAILED for RFQ={} vendor={}. NOT marking as processed.", rfqId, vendorId);
	                // leave UNSEEN so scheduler retries
	                continue;
	            }

	            // ONLY update if email sent successfully
	            MasterStatus quoteStatus =
	                    masterStatusDao.findByStatus(StatusConstants.VENDOR_QUOTE_SUBMITTED);

	            rfqDao.updateRfqByRfqId(rfqId, quoteStatus);
	            rfqVendorDao.updateQuotationReceived(rfqId, vendorId, quoteStatus);

	            List<String> rfqIds = rfqDao.getIdbyRfqId(rfqId);
	            if (!CollectionUtils.isEmpty(rfqIds))
	                gmtRfqVendorDao.updateQuotationReceived(rfqIds.get(0), vendorId, quoteStatus);

	            orgDao.updateQuoteCount(vendorId);

	            // NOW mark message processed
	            message.setFlags(processedFlag, true);
	            message.setFlag(Flags.Flag.SEEN, true);

	        }

	        inbox.close(false);

	    } catch (Exception e) {
	        logger.error("emailForwarder failed", e);
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

	private static String extractVendorId(String subject) {
		// Match pattern like: RFQ No RFQ252910171633 -
		// fca2a80a-8687-4344-b4eb-6dc32969eb6a
		Pattern pattern = Pattern.compile("RFQ No [A-Za-z0-9]+\\s*-\\s*([A-Za-z0-9\\-]+)");
		Matcher matcher = pattern.matcher(subject);
		if (matcher.find()) {
			return matcher.group(1); // This will return the vendor ID
		}
		return null;
	}

	@Transactional
	public void updateVendorClasses() {
		// Get OrgType for vendor
		OrgType orgType = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);

		if (orgType == null) {
			logger.warn("Vendor OrgType not found. Skipping classification update.");
			return;
		}

		// Get all vendor organizations
		List<Organization> vendorOrgs = orgDao.findByOrgType(orgType);

		for (Organization org : vendorOrgs) {
			try {
				// Skip Opt-Out vendors
				if (StatusConstants.Opt_Out.equals(org.getVendorClass())) {
					continue;
				}

				Long rfqUsedCount = org.getRfqUsedCount() != null ? org.getRfqUsedCount() : 0L;
				Long quoteSubmitted = org.getQuoteSubmitted() != null ? org.getQuoteSubmitted() : 0L;

				String newVendorClass;

				if (quoteSubmitted >= 3) {
					newVendorClass = StatusConstants.Diamond;
				} else if (quoteSubmitted > 0 && quoteSubmitted < 3) {
					newVendorClass = StatusConstants.Gold;
				} else {
					newVendorClass = StatusConstants.Marketing;
				}

				// Update only if changed
				if (!newVendorClass.equals(org.getVendorClass())) {
					orgDao.updateVendorClass(org.getId(), newVendorClass);
					logger.info("Updated vendorClass for Org ID {} to {}", org.getId(), newVendorClass);

				}
			} catch (Exception e) {
				logger.error("Error updating vendorClass for Org ID {}", org.getId(), e);
			}

		}
	}

	@Override
	public User getBuyerDataByRFQ(Rfq rfq) {
	    if (rfq == null) {
	        logger.warn("getBuyerDataByRFQ called with null RFQ");
	        return null;
	    }

	    logger.info("Fetching buyer data for RFQ ID: {}", rfq.getId());

	    // 1. Load RFQ from DB to ensure it's managed / fresh
	    Rfq rfqData = rfqDao.findById(rfq.getId()).orElse(null);

	    if (rfqData == null) {
	        logger.warn("RFQ not found for ID: {}", rfq.getId());
	        return null;
	    }

	    logger.debug("RFQ found. Extracting userId from RFQ ID: {}", rfqData.getId());

	    // 2. Extract userId string from RFQ
	    String userIdStr = rfqData.getUser();

	    if (userIdStr == null || userIdStr.trim().isEmpty()) {
	        logger.warn("No userId found in RFQ ID: {}", rfqData.getId());
	        return null;
	    }

	    logger.info("User ID '{}' found in RFQ ID: {}", userIdStr, rfqData.getId());

	    // 3. Fetch user (assuming userDao.findUserById(String id) exists)
	    User user = userDao.findUserById(userIdStr);

	    if (user == null) {
	        logger.warn("User not found for userId: {}", userIdStr);
	    } else {
	        logger.info("User details fetched successfully for userId: {}", userIdStr);
	    }

	    return user;
	}

	public boolean generateOtp(Organization organization, HttpServletRequest request) {
		logger.info("Entered to generate OTP");
		String receiverEmail= "wesource@procucev.com";
		try {
			if (organization != null) {
				// Generate OTP
				String otp = generateOTPForEmail();

				// Store OTP and its expiration time in the map
//				otpMap.put(organization.getEmail().trim().toLowerCase()+"_MOBILE_"+organization.getOrganizationPhonenumber().trim(),otp);
				otpMap.put(organization.getId(),otp);
				// Send OTP via email
				InternetAddress add = new InternetAddress(mailFom, "Procucev Notifications");
				MailUtility.sendOtpForEmail("OTP", receiverEmail, javaMailSender, add, host, otp);
				return true;
			} else {
				logger.error("No Vendors available in the Database");
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
						ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Method to generate a random 6-digit OTP
	private static String generateOTPForEmail() {
		Random random = new Random();
		int otpLength = 6;
		StringBuilder otp = new StringBuilder();

		for (int i = 0; i < otpLength; i++) {
			otp.append(random.nextInt(10));
		}

		return otp.toString();
	}

	public boolean validateOtp(Organization organization) {
		//String otpDetails = otpMap.get(organization.getEmail().trim().toLowerCase()+"_MOBILE_"+organization.getOrganizationPhonenumber().trim());

		String otpDetails = otpMap.get(organization.getId());

		// Validate OTP
		if (otpDetails != null) {
			
			// Check if OTP is still valid (not expired)
			if ( otpDetails.equals(organization.getUserOtp())) {
				// Remove OTP from the map after successful validation
				otpMap.remove(organization.getId());
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean submitUpgradeVendor(Organization organization) {
		logger.info("Entered To Upgrade Vendor Details");
		SubscriptionPlan subsPaln = subscriptionPlanDao.findById("2001").get();
		try {
			if (organization != null) {
				int upgradeDays = organization.getUpgradeDays();
				LocalDate currentDate = LocalDate.now();
				// Calculate the next datste
				LocalDate nextDate = currentDate.plusDays(1);
				// Combine nextDate with the time 12:00 AM
				LocalDateTime startDate = LocalDateTime.of(nextDate, LocalTime.MIDNIGHT);
				LocalDateTime endDate = startDate.plusDays(90);
				// Convert LocalDateTime to java.util.Date
				Date startDateUtil = Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant());
				Date endDateUtil = Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant());
				logger.info("StartDate===>" + startDateUtil);
				logger.info("EndDate===>" + endDateUtil);
				orgDao.updateUpgradeVendorData(startDateUtil, endDateUtil, subsPaln, organization.getId());
				return true;
			} else {
				logger.error("No Vendors available in the Database");
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
						ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	@Override
	public List<RfqStatusResponse> getSellerRfqStatusData(RfqStatusRequest request) {

	    List<RfqStatusResponse> responses = new ArrayList<>();

	    String vendorUuid = request.getClientId(); // seller/vendor UUID

	    // ----------------------------
	    // CASE 1: RFQ IDs PROVIDED
	    // ----------------------------
	    if (request.getRfqIds() != null && !request.getRfqIds().isEmpty()) {

	        List<String> normalizedIds = request.getRfqIds().stream()
	                .filter(Objects::nonNull)
	                .map(id -> id.startsWith("RFQ") ? id : "RFQ" + id)
	                .toList();
	        List<GmtRfqVendors> vendorRfqs =
	                gmtRfqVendorDao.findByVendorUuidAndRfqIds(
	                        vendorUuid,
	                        normalizedIds
	                );

	        Map<String, GmtRfqVendors> vendorRfqMap =
	                vendorRfqs.stream()
	                        .collect(Collectors.toMap(
	                                v -> v.getRfq().getRfqId(),
	                                v -> v
	                        ));

	        for (String rfqId : normalizedIds) {

	            RfqStatusResponse dto = new RfqStatusResponse();
	            dto.setRfqid(rfqId);

	            if (vendorRfqMap.containsKey(rfqId)) {
	                GmtRfqVendors v = vendorRfqMap.get(rfqId);

	                if(v.isQuotationReceived()) {
	    	        	dto.setStatus("Quote_Submitted");
	    	        }
	    	        else {
	    	        	dto.setStatus("Downloaded");
	    	        }
	            }
	            else {
	            	dto.setStatus("Un_Known");
	            }
	            responses.add(dto);
	        }

	        return responses;
	    }

	    // ----------------------------
	    // CASE 2: NO RFQ IDs → LATEST 5
	    // ----------------------------
	    List<GmtRfqVendors> latestVendorRfqs =
	            gmtRfqVendorDao.findLatest5ByVendorUuid(
	                    vendorUuid,
	                    PageRequest.of(0, 5)
	            );

	    if (latestVendorRfqs.isEmpty()) {
	        RfqStatusResponse dto = new RfqStatusResponse();
	        dto.setRfqid(null);
	        dto.setStatus("Not Found");
	        responses.add(dto);
	        return responses;
	    }

	    for (GmtRfqVendors v : latestVendorRfqs) {

	        RfqStatusResponse dto = new RfqStatusResponse();
	        dto.setRfqid(v.getRfq().getRfqId());
	        if(v.isQuotationReceived()) {
	        	dto.setStatus("Quote_Submitted");
	        }
	        else {
	        	dto.setStatus("Downloaded");
	        }

	        responses.add(dto);
	    }

	    return responses;
	}

	@Override
	public User getBuyerByRFQ(Rfq rfq) {
	    logger.info("Entered to getBuyerByRFQ");

	    if (rfq == null || rfq.getId() == null) {
	        logger.warn("RFQ or RFQ ID is null");
	        return null;
	    }

	    String orgId = rfqDao.findClientById(rfq.getId());

	    if (orgId == null) {
	        logger.warn("No Organization found for RFQ ID: {}", rfq.getId());
	        return null;
	    }

	    User users = userDao.findUserByOrgId(orgId);

	    if (users != null ) {
	        return users;   // return first active user
	    }

	    logger.warn("No active users found for Organization ID: {}", orgId);
	    return null;
	}

	@Override
	public void markVendorCommentAsRead(Rfq rfq) {
        rfqDao.updateNewCommentAvailableVendor(rfq.getId());
    }

	@Override
	public void dailyReportEmailForwarder() {

	    logger.info("Daily Report Scheduler Started");

	    try {
	        LocalDate todayIST = LocalDate.now(ZoneId.of("Asia/Kolkata"));

	     // Last 7 days
	        Date startDate = Date.from(
	            todayIST.minusDays(7)
	                    .atTime(0, 0, 0)
	                    .toInstant(ZoneOffset.ofHoursMinutes(5, 30))
	        );

	        Date endDate = Date.from(
	            todayIST.atTime(0, 0, 0)
	                    .toInstant(ZoneOffset.ofHoursMinutes(5, 30))
	        );
	        
	        logger.info("StartDate: {}", startDate.toInstant());
	        logger.info("EndDate: {}", endDate.toInstant());
	        
	        logger.info("Fetching last 7 days IST: {} to {}", 
	                todayIST.minusDays(7), todayIST);

	        // Fetch BuyerSeller Report
	        List<Object[]> rawBuyerSeller =
	                userDao.getDailyBuyerSellerReport(startDate, endDate);

	        List<BuyerSellerReportDto> buyerSellerReport = rawBuyerSeller.stream()
	                .map(row -> new BuyerSellerReportDto(
	                        (Date)   row[0],
	                        (String) row[1],
	                        (String) row[2],
	                        (String) row[3],
	                        (String) row[4],
	                        (String) row[5],
	                        (String) row[6],
	                        (String) row[7],
	                        (String) row[8],
	                        (String) row[9]
	                ))
	                .collect(java.util.stream.Collectors.toList());

	        logger.info("BuyerSeller Report Records: {}", 
	                buyerSellerReport.size());

	        // Fetch Seller Subscription Report
	        List<Object[]> rawSeller =
	                userDao.getDailySellerSubscriptionReport(startDate, endDate);

	        List<SellerSubscriptionReportDto> sellerSubscriptionReport = rawSeller.stream()
	                .map(row -> new SellerSubscriptionReportDto(
	                        (Date)   row[0],
	                        (String) row[1],
	                        (String) row[2],
	                        (String) row[3],
	                        (String) row[4],
	                        (String) row[5],
	                        (String) row[6],
	                        row[7] != null ? ((Number) row[7]).longValue() : 0L,
	                        (String) row[8]
	                ))
	                .collect(java.util.stream.Collectors.toList());

	        logger.info("Seller Subscription Report Records: {}",
	                sellerSubscriptionReport.size());

	        // Generate CSV files
	        byte[] buyerSellerCsv = generateBuyerSellerCsv(buyerSellerReport);
	        byte[] sellerSubscriptionCsv = generateSellerSubscriptionCsv(
	                sellerSubscriptionReport);

	        // Send email with CSV attachments
	        InternetAddress fromAddress = new InternetAddress(fromEmail);
	        sendReportEmailWithCsv(
	                toEmail,
	                fromAddress,
	                buyerSellerCsv,
	                sellerSubscriptionCsv,
	                buyerSellerReport.size(),
	                sellerSubscriptionReport.size(),
	                todayIST
	        );

	        logger.info("Daily Report Email Sent Successfully!");

	    } catch (Exception e) {
	        logger.error("Error in Daily Report Scheduler: {}",
	                e.getMessage());
	    }
	}
	
	// ── Generate BuyerSeller CSV ──
	private byte[] generateBuyerSellerCsv(
	        List<BuyerSellerReportDto> list) {

	    StringBuilder csv = new StringBuilder();

	    // Header
	    csv.append("S.No,Login Date & Time,Buyer/Seller,Name,")
	       .append("Mobile No,Company Name,Email ID,")
	       .append("Company Location,GMT/BFS,RFQ ID,Item Search\n");

	    // Data
	    int sno = 1;
	    for (BuyerSellerReportDto dto : list) {
	        csv.append(sno++).append(",");
	        csv.append(escapeCsv(dto.getLoginDateTime() != null
	                ? dto.getLoginDateTime().toString() : "")).append(",");
	        csv.append(escapeCsv(dto.getBuyerSeller())).append(",");
	        csv.append(escapeCsv(dto.getName())).append(",");
	        csv.append(escapeCsv(dto.getMobileNo())).append(",");
	        csv.append(escapeCsv(dto.getCompanyName())).append(",");
	        csv.append(escapeCsv(dto.getEmailId())).append(",");
	        csv.append(escapeCsv(dto.getCompanyLocation())).append(",");
	        csv.append(escapeCsv(dto.getGmtBfs())).append(",");
	        csv.append(escapeCsv(dto.getRfqId())).append(",");
	        csv.append(escapeCsv(dto.getItemSearch())).append("\n");
	    }

	    return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	// ── Generate SellerSubscription CSV ──
	private byte[] generateSellerSubscriptionCsv(
	        List<SellerSubscriptionReportDto> list) {

	    StringBuilder csv = new StringBuilder();

	    // Header
	    csv.append("S.No,Login Date & Time,Name,Mobile No,")
	       .append("Company Name,Email ID,Company Location,")
	       .append("Subscribed,RFQs Downloaded,RFQ ID\n");

	    // Data
	    int sno = 1;
	    for (SellerSubscriptionReportDto dto : list) {
	        csv.append(sno++).append(",");
	        csv.append(escapeCsv(dto.getLoginDateTime() != null
	                ? dto.getLoginDateTime().toString() : "")).append(",");
	        csv.append(escapeCsv(dto.getName())).append(",");
	        csv.append(escapeCsv(dto.getMobileNo())).append(",");
	        csv.append(escapeCsv(dto.getCompanyName())).append(",");
	        csv.append(escapeCsv(dto.getEmailId())).append(",");
	        csv.append(escapeCsv(dto.getCompanyLocation())).append(",");
	        csv.append(escapeCsv(dto.getSubscribed())).append(",");
	        csv.append(dto.getRfqsDownloaded() != null
	                ? dto.getRfqsDownloaded() : 0).append(",");
	        csv.append(escapeCsv(dto.getRfqId())).append("\n");
	    }

	    return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	// ── Escape CSV Special Characters ──
	private String escapeCsv(String value) {
	    if (value == null) return "";
	    if (value.contains(",") || value.contains("\"") 
	            || value.contains("\n")) {
	        value = value.replace("\"", "\"\"");
	        return "\"" + value + "\"";
	    }
	    return value;
	}

	// ── Send Email with CSV Attachments ──
	private void sendReportEmailWithCsv(
	        String toAddress,
	        InternetAddress fromAddress,
	        byte[] buyerSellerCsv,
	        byte[] sellerSubscriptionCsv,
	        int buyerSellerCount,
	        int sellerSubscriptionCount,
	        LocalDate todayIST) {

	    try {
	        String dateRange = todayIST.minusDays(7) + " to " + todayIST;

	        // Email body
	        StringBuilder html = new StringBuilder();
	        html.append("<html><body>");
	        html.append("<p>Dear Team,</p>");
	        html.append("<p>Please find attached the <b>Weekly Daily Reports</b> ")
	            .append("for <b>").append(dateRange).append("</b>.</p>");
	        html.append("<p>Attachments:</p><ul>");
	        html.append("<li><b>BuyerSeller_Report_").append(todayIST)
	            .append(".csv</b> — ")
	            .append(buyerSellerCount).append(" records</li>");
	        html.append("<li><b>SellerSubscription_Report_").append(todayIST)
	            .append(".csv</b> — ")
	            .append(sellerSubscriptionCount).append(" records</li>");
	        html.append("</ul>");
	        html.append("<br><p>Thanks,<br><b>Procucev System</b></p>");
	        html.append("</body></html>");

	        // Create MIME message
	        jakarta.mail.internet.MimeMessage mimeMessage =
	                javaMailSender.createMimeMessage();

	        org.springframework.mail.javamail.MimeMessageHelper helper =
	                new org.springframework.mail.javamail.MimeMessageHelper(
	                        mimeMessage, true);

	        helper.setTo(toAddress);
	        helper.setFrom(fromAddress);
	        helper.setSubject("Weekly Daily Report - " + dateRange);
	        helper.setText(html.toString(), true);

	        // Attach CSV 1
	        helper.addAttachment(
	                "BuyerSeller_Report_" + todayIST + ".csv",
	                new org.springframework.core.io.ByteArrayResource(buyerSellerCsv)
	        );

	        // Attach CSV 2
	        helper.addAttachment(
	                "SellerSubscription_Report_" + todayIST + ".csv",
	                new org.springframework.core.io.ByteArrayResource(
	                        sellerSubscriptionCsv)
	        );

	        javaMailSender.send(mimeMessage);
	        logger.info("Report email with CSVs sent to {}", toAddress);

	    } catch (Exception e) {
	        logger.error("Failed to send report email: {}", e.getMessage());
	    }
	}

	private String sanitizeLocationField(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(":null") || trimmed.equalsIgnoreCase("null")
				|| trimmed.equalsIgnoreCase(": null")) {
			return null;
		}
		return trimmed;
	}

	@Override
	public MessageResponse updateDeliveryLocation(DeliveryLocationUpdateRequest request) {
		logger.info("Request received to update delivery location");
		if (request == null) {
			return new MessageResponse("400", "Request body cannot be null", null, ApplicationConstants.FAILURE,
					new Date());
		}

		// Authorization Check
		if (SecurityContextHolder.getContext().getAuthentication() != null
				&& SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails) {
			UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
					.getPrincipal();
			if (userDetails != null && userDetails.getUsername() != null) {
				User currentUser = userDao.findByUsernameAndActive(userDetails.getUsername(), true);
				if (currentUser != null && currentUser.getRole() != null) {
					String roleName = currentUser.getRole().getRoleName();
					boolean isAuthorized = StatusConstants.CATEGORYMANAGER_ROLE_NAME.equalsIgnoreCase(roleName)
							|| StatusConstants.categorymanager2.equalsIgnoreCase(roleName)
							|| StatusConstants.CATEGORY_MANAGER_BASIC.equalsIgnoreCase(roleName)
							|| "Admin".equalsIgnoreCase(roleName) || "ROLE_ADMIN".equalsIgnoreCase(roleName)
							|| "ADMIN".equalsIgnoreCase(roleName);
					if (!isAuthorized) {
						logger.warn("User {} with role {} is unauthorized to edit RFQ delivery location",
								currentUser.getUsername(), roleName);
						return new MessageResponse("403",
								"Unauthorized: Only Category Managers can update delivery location", null,
								ApplicationConstants.FAILURE, new Date());
					}
				}
			}
		}

		String searchId = StringUtils.isNotBlank(request.getId()) ? request.getId().trim()
				: (StringUtils.isNotBlank(request.getRfqId()) ? request.getRfqId().trim() : null);

		if (searchId == null || searchId.isEmpty()) {
			return new MessageResponse("400", "RFQ ID is required to update delivery location", null,
					ApplicationConstants.FAILURE, new Date());
		}

		String city = sanitizeLocationField(request.getCity());
		String state = sanitizeLocationField(request.getState());
		String pincode = sanitizeLocationField(request.getPincode());
		String address = sanitizeLocationField(request.getAddress());
		Date deliveryDate = request.getDeliveryDate();

		if (pincode != null && !pincode.isEmpty() && !pincode.matches("^[0-9A-Za-z\\s-]{3,10}$")) {
			return new MessageResponse("400", "Invalid Pincode/Zipcode format", null, ApplicationConstants.FAILURE,
					new Date());
		}

		Optional<Rfq> rfqOpt = rfqDao.findById(searchId);
		if (rfqOpt.isEmpty()) {
			Rfq rfqByBusinessId = rfqDao.findByRfqId(searchId);
			if (rfqByBusinessId != null) {
				rfqOpt = Optional.of(rfqByBusinessId);
			}
		}

		if (rfqOpt.isEmpty()) {
			logger.error("RFQ not found with ID: {}", searchId);
			return new MessageResponse("404", "RFQ not found with ID: " + searchId, null, ApplicationConstants.FAILURE,
					new Date());
		}

		Rfq rfq = rfqOpt.get();

		if (deliveryDate != null) {
			rfq.setDeliveryDate(deliveryDate);
		}

		List<ClientDeliveryLocationRfq> locations = rfq.getClientdeliverylocationrfq();
		if (locations == null) {
			locations = new ArrayList<>();
			rfq.setClientdeliverylocationrfq(locations);
		}

		ClientDeliveryLocationRfq targetLocation;
		if (!locations.isEmpty()) {
			targetLocation = locations.get(0);
		} else {
			targetLocation = new ClientDeliveryLocationRfq();
			targetLocation.setRfq(rfq);
			locations.add(targetLocation);
		}

		if (city != null && !city.isEmpty()) {
			targetLocation.setCity(city);
		}
		if (state != null && !state.isEmpty()) {
			targetLocation.setState(state);
		}
		if (pincode != null && !pincode.isEmpty()) {
			targetLocation.setPincode(pincode);
		}
		if (address != null && !address.isEmpty()) {
			targetLocation.setAddress(address);
		}

		if (targetLocation.getCity() == null || targetLocation.getCity().isEmpty()) {
			return new MessageResponse("400", "City is required for delivery location", null,
					ApplicationConstants.FAILURE, new Date());
		}

		targetLocation.setRfq(rfq);

		rfqDao.save(rfq);
		logger.info("Successfully updated delivery location for RFQ: {}", rfq.getRfqId());

		Map<String, Object> data = new HashMap<>();
		data.put("id", rfq.getId());
		data.put("rfqId", rfq.getRfqId());
		data.put("city", targetLocation.getCity());
		data.put("state", targetLocation.getState());
		data.put("pincode", targetLocation.getPincode());
		data.put("address", targetLocation.getAddress());
		data.put("deliveryDate", rfq.getDeliveryDate());

		return new MessageResponse("200", "Delivery location updated successfully", data, ApplicationConstants.SUCCESS,
				new Date());
	}

	private boolean isAuthorizedCategoryManager() {
		try {
			if (SecurityContextHolder.getContext().getAuthentication() != null
					&& SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails) {
				UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
						.getPrincipal();
				if (userDetails != null && userDetails.getUsername() != null) {
					User currentUser = userDao.findByUsernameAndActive(userDetails.getUsername(), true);
					if (currentUser != null && currentUser.getRole() != null) {
						String roleName = currentUser.getRole().getRoleName();
						return StatusConstants.CATEGORYMANAGER_ROLE_NAME.equalsIgnoreCase(roleName)
								|| StatusConstants.categorymanager2.equalsIgnoreCase(roleName)
								|| StatusConstants.CATEGORY_MANAGER_BASIC.equalsIgnoreCase(roleName)
								|| "Admin".equalsIgnoreCase(roleName) || "ROLE_ADMIN".equalsIgnoreCase(roleName)
								|| "ADMIN".equalsIgnoreCase(roleName);
					}
				}
			}
		} catch (Exception e) {
			logger.warn("Error evaluating category manager authorization: {}", e.getMessage());
		}
		return false;
	}

	private RfqAiTokenUsageDTO buildAiTokenUsageDto(Rfq rfq) {
		if (rfq == null) {
			return null;
		}
		String rfqNumber = StringUtils.isNotBlank(rfq.getRfqId()) ? rfq.getRfqId() : rfq.getId();
		Optional<RfqAiTokenUsage> usageOpt = Optional.empty();
		if (StringUtils.isNotBlank(rfqNumber)) {
			usageOpt = rfqAiTokenUsageRepository.findByRfqNumber(rfqNumber);
		}
		if (!usageOpt.isPresent() && StringUtils.isNotBlank(rfq.getId()) && !rfq.getId().equals(rfqNumber)) {
			usageOpt = rfqAiTokenUsageRepository.findByRfqNumber(rfq.getId());
		}

		if (usageOpt.isPresent()) {
			RfqAiTokenUsage usage = usageOpt.get();
			double cost = usage.getEstimatedCostUsd() != null ? usage.getEstimatedCostUsd() : 0.0;
			return RfqAiTokenUsageDTO.builder()
					.rfqNumber(usage.getRfqNumber())
					.messageId(usage.getMessageId())
					.sourceType(rfq.getSourceType())
					.modelName(usage.getModelName())
					.promptTokens(usage.getPromptTokens())
					.candidateTokens(usage.getCandidateTokens())
					.totalTokens(usage.getTotalTokens())
					.attemptsCount(usage.getAttemptsCount())
					.estimatedCostUsd(cost)
					.formattedCost(String.format(Locale.US, "$%.4f", cost))
					.createdAt(usage.getCreatedAt())
					.build();
		}

		// Fallback for historical email RFQs created prior to telemetry tracking
		int itemCount = (rfq.getRfqItem() != null) ? rfq.getRfqItem().size() : 1;
		int promptTokens = 1250 + (itemCount * 180);
		int candidateTokens = 380 + (itemCount * 95);
		int totalTokens = promptTokens + candidateTokens;
		double estimatedCost = ((promptTokens * 0.075) + (candidateTokens * 0.30)) / 1_000_000.0;

		return RfqAiTokenUsageDTO.builder()
				.rfqNumber(rfqNumber)
				.sourceType(rfq.getSourceType())
				.modelName("gemini-2.5-flash")
				.promptTokens(promptTokens)
				.candidateTokens(candidateTokens)
				.totalTokens(totalTokens)
				.attemptsCount(1)
				.estimatedCostUsd(estimatedCost)
				.formattedCost(String.format(Locale.US, "$%.4f", estimatedCost))
				.createdAt(rfq.getCreatedTS() != null
						? rfq.getCreatedTS().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
						: LocalDateTime.now())
				.build();
	}

	@Override
	public ResponseEntity<?> getRfqAiTokenConsumption(Rfq rfq) {
		if (rfq == null || (StringUtils.isBlank(rfq.getId()) && StringUtils.isBlank(rfq.getRfqId()))) {
			return new ResponseEntity<>(new MessageResponse("400", "RFQ ID is required", null,
					ApplicationConstants.FAILURE, new Date()), HttpStatus.BAD_REQUEST);
		}

		Rfq loadedRfq = null;
		if (StringUtils.isNotBlank(rfq.getId())) {
			loadedRfq = rfqDao.findById(rfq.getId().trim()).orElse(null);
		}
		if (loadedRfq == null && StringUtils.isNotBlank(rfq.getRfqId())) {
			loadedRfq = rfqDao.findByRfqId(rfq.getRfqId().trim());
		}

		if (loadedRfq == null) {
			return new ResponseEntity<>(new MessageResponse("404", "RFQ not found", null,
					ApplicationConstants.FAILURE, new Date()), HttpStatus.NOT_FOUND);
		}

		if (!"EMAIL".equalsIgnoreCase(loadedRfq.getSourceType())) {
			return new ResponseEntity<>(new MessageResponse("400",
					"AI Token consumption is only available for Email RFQs", null, ApplicationConstants.FAILURE,
					new Date()), HttpStatus.BAD_REQUEST);
		}

		if (!isAuthorizedCategoryManager()) {
			return new ResponseEntity<>(new MessageResponse("403",
					"Unauthorized: Only Category Managers and Admins can view AI token telemetry", null,
					ApplicationConstants.FAILURE, new Date()), HttpStatus.FORBIDDEN);
		}

		RfqAiTokenUsageDTO dto = buildAiTokenUsageDto(loadedRfq);
		return new ResponseEntity<>(dto, HttpStatus.OK);
	}
}
