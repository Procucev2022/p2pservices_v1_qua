package com.portal.procucev.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.mail.internet.InternetAddress;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.BFSDao;
import com.portal.procucev.dao.BFSDocumentsDao;
import com.portal.procucev.dao.BFSImagesDao;
import com.portal.procucev.dao.BFSUserCommentsDao;
import com.portal.procucev.dao.BFSUserDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.SearchRepository;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.Dto.BFSItemDto;
import com.portal.procucev.Dto.BFSItemMainDetailsDTO;
import com.portal.procucev.Dto.BfsDTO;
import com.portal.procucev.Dto.VendorInfoBean;
import com.portal.procucev.model.BFSDocuments;
import com.portal.procucev.model.BFSImages;
import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.BFSUserComments;
import com.portal.procucev.model.BFSUsers;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.MailUtility;
import com.portal.procucev.utils.StatusConstants;

@Service
public class BFSServiceImpl implements BFSService {

	@Autowired
	private OrgTypeDao orgTypeDao;

	@Autowired
	private OrgDao orgDao;

	@Autowired
	private MasterStatusDao masterStatusDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private BFSDao bfsDao;

	@Autowired
	private BFSUserDao bfsUserDao;

	@Autowired
	private BFSImagesDao bfsImagesDao;

	@Value("${host}")
	String host;

	@Autowired
	JavaMailSender javaMailSender;

	@Value("${spring.mail.username}")
	String mailFom;

	@Value("${toAddress}")
	String toAddress;

	@Autowired
	private BFSDocumentsDao bFSDocumentsDao;

	@Autowired
	private SearchRepository searchRepository;

	@Autowired
	private BFSUserCommentsDao bfsUserCommentsDao;

	private static final Logger log = LoggerFactory.getLogger(BFSServiceImpl.class);

	@Override
	public List<Organization> orgSearch(Organization org) {
		log.info("Entered To orgSearch()");

		// Fetch the OrgType objects for CLIENT and VENDOR
		OrgType client = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);
		OrgType vendor = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);

		// Fetch organizations with matching criteria
		List<Organization> organizations = orgDao.findOrganizationsByTypeAndNameIgnoreCase(client, vendor,
				org.getCompanyName());

		if (!organizations.isEmpty()) {
			log.info("Completed and returning resonse{}");
			return organizations;
		} else {
			log.error("No Organization found");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_ORG_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}

	@Override
	public List<User> getUsersByOrg(Organization org) {
		// TODO Auto-generated method stub
		log.info("Enterd into getUsersByOrg()");
		List<User> usersList = userDao.findByOrganization(org.getId());
		if (usersList != null) {
			log.info("Completed and Returning Response");
			return usersList;
		} else {
			log.error("No Users found");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	@Override
	public boolean createBfs(List<BFSItems> items) {
		log.info("Entered to Create BFS");

		if (items == null || items.isEmpty()) {
			log.warn("Items list is null or empty. Exiting method.");
			return false;
		}

		try {
			MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BFS_NEW);
			userDao.updateBfsGroupForUsers(items.get(0).getBfsGroup(), items.get(0).getUserId());
			for (BFSItems item : items) {
				item.setStatus(status);
				List<BFSImages> bfsImages = item.getBfsImages();
				if (!CollectionUtils.isEmpty(bfsImages)) {
					item.setImagesFlag(true);
				}
			}
			bfsDao.saveAll(items);
			log.info("BFS items saved successfully.");
			return true;
		} catch (Exception e) {
			log.error("Error occurred while creating BFS: {}", e.getMessage(), e);
			return false;
		}
	}

	public List<BFSItems> getItemsByOrgAndUser(User user) {
		if (user == null) {
			log.warn("User is null. Exiting method.");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		List<BFSItems> bfsList = bfsDao.findByOrgAndUser(user.getOrg().getId(), user.getId());

		if (bfsList == null || bfsList.isEmpty()) {
			log.warn("No BFS items found for the given organization and  User (Org ID: {}, User ID: {}).",
					user.getOrg().getId(), user.getId());
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		for (BFSItems item : bfsList) {
			long bfsCount = bfsImagesDao.findCountByBfs(item);
			if (bfsCount >= 1) {
				item.setImagesFlag(true);
			}
		}

		return bfsList;
	}

	@Override
	public List<BFSItems> getAllItems(User user) {
		if (user == null) {
			log.warn("User is null. Exiting method.");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		List<BFSItems> bfsList = bfsDao.findAllItems(user.getId());

		if (bfsList == null || bfsList.isEmpty()) {
			log.warn("No BFS items found for the given user with ID: {}", user.getId());
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		for (BFSItems item : bfsList) {
//			List<MasterStatus> statusOpt = bfsUserDao.findByUserAndItem(user.getId(), item.getId());
//			if (!CollectionUtils.isEmpty(statusOpt)) {
//				item.setStatus(statusOpt.get(0));
//			}
			long bfsCount = bfsImagesDao.findCountByBfs(item);
			if (bfsCount >= 1) {
				item.setImagesFlag(true);
			}
		}
		return bfsList;
	}

	@Override
	public List<BFSItems> createBfsByBoq(BFSItems items) {
		log.info("Entered to Create BFS By Boq");

		if (items == null) {
			log.warn("Items list is null or empty. Exiting method.");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		if (items.getBoqfile() != null && items.getBoqfile().length > 0) {
			List<BFSItems> bfsItems = processBOQFiles(items.getBoqfile());
			log.info("Fetching BFS Items after processing BOQ File");
			if (!CollectionUtils.isEmpty(bfsItems)) {
				return bfsItems;
			} else {
				log.warn("BFSItems list is null or empty. Exiting method.");
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
						ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
			}
		}
		return null;
	}

	private List<BFSItems> processBOQFiles(byte[] boqfile) {

		List<BFSItems> bfsItem = new ArrayList<BFSItems>();

		ByteArrayInputStream bin = new ByteArrayInputStream(boqfile);
		XSSFWorkbook myExcelBook = null;
		try {
			myExcelBook = new XSSFWorkbook(bin);
		} catch (IOException e) {
			log.error("Error while parsing BOQ file " + e.getMessage());
		}
		XSSFSheet myExcelSheet = myExcelBook.getSheetAt(0);

		// HSSFRow row = myExcelSheet.getRow(7);
		Iterator<Row> rowIteratorforRows = myExcelSheet.iterator();
		Iterator<Row> rowIteratorcheckcount = myExcelSheet.iterator();
		Iterable<Row> newIterable = () -> rowIteratorcheckcount;
		long count = StreamSupport.stream(newIterable.spliterator(), false).count();

		// It will throw Exception when File has Empty Rows

		if (count == 0 || count == 1) {

			log.info("File Has Empty Rows and Closing the workbook");
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
					log.info("Row Format " + headers.toString());

					// This method will validate the format expected to process
					validateExcelTemplate(headers);
					continue;
				} else {

					BFSItems bfsItems = new BFSItems();
					DataFormatter formatter = new DataFormatter();
					if (row.getCell(1) != null) {
						String cellValue = row.getCell(1).getStringCellValue().replaceAll("^\\s+", "");
						if (StringUtils.isNotEmpty(cellValue)) {
							log.info("Entered To set Description{}" + cellValue);
							bfsItems.setDescription(cellValue);
						}
					}

					if (row.getCell(2) != null) {
						String cellValue = row.getCell(2).getStringCellValue().replaceAll("^\\s+", "");
						if (StringUtils.isNotEmpty(cellValue)) {
							log.info("Entered To set Specification{}" + cellValue);
							bfsItems.setSpecification(cellValue);
						}
					}
					if (row.getCell(3) != null
							&& StringUtils.isNotEmpty(row.getCell(3).getStringCellValue().replaceAll("^\\s+", ""))) {
						if (row.getCell(3).getStringCellValue().replaceAll("^\\s+", "") != null)
							log.info("Entered To set UOM{}"
									+ row.getCell(3).getStringCellValue().replaceAll("^\\s+", ""));
						bfsItems.setUnitofMeasures(row.getCell(3).getStringCellValue().replaceAll("^\\s+", ""));

					}
					if (row.getCell(4) != null
							&& StringUtils.isNotEmpty(row.getCell(4).getStringCellValue().replaceAll("^\\s+", ""))) {
						if (row.getCell(4).getStringCellValue().replaceAll("^\\s+", "") != null)
							log.info("Entered To set Category{}"
									+ row.getCell(4).getStringCellValue().replaceAll("^\\s+", ""));
						bfsItems.setCategory(row.getCell(4).getStringCellValue().replaceAll("^\\s+", ""));
					}
					// To allow both Numeric and String values

					if (row.getCell(5) != null) {
						log.info("Entered To set Quantity{}" + row.getCell(5).getNumericCellValue());
						bfsItems.setTotalQuantity(row.getCell(5).getNumericCellValue());
					}
					if (row.getCell(6) != null
							&& StringUtils.isNotEmpty(row.getCell(6).getStringCellValue().replaceAll("^\\s+", ""))) {
						if (row.getCell(6).getStringCellValue().replaceAll("^\\s+", "") != null)
							log.info("Entered To set Location{}"
									+ row.getCell(6).getStringCellValue().replaceAll("^\\s+", ""));
						bfsItems.setLocation(row.getCell(6).getStringCellValue().replaceAll("^\\s+", ""));
					}
					if (row.getCell(7) != null) {
						// To allow both Numeric and String values
						String cellValue = formatter.formatCellValue(row.getCell(7));
						log.info("Entered To set AgeOfAsset{}" + cellValue);
						bfsItems.setAgeOfAsset(cellValue);
					}
					if (row.getCell(8) != null) {
						String cellValue = formatter.formatCellValue(row.getCell(8));
						cellValue = cellValue.replace(",", ""); // Remove commas
						log.info("Entered To set SellPrice{}" + cellValue);
						bfsItems.setSellPrice(Double.valueOf(cellValue));
					}
					if (row.getCell(9) != null) {
						log.info("Entered To set Discount{}" + row.getCell(9).getNumericCellValue());
						bfsItems.setDiscount((double) row.getCell(9).getNumericCellValue());
					}
					if (row.getCell(10) != null
							&& StringUtils.isNotEmpty(row.getCell(10).getStringCellValue().replaceAll("^\\s+", ""))) {
						if (row.getCell(10).getStringCellValue().replaceAll("^\\s+", "") != null)
							log.info("Entered To Set Remarks{}"
									+ row.getCell(10).getStringCellValue().replaceAll("^\\s+", ""));
						bfsItems.setRemarks(row.getCell(10).getStringCellValue().replaceAll("^\\s+", ""));
					}
					if (row.getCell(11) != null
							&& StringUtils.isNotEmpty(row.getCell(11).getStringCellValue().replaceAll("^\\s+", ""))) {
						if (row.getCell(11).getStringCellValue().replaceAll("^\\s+", "") != null)
							log.info("Entered To Set BFSGroup{}"
									+ row.getCell(11).getStringCellValue().replaceAll("^\\s+", ""));
						bfsItems.setBfsGroup(row.getCell(11).getStringCellValue().replaceAll("^\\s+", ""));
					}

					bfsItem.add(bfsItems);

				}

			}

		}
		return bfsItem;

	}

	private boolean validateExcelTemplate(List<String> headers) throws AppException {

		log.info("Validation Initiated for Processing Excel File.");
		boolean valid = false;
		log.info("Size of Headers " + headers.size());

		List<String> unknownHeader = new ArrayList<String>();

		List<String> constantsformat = Arrays.asList(ApplicationConstants.REQUIRED_FORMAT_BFS_ITEM_FORMAT.split(", "));

		for (String string : constantsformat) {
			if (!headers.contains(string)) {
				unknownHeader.add(string);
			}
		}

		if (unknownHeader.size() > 0) {
			log.info("Unknown Headers Found for Uploaded File.");
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					String.format(ApplicationConstants.FILE_FORMAT_NOT_MATCHED_DATA,
							unknownHeader.stream().map(String::valueOf).collect(Collectors.joining(",")),
							ApplicationConstants.REQUIRED_FORMAT_RFQITEMFORMAT),
					ApplicationConstants.SERVICE_LEVEL_EXCEPTION, ApplicationConstants.FAILURE);

		}
		//
		if (headers.size() >= 12) {

			if (headers.size() == 12) {

				if ((ApplicationConstants.SerialNO.equalsIgnoreCase(headers.get(0)))
						&& (ApplicationConstants.ItemDescription.equalsIgnoreCase(headers.get(1)))
						&& (ApplicationConstants.Spec.equalsIgnoreCase(headers.get(2)))
						&& (ApplicationConstants.Uom.equalsIgnoreCase(headers.get(3)))
						&& (ApplicationConstants.Category.equalsIgnoreCase(headers.get(4)))
						&& (ApplicationConstants.Quantity.equalsIgnoreCase(headers.get(5)))
						&& (ApplicationConstants.Location.equalsIgnoreCase(headers.get(6)))
						&& (ApplicationConstants.AgeOfAsset.equalsIgnoreCase(headers.get(7)))
						&& (ApplicationConstants.BuyPrice.equalsIgnoreCase(headers.get(8)))
						&& (ApplicationConstants.Discount.equalsIgnoreCase(headers.get(9)))
						&& (ApplicationConstants.Remarks.equalsIgnoreCase(headers.get(10)))
						&& (ApplicationConstants.BFSGroup.equalsIgnoreCase(headers.get(11)))) {
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
	public boolean requestBfsItem(BFSUsers bfsUser) {
		log.info("Entered To Request Bfs Item");

		if (bfsUser == null) {
			log.warn("BFSUser is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSUser is null.");
		}

		// Check if the item has already been requested by the user
//		List<String> bfsUserRequested = bfsUserDao.findByUserIdAndBfs(bfsUser.getUser().getId(),
//				bfsUser.getItems().getId());
//		if (!bfsUserRequested.isEmpty()) {
//			log.warn("Item with ID {} has already been requested by user with ID {}. Exiting method.",
//					bfsUser.getItems().getId(), bfsUser.getUser());
//			throw new AppException(HttpStatus.CONFLICT.value(), "Item has already been requested.");
//		}
		// Set status and save bfsUser
		MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_APPROVED);
		bfsUser.setStatus(status);
		bfsDao.updateLatestBidDate(bfsUser.getItems());
		BFSUsers savedUser = bfsUserDao.save(bfsUser);
		BFSItems itemDetails = bfsDao.findSellerById(bfsUser.getItems().getId());
		List<User> sellerDetails = userDao.findByOrganization(itemDetails.getOrgId());
		InternetAddress add;
		User user = userDao.findOrgByID(savedUser.getUser().getId());
		try {
			add = new InternetAddress(mailFom, "Procucev Notifications");
			MailUtility.emailForBidRequest("Request For Bid", toAddress, javaMailSender, add, host, savedUser, user,
					itemDetails.getDescription());
			MailUtility.emailForBuyerBidRequest("Bid Request For Bid", user.getUsername(), javaMailSender, add, host,
					savedUser, user, itemDetails.getDescription());
			if (!CollectionUtils.isEmpty(sellerDetails)) {
				String sellerEmail = sellerDetails.get(0).getUsername();
				MailUtility.emailForsellerBidRequest("Bid Request For Bid", sellerEmail, javaMailSender, add, host,
						savedUser, user, itemDetails.getDescription());
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("BFSItem with ID {} requested successfully for user with ID {}.", bfsUser.getItems().getId(),
				bfsUser.getUser().getId());
		return true;
	}

	@Override
	public List<BFSUsers> getRequestedUserByBFS(BFSItems item) {
		// TODO Auto-generated method stub
		log.info("Entered To getRequestedUserByBFS");

		if (item == null) {
			log.warn("BFSItem is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSItem is null.");
		}

		List<BFSUsers> usersList = bfsUserDao.findByItems(item);
		if (usersList == null || usersList.isEmpty()) {
			log.warn("No BFS Users found for the given user with ID: {}", item.getId());
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		return usersList;
	}

	@Override
	public List<BfsDTO> getRequestedItems() {
		// TODO Auto-generated method stub
		List<BfsDTO> bfsList = new ArrayList<>();
		List<String> inputStatus = new ArrayList<String>();
		inputStatus.add(StatusConstants.BID_REQUESTED);
		inputStatus.add(StatusConstants.BID_APPROVED);
		inputStatus.add(StatusConstants.BID_APPROVAL_REJECTED);
		inputStatus.add(StatusConstants.BID_ACCEPTED);
		inputStatus.add(StatusConstants.BID_NOT_ACCEPTED);

		List<MasterStatus> resultStatus = masterStatusDao.findByStatusIn(inputStatus);

		List<BFSUsers> usersList = bfsUserDao.findByStatusIn(resultStatus, Sort.by(Sort.Direction.DESC, "createdTS"));
		if (!CollectionUtils.isEmpty(usersList)) {
			for (BFSUsers user : usersList) {
				BfsDTO bfsDto = bfsDetails(user);
				bfsList.add(bfsDto);

			}
			return bfsList;
		} else {
			log.warn("No BFS Users found with the requested status {}", StatusConstants.BID_REQUESTED);
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	private BfsDTO bfsDetails(BFSUsers user) {
		BfsDTO bfsDto = new BfsDTO();
		bfsDto.setId(user.getId());
		bfsDto.setCreatedTS(user.getCreatedTS());
		bfsDto.setBuyerCompanyName(user.getOrg().getCompanyName());
		bfsDto.setBuyerName(user.getUser().getFullName());
		bfsDto.setBuyerEmail(user.getUser().getUsername());
		bfsDto.setBuyerId(user.getUser().getId());
		bfsDto.setDescription(user.getItems().getDescription());
		bfsDto.setSpecification(user.getItems().getSpecification());
		bfsDto.setBuyPrice(user.getBuyPrice());
		bfsDto.setBuyerDiscount(user.getDiscount());
		bfsDto.setBuyerFinalPrice(user.getBuyPrice());
		bfsDto.setSellPrice(user.getItems().getSellPrice());
		bfsDto.setSellFinalPrice(user.getItems().getAskPrice());
		bfsDto.setDiscount(user.getItems().getDiscount());
		bfsDto.setUnitofMeasures(user.getItems().getUnitofMeasures());
		bfsDto.setAskPrice(user.getAskPrice());
		User userData = userDao.findById(user.getItems().getUserId()).get();
		if (userData != null) {
			bfsDto.setSellerCompanyName(userData.getOrg().getCompanyName());
			bfsDto.setSellerEmail(userData.getUsername());
			bfsDto.setSellerName(userData.getFullName());
		}

		bfsDto.setSellerId(user.getItems().getUserId());
		bfsDto.setQuantity(user.getQuantity());
		bfsDto.setAvailableQuantity(user.getItems().getAvailableQuantity());
		bfsDto.setTotalQuantity(user.getItems().getTotalQuantity());
		bfsDto.setBuyerRemarks(user.getBuyerRemarks());
		bfsDto.setStatus(user.getStatus());
		bfsDto.setDisclosedBuypriceValue(user.getItems().getDisclosedBuypriceValue());
		bfsDto.setBuyPriceDisclosure(user.getItems().isBuyPriceDisclosure());
		return bfsDto;
	}

	@Override
	public boolean approveBfsItem(BFSUsers bfsUser) {
		// TODO Auto-generated method stub

		if (bfsUser == null) {
			log.warn("BFSUser is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSUser is null.");
		} else {
			String bfsItemId = bfsUserDao.findItemByBfsUser(bfsUser.getId());
			if (bfsUser.isApproval()) {
				MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_APPROVED);
				bfsUserDao.updateStatus(bfsUser, status);
				bfsDao.updateStatus(bfsItemId, status);
				return true;
			} else {
				MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_APPROVAL_REJECTED);
				bfsUserDao.updateCmRemarksAndStatus(bfsUser, status, bfsUser.getCmRemarks());
				bfsDao.updateStatus(bfsItemId, status);
				return true;
			}
		}

	}

	@Override
	public List<BfsDTO> getApprovedItems(User user) {
		// TODO Auto-generated method stub
		List<BfsDTO> bfsList = new ArrayList<>();
		MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_APPROVED);
		List<BFSUsers> usersList = bfsUserDao.findByStatusAndUser(status, user.getId());
		if (!CollectionUtils.isEmpty(usersList)) {
			for (BFSUsers bfsUser : usersList) {
				BfsDTO bfsDto = bfsDetails(bfsUser);
				bfsList.add(bfsDto);

			}
			return bfsList;
		} else {
			log.warn("No BFS Users found with the requested status {}", StatusConstants.BID_REQUESTED);
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	@Override
	public List<BfsDTO> getBidsByBuyer(BFSUsers user) {
		// TODO Auto-generated method stub
		log.info("Entered To Get Bids Buyer And Item");
		List<BfsDTO> bfsList = new ArrayList<>();
		List<BFSUsers> usersList = bfsUserDao.findByUserAndItems(user.getUser().getId(), user.getItems().getId());
		if (!CollectionUtils.isEmpty(usersList)) {
			for (BFSUsers bfsUser : usersList) {
				BfsDTO bfsDto = bfsDetails(bfsUser);
				bfsList.add(bfsDto);
			}
			return bfsList;
		} else {
			log.warn("No BFS Users found with the requested status {}", StatusConstants.BID_REQUESTED);
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	@Override
	public boolean acceptBfsItemBySeller(BFSUsers bfsUser) {
		// TODO Auto-generated method stub
		if (bfsUser == null) {
			log.warn("BFSUser is null. Exiting method");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSUser is null");
		} else {
			MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_ACCEPTED);
			bfsUserDao.updateStatus(bfsUser, status);
			// Adding Logic to minus buyer quantiy from item quantity
			Optional<BFSUsers> findById = bfsUserDao.findById(bfsUser.getId());
			if (findById.isPresent()) {
				BFSUsers bfsUsers = findById.get();
				int quantity = bfsUsers.getQuantity();

				double totalQuantity = bfsUsers.getItems().getAvailableQuantity();
				double availableQuantity = totalQuantity - quantity;
				log.info("Buyer Quantity" + quantity + " Item Total Quantity" + totalQuantity);
				bfsDao.updateAvailableQuantity(availableQuantity, bfsUsers.getItems().getId());
				String uniqueId = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
				bfsUserDao.updateUniqueId(uniqueId, bfsUser.getId());
				String userName = userDao.findByUserID(bfsUsers.getUser().getId());
				String clientName = userDao.findByUserID(bfsUsers.getItems().getUserId());
				String cmUser = "srinivas.mukku@procucev.com";
				try {
					InternetAddress add = new InternetAddress(mailFom, "Procucev Notifications");
					// To send to Buyer
					MailUtility.emailBFSAccepted(
							"Item is Accepted from Buy From Stock(BFS) ! Next Steps for Transaction...", userName,
							javaMailSender, add, bfsUsers, host, uniqueId);
					log.info("Mail Sent To Buyer");
					// To send To CM
					MailUtility.emailBFSAccepted(
							"Item is Accepted from Buy From Stock(BFS) ! Next Steps for Transaction...", cmUser,
							javaMailSender, add, bfsUsers, host, uniqueId);
					log.info("Mail Sent To Category Manager");
					// To Send Email To Seller
					MailUtility.emailBFSAccepted(
							"Item is Accepted from Buy From Stock(BFS) ! Next Steps for Transaction...", clientName,
							javaMailSender, add, bfsUsers, host, uniqueId);
				} catch (UnsupportedEncodingException e) {
					log.error("Error sending email for PPO approval", e);
				}

			}
			return true;
		}
	}

	@Override
	public boolean rejectBfsItemBySeller(BFSUsers bfsUser) {
		// TODO Auto-generated method stub
		if (bfsUser == null) {
			log.warn("BFSUser is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSUser is null.");
		} else {
			MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_NOT_ACCEPTED);
			bfsUserDao.updateSellerRejectStatus(bfsUser, status, bfsUser.getSellerRemarks());
			return true;
		}
	}

	@Override
	public List<BFSDocuments> getDocumentsByBfs(BFSItems item) {
		// TODO Auto-generated method stub
		if (item == null) {
			log.warn("BFSItem is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSItem is null.");
		}
		log.info("To Get BFS Documents By BFSItems.");
		List<BFSDocuments> documentsList = bFSDocumentsDao.findByBfs(item);
		if (CollectionUtils.isEmpty(documentsList)) {
			log.warn("BFS Documents is null for BFSItem.");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		return documentsList;

	}

	@Override
	public List<BFSUsers> getRequestedUserByBFSAndStatus(BFSItems item) {
		// TODO Auto-generated method stub
		log.info("Entered To getRequestedUserByBFS");

		if (item == null) {
			log.warn("BFSItem is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSItem is null.");
		}
		List<String> inputStatus = new ArrayList<String>();
		inputStatus.add(StatusConstants.BID_APPROVED);
		inputStatus.add(StatusConstants.BID_ACCEPTED);
		inputStatus.add(StatusConstants.BID_NOT_ACCEPTED);
		List<MasterStatus> resultStatus = masterStatusDao.findByStatusIn(inputStatus);
		List<BFSUsers> usersList = bfsUserDao.findByItemsAndStatus(item, resultStatus);
		if (usersList == null || usersList.isEmpty()) {
			log.warn("No BFS Users found for the given user with ID: {}", item.getId());
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		return usersList;
	}

	@Override
	public BFSItems getBfsById(BFSItems item) {
		// TODO Auto-generated method stub
		if (item == null) {
			log.warn("BFSItem is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSItem is null.");
		}
		log.info("To Get BFS  By Id.");
		Optional<BFSItems> bfs = bfsDao.findById(item.getId());
		if (bfs.isPresent()) {
			log.info("Returning BFS::");
			BFSItems bfsItems = bfs.get();
			List<BFSDocuments> docs = bFSDocumentsDao.findByBfs(item);
			List<BFSImages> images = bfsImagesDao.findByBfs(item.getId());
			if (!CollectionUtils.isEmpty(docs)) {
				bfsItems.setBfsDocuments(docs);
			}
			if (!CollectionUtils.isEmpty(images)) {
				bfsItems.setBfsImages(images);
			}
			return bfsItems;
		} else {
			log.warn("BFS is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "No BFS Found With Id.");
		}

	}

	@Override
	public boolean editBfs(BFSItems items) {
		// Check if the input items are null or empty
		if (items == null) {
			log.warn("Items list is null or empty. Exiting method.");
			return false;
		}

		try {
			// Fetch the status for BFS_NEW
			MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BFS_NEW);

			// Update BFS group for users
			userDao.updateBfsGroupForUsers(items.getBfsGroup(), items.getUserId());

			// Get existing images from the database for the BFS item
			List<BFSImages> existingImages = bfsImagesDao.findByBfs(items.getId());
			List<BFSDocuments> existingDocs = bFSDocumentsDao.findByBfs(items);

			// Get the images from the input BFSItems object
			List<BFSImages> inputImages = items.getBfsImages();
			List<BFSDocuments> inputDocuments = items.getBfsDocuments();

			// Update or delete existing images based on input

			if (!CollectionUtils.isEmpty(existingImages)) {
				// Delete the image if it is not present in the input images
				bfsImagesDao.deleteAll(existingImages);
			}
			if (!CollectionUtils.isEmpty(existingDocs)) {
				// Delete the image if it is not present in the input images
				bFSDocumentsDao.deleteAll(existingDocs);
			}
			log.info("Updating BFS Images");
			items.setBfsImages(inputImages);
			log.info("Updating BFS Documents");
			items.setBfsDocuments(inputDocuments);
			// Update the status of the BFS item
			items.setStatus(status);
			List<BFSImages> bfsImages = items.getBfsImages();
			if (!CollectionUtils.isEmpty(bfsImages)) {
				items.setImagesFlag(true);
			}
			// Save the updated BFS item
			bfsDao.save(items);

			log.info("BFS items edited successfully.");
			return true;
		} catch (Exception e) {
			log.error("Error occurred while editing BFS: {}", e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean editBfsUser(BFSUsers bfsUser) {
		// TODO Auto-generated method stub
		if (bfsUser == null) {
			log.warn("Items list is null or empty. Exiting method.");
			return false;
		}

		try {
//			bfsUserDao.updatePrices(bfsUser.getId(), bfsUser.getBuyPrice(), bfsUser.getQuantity(),
//					bfsUser.getAskPrice(), bfsUser.getDiscount());
			Optional<BFSUsers> bfsData = bfsUserDao.findById(bfsUser.getId());
			if (bfsData.isPresent()) {
				BFSUsers bfsUsers = bfsData.get();
				BFSUsers bfs = new BFSUsers();
				bfs.setItems(bfsUsers.getItems());
				bfs.setUser(bfsUsers.getUser());
				MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_REQUESTED);
				bfs.setStatus(status);
				bfs.setOrg(bfsUsers.getOrg());
				bfs.setBuyPrice(bfsUser.getBuyPrice());
				bfs.setQuantity(bfsUser.getQuantity());
				bfs.setAskPrice(bfsUser.getAskPrice());
				bfs.setDiscount(bfsUser.getDiscount());
				bfsUserDao.save(bfs);
			}

			log.info("BFS User Edited successfully.");
			return true;
		} catch (Exception e) {
			log.error("Error occurred while creating BFS: {}", e.getMessage(), e);
			return false;
		}
	}

	@Override
	public List<BFSItems> getRequestedItemByBuyer(User user) {
		log.info("Entered to get requested items by user {}", user.getId());

		// Fetch item IDs associated with the user
		List<String> itemIds = bfsUserDao.findItemByUser(user.getId());
		if (CollectionUtils.isEmpty(itemIds)) {
			log.info("No item IDs found for user {}", user.getId());
			return new ArrayList<>();
		}

		// Fetch BFS items by the retrieved item IDs
		List<BFSItems> bfsItems = bfsDao.findByIdIn(itemIds);
		if (CollectionUtils.isEmpty(bfsItems)) {
			log.info("No BFS items found for item IDs {}", itemIds);
			return new ArrayList<>();
		}

		// Optional: Check item status and set requested flag if needed
		// TODO: Uncomment and implement status check if needed in the future
		/*
		 * MasterStatus status =
		 * masterStatusDao.findByStatus(StatusConstants.BID_REQUESTED); long value =
		 * bfsUserDao.findItemStatusByUser(user.getId(), status);
		 * log.info("Item status count for user {}: {}", user.getId(), value);
		 * 
		 * if (value > 0) { for (BFSItems item : bfsItems) {
		 * item.setRequestedFlag(true); } }
		 */

		// Set image flags for items with associated images
		for (BFSItems item : bfsItems) {
			long bfsCount = bfsImagesDao.findCountByBfs(item);
			if (bfsCount >= 1) {
				item.setImagesFlag(true);
			}
		}

		return bfsItems;
	}

	@Override
	public boolean editRequestedItemBySeller(BFSUsers bfsUser) {
		if (bfsUser != null) {
			try {
				log.info("Entered To Edit Requested Item By Seller");
				bfsUserDao.updateQuantity(bfsUser.getId(), bfsUser.getQuantity());
				// Optionally log the successful update operation
				log.info("Successfully updated quantity for user ID: " + bfsUser.getId());
				return true;
			} catch (Exception e) {
				// Log the exception
				log.warn("Failed to update quantity for user ID: " + bfsUser.getId());
				e.printStackTrace();
				return false;
			}
		} else {
			// Optionally log the invalid input
			log.warn("Invalid BFSUsers object provided.");
			return false;
		}
	}

	@Override
	public boolean createBFSCommentByBuyer(BFSUserComments comment) {
		// TODO Auto-generated method stub
		if (comment != null) {
			try {
				log.info("Entered To Create BFS Comment By Buyer");
				bfsUserCommentsDao.save(comment);
				bfsDao.updateCommentsFlag(comment.getItems().getId());
				log.info("Completed Returning Response");
				return true;
			} catch (Exception e) {
				// Log the exception
				log.warn("Failed to Create BFS Comment");
				e.printStackTrace();
				return false;
			}
		} else {
			// Optionally log the invalid input
			log.warn("Invalid BFSUser Comments object provided.");
			return false;
		}
	}

	@Override
	public List<BFSUserComments> getCommentsByItem(BFSUserComments comment) {
		// TODO Auto-generated method stub
		if (comment == null) {
			log.warn("BFSUserComments is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSUserComments is null.");
		}
		log.info("To BFS User Comments.");
		List<BFSUserComments> commentsList = bfsUserCommentsDao.findByItemId(comment.getItems());
		if (!CollectionUtils.isEmpty(commentsList)) {
			log.info("Returning BFS::");
			return commentsList;
		} else {
			log.warn("BFS is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "No BFS Found With Id.");
		}
	}

	@Override
	public List<BFSUserComments> getCommentsByItemAndBuyer(BFSUserComments comment) {
		// TODO Auto-generated method stub
		if (comment == null) {
			log.warn("BFSUserComments is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSUserComments is null.");
		}
		log.info("To BFS User Comments.");
		List<BFSUserComments> commentsList = bfsUserCommentsDao.findByItemId(comment.getItems(), comment.getUser());
		if (!CollectionUtils.isEmpty(commentsList)) {
			log.info("Returning BFS::");
			return commentsList;
		} else {
			log.warn("BFS is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "No BFS Found With Id.");
		}
	}

//	@Override
//	public List<BFSItems> getRequestedItemsByCM() {
//		// TODO Auto-generated method stub
//		log.info("Entered To Get Requested Items By User");
//
//		List<String> itemIds = bfsUserDao.findItems();
//		if (CollectionUtils.isEmpty(itemIds)) {
//			log.info("No item IDs found");
//			return new ArrayList<>();
//		}
//
//		List<BFSItems> bfsItems = bfsDao.findByIdIn(itemIds);
//		if (CollectionUtils.isEmpty(bfsItems)) {
//			log.info("No BFS items found for item IDs {}", itemIds);
//			return new ArrayList<>();
//		}
//		for (BFSItems item : bfsItems) {
//			MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_REQUESTED);
//			long value = bfsUserDao.getCountByStatusAndItem(status, item.getId());
//			log.info("Item status count for user {}: {}", value);
//			long bfsCount = bfsImagesDao.findCountByBfs(item);
//			if (bfsCount > 0) {
//				item.setImagesFlag(true);
//			}
//			if (value > 0) {
//				item.setRequestedFlag(true);
//			}
//		}
//
//		return bfsItems;
//	}

//	@Override
//	public List<BFSItems> getRequestedItemsByCM() {
//
//	    log.info("Entered To Get Requested Items By User");
//
//	    List<BFSItems> bfsItems = bfsUserDao.findItemsOrderedByLatestBid();
//
//	    if (CollectionUtils.isEmpty(bfsItems)) {
//	        log.info("No bid items found");
//	        return new ArrayList<>();
//	    }
//
//	    MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_REQUESTED);
//
//	    for (BFSItems item : bfsItems) {
//
//	        long value = bfsUserDao.getCountByStatusAndItem(status, item.getId());
//	        long bfsCount = bfsImagesDao.findCountByBfs(item);
//
//	        item.setRequestedFlag(value > 0);
//	        item.setImagesFlag(bfsCount > 0);
//	    }
//
//	    return bfsItems;
//	}

	@Override
	public List<BFSItems> getRequestedItemsByCM() {

		log.info("Entered To Get Requested Items By User");

		// 1️⃣ Get lightweight items ordered by latest bid
		List<BFSItems> bfsItems = bfsUserDao.findItemsOrderedByLatestBidProjected();

		if (CollectionUtils.isEmpty(bfsItems)) {
			log.info("No bid items found");
			return new ArrayList<>();
		}

		List<String> itemIds = bfsItems.stream().map(BFSItems::getId).toList();

		MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BID_REQUESTED);

		// 2️⃣ Bulk requested counts
		Map<String, Long> requestedCountMap = bfsUserDao.countRequestedByItemIds(status, itemIds).stream()
				.collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));

		// 3️⃣ Bulk image counts
		Map<String, Long> imageCountMap = bfsImagesDao.countImagesByItemIds(itemIds).stream()
				.collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));

		// 4️⃣ Set flags (NO extra DB calls)
		for (BFSItems item : bfsItems) {
			item.setRequestedFlag(requestedCountMap.getOrDefault(item.getId(), 0L) > 0);
			item.setImagesFlag(imageCountMap.getOrDefault(item.getId(), 0L) > 0);
		}

		return bfsItems;
	}

	@Override
	public BfsDTO getItemByUniqueId(String uniqueId) {
		// TODO Auto-generated method stub
		if (uniqueId == null) {
			log.warn("UniqueId is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "UniqueId is null.");
		}
		log.info("Entered To Item Details By Unique ID");
		BFSUsers user = bfsUserDao.findByUniqueId(uniqueId);
		if (user != null) {
			BfsDTO bfsDto = bfsDetails(user);
			log.info("Completed and Returning response");
			return bfsDto;
		} else {
			log.warn("No BFS Users found with the requested status {}", StatusConstants.BID_REQUESTED);
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

	@Override
	public List<BFSImages> getImagesByBfs(BFSItems item) {
		// TODO Auto-generated method stub
		if (item == null) {
			log.warn("BFSItem is null. Exiting method.");
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "BFSItem is null.");
		}
		log.info("To Get BFS Images By BFSItems.");
		List<BFSImages> imagesList = bfsImagesDao.findByBfs(item.getId());
		if (CollectionUtils.isEmpty(imagesList)) {
			log.warn("BFS Images is null for BFSItem.");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		return imagesList;

	}

	@Override
	public VendorInfoBean getuserInfoById(User user) {
		// TODO Auto-generated method stub
		log.info("Entered To Get User Info");
		VendorInfoBean vendorInfoBean = new VendorInfoBean();

		if (user != null) {
			Optional<User> userData = userDao.findById(user.getId());
			if (userData.isPresent()) {
				User userRes = userData.get();

				vendorInfoBean.setSubCategory(userRes.getOrg().getSubCategory());
				vendorInfoBean.setCategory(userRes.getOrg().getDetails());
				vendorInfoBean.setEmail(userRes.getUsername());
				vendorInfoBean.setOrganizationPhonenumber(userRes.getPhone());
				vendorInfoBean.setPan(userRes.getOrg().getGstin());
				vendorInfoBean.setCompanyName(userRes.getOrg().getCompanyName());
				vendorInfoBean.setFullName(userRes.getFullName());
				vendorInfoBean.setCity(userRes.getOrg().getCity());
				vendorInfoBean.setAddress(userRes.getOrg().getAddress1());
				vendorInfoBean.setVendorClass(userRes.getOrg().getVendorClass());

			}

			log.info("Completed and returning response");
			return vendorInfoBean;
		} else {
			log.error("No Vendors  available in the Database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}

	@Override
	public boolean deactivateCommentsFlag(BFSItems item) {
		log.info("Entered to deactivate comments flag");
		if (item == null) {
			log.warn("BFSItems object is null, cannot deactivate comments flag");
			return false;
		}
		try {
			bfsDao.deactivateComment(item.getId());
			log.info("Successfully deactivated comments flag for item ID: " + item.getId());
			return true;
		} catch (Exception e) {
			log.error("Error deactivating comments flag for item ID: " + item.getId(), e);
			return false;
		}
	}

	@Override
	public List<BFSItemMainDetailsDTO> getBfsItemsByCategory(List<BFSItemDto> items) {

		Set<String> categoryKeywords = new HashSet<>();
		Set<String> descriptionKeywords = new HashSet<>();

		for (BFSItemDto dto : items) {
			if (dto.getCategory() != null) {
				dto.getCategory().forEach(k -> categoryKeywords.add(k.toLowerCase()));
			}
			if (dto.getDescription() != null) {
				dto.getDescription().forEach(k -> descriptionKeywords.add(k.toLowerCase()));
			}
		}

		log.info("Category Keywords => {}", categoryKeywords);
		log.info("Description Keywords => {}", descriptionKeywords);

		// Call optimized search with separate keyword lists
		return searchRepository.searchItems(categoryKeywords, descriptionKeywords, 5);
	}

	@Transactional
	public void processExcel(MultipartFile file) {

		List<BFSItems> items = new ArrayList<>();

		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

			Sheet sheet = workbook.getSheetAt(0);
			MasterStatus status = masterStatusDao.findByStatus(StatusConstants.BFS_NEW);

			for (int i = 1; i <= sheet.getLastRowNum(); i++) { // skip header

				final int rowNum = i + 1;

				Row row = sheet.getRow(i);
				if (row == null)
					continue;

				String orgUuid = getString(row.getCell(12));
				String userId = getString(row.getCell(13));

//	            if (orgUuid == null || userId == null) {
//	                throw new RuntimeException("Missing orgId or userId at row " + rowNum);
//	            }
//
				if (orgUuid != null) {
					Organization org = orgDao.findById(orgUuid)
							.orElseThrow(() -> new RuntimeException("Invalid orgId at row " + rowNum));

					BFSItems item = new BFSItems();
					item.setId(UUID.randomUUID().toString());
					item.setCreatedTS(new Date());

					// --- Correct mappings ---
					item.setDescription(getString(row.getCell(1))); // ItemDescription
					item.setSpecification(getString(row.getCell(2))); // Specification
					item.setUnitofMeasures(getString(row.getCell(3))); // Uom
					item.setCategory(getString(row.getCell(4))); // Category

					double qty = getNumeric(row.getCell(5)); // Quantity
					item.setTotalQuantity(qty);
					item.setAvailableQuantity(qty);

					item.setLocation(getString(row.getCell(6))); // Location
					item.setAgeOfAsset(getString(row.getCell(7))); // AgeOfAsset

					double buyPrice = getNumeric(row.getCell(8)); // BuyPrice
					item.setSellPrice(buyPrice);
					item.setAskPrice(buyPrice);

					item.setDiscount(getNumeric(row.getCell(9))); // Discount
					item.setRemarks(getString(row.getCell(10))); // Remarks
					item.setBfsGroup(getString(row.getCell(11))); // BFSGroup

					item.setOrg(org);
					item.setUserId(userId);
					item.setStatus(status);

					item.setRequestedFlag(false);
					item.setCommentsFlag(false);
					item.setImagesFlag(false);
					item.setBuyPriceDisclosure(false);

					items.add(item);
				}
			}
			bfsDao.saveAll(items);

		} catch (Exception e) {
			throw new RuntimeException("Excel processing failed", e);
		}
	}

	private String getString(Cell cell) {
		if (cell == null)
			return null;
		cell.setCellType(CellType.STRING);
		return cell.getStringCellValue().trim();
	}

	private double getNumeric(Cell cell) {
		if (cell == null)
			return 0;
		return cell.getNumericCellValue();
	}

//	@Override
//	public List<BfsDTO> getBidsBySeller(BFSUsers user) {
//		// TODO Auto-generated method stub
//		log.info("Entered To Get Bids Buyer And Item");
//		List<BfsDTO> bfsList = new ArrayList<>();
//		List<BFSUsers> usersList = bfsUserDao.findByUser(user.getId());
//		if (!CollectionUtils.isEmpty(usersList)) {
//			for (BFSUsers bfsUser : usersList) {
//				BfsDTO bfsDto = bfsDetails(bfsUser);
//				bfsList.add(bfsDto);
//			}
//			return bfsList;
//		} else {
//			log.warn("No BFS Users found with the requested status {}", StatusConstants.BID_REQUESTED);
//			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
//					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
//		}
//	}

	@Override
	public List<BfsDTO> getBidsBySeller(BFSUsers user) {

		log.info("Entered To Get Bids By Seller");

		List<BfsDTO> bfsList = new ArrayList<>();

		// Step 1: get distinct itemIds from BFSItems using sellerId
		List<String> itemIds = bfsDao.findDistinctItemIdsBySellerId(user.getId());

		if (CollectionUtils.isEmpty(itemIds)) {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		// Step 2: fetch BFSUsers using itemIds
		List<BFSUsers> usersList = bfsUserDao.findByItemIdIn(itemIds);

		if (!CollectionUtils.isEmpty(usersList)) {
			for (BFSUsers bfsUser : usersList) {
				BfsDTO bfsDto = bfsDetails(bfsUser);
				bfsList.add(bfsDto);
			}
			return bfsList;
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}

}
