package com.portal.procucev.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.Dto.BfsDTO;
import com.portal.procucev.Dto.VendorInfoBean;
import com.portal.procucev.model.BFSDocuments;
import com.portal.procucev.model.BFSImages;
import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.BFSUserComments;
import com.portal.procucev.model.BFSUsers;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;
import com.portal.procucev.service.BFSService;
import com.portal.procucev.utils.ApplicationConstants;

@RestController
@RequestMapping("/rest/bfs")
public class BFSController {
	private static final Logger log = LoggerFactory.getLogger(BFSController.class);

	@Autowired
	BFSService bfsService;

	@PostMapping(value = "/orgSearch")

	public ResponseEntity<?> organizationSearch(@RequestBody Organization org) {
		log.info("Entered To Search Organization");
		List<Organization> status = bfsService.orgSearch(org);
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	@PostMapping("/getUsersByOrg")
	public ResponseEntity<?> getUsersByOrg(@RequestBody Organization org) {
		List<User> clientList = bfsService.getUsersByOrg(org);
		return new ResponseEntity<>(clientList, HttpStatus.OK);
	}

	@PostMapping(value = "/createBfsItems")
	public ResponseEntity<?> createBfs(@RequestBody List<BFSItems> items) throws AppException {
		log.info("BFS Item Creation");
		boolean bfsList = bfsService.createBfs(items);
		String statusCode = bfsList ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = bfsList ? String.format(ApplicationConstants.CREATE_BFS, items.get(0).getId())
				: String.format(ApplicationConstants.BFS_FAILED, items.get(0).getId());
		String code = bfsList ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/getItemsByOrgAndUser")
	public ResponseEntity<?> getItemsByOrgAndGroup(@RequestBody User user) {
		List<BFSItems> itemsList = bfsService.getItemsByOrgAndUser(user);
		return new ResponseEntity<>(itemsList, HttpStatus.OK);
	}

	@PostMapping("/getAllBfsItems")
	public ResponseEntity<?> getAllItems(@RequestBody User user) {
		List<BFSItems> itemsList = bfsService.getAllItems(user);
		return new ResponseEntity<>(itemsList, HttpStatus.OK);
	}

	@PostMapping(value = "/requestBfsItem")
	public ResponseEntity<?> requestBfsItem(@RequestBody BFSUsers bfsUser) throws AppException {
		log.info("Request BFS By User");
		boolean status = bfsService.requestBfsItem(bfsUser);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.BFS_REQUEST_SUCCESS)
				: String.format(ApplicationConstants.BFS_REQUEST_FAILED);
		String code = status ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/approveBfsItem")
	public ResponseEntity<?> approveBfsItem(@RequestBody BFSUsers bfsUser) throws AppException {
		log.info("Approve BFS By User");
		String msg = null;
		boolean status = bfsService.approveBfsItem(bfsUser);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		if (bfsUser.isApproval()) {
			msg = status ? String.format(ApplicationConstants.BFS_Approve_SUCCESS)
					: String.format(ApplicationConstants.BFS_Approve_FAILED);
		} else {
			msg = status ? String.format(ApplicationConstants.BFS_REJECT_SUCCESS)
					: String.format(ApplicationConstants.BFS_REJECT_FAILED);
		}
		String code = status ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/getRequestedUserByBFS")
	public ResponseEntity<?> getRequestedUserByBFS(@RequestBody BFSItems item) {
		List<BFSUsers> usersList = bfsService.getRequestedUserByBFS(item);
		return new ResponseEntity<>(usersList, HttpStatus.OK);
	}

	@GetMapping("/getRequestedItems")
	public ResponseEntity<?> getAllItemMaster() throws AppException {
		List<BfsDTO> usersList = bfsService.getRequestedItems();
		return new ResponseEntity<>(usersList, HttpStatus.OK);
	}

	@PostMapping("/geApprovedItemsByUser")
	public ResponseEntity<?> geApprovedItems(@RequestBody User user) throws AppException {
		List<BfsDTO> usersList = bfsService.getApprovedItems(user);
		return new ResponseEntity<>(usersList, HttpStatus.OK);
	}

	@PostMapping("/getBidsByBuyerAndItems")
	public ResponseEntity<?> getBidsByBuyerAndItem(@RequestBody BFSUsers user) throws AppException {
		List<BfsDTO> usersList = bfsService.getBidsByBuyer(user);
		return new ResponseEntity<>(usersList, HttpStatus.OK);
	}

	@PostMapping(value = "/acceptBfsItemBySeller")
	public ResponseEntity<?> acceptBfsItem(@RequestBody BFSUsers bfsUser) throws AppException {
		log.info("Accept BFS By Seller ==>");
		String msg = null;
		boolean status = bfsService.acceptBfsItemBySeller(bfsUser);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		msg = status ? String.format(ApplicationConstants.BFS_ACCEPT_SUCCESS)
				: String.format(ApplicationConstants.BFS_ACCEPT_FAILED);

		String code = status ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/rejectBfsItemBySeller")
	public ResponseEntity<?> rejectBfsItemBySeller(@RequestBody BFSUsers bfsUser) throws AppException {
		log.info("Reject BFS By User");
		String msg = null;
		boolean status = bfsService.rejectBfsItemBySeller(bfsUser);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		msg = status ? String.format(ApplicationConstants.BFS_REJECT_SUCCESS)
				: String.format(ApplicationConstants.BFS_REJECT_FAILED);

		String code = status ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/getDocumentsByBfs")
	public ResponseEntity<?> getDocumentsByBfs(@RequestBody BFSItems item) throws AppException {
		List<BFSDocuments> documentsList = bfsService.getDocumentsByBfs(item);
		return new ResponseEntity<>(documentsList, HttpStatus.OK);
	}

	@PostMapping("/getRequestedUserByBFSAndStatus")
	public ResponseEntity<?> getRequestedUserByBFSAndStatus(@RequestBody BFSItems item) {
		List<BFSUsers> usersList = bfsService.getRequestedUserByBFSAndStatus(item);
		return new ResponseEntity<>(usersList, HttpStatus.OK);
	}

	@PostMapping("/getBfsItemsByBOQ")
	public ResponseEntity<?> getBfsItemsByBOQ(@RequestBody BFSItems item) throws AppException {
		List<BFSItems> itemsList = bfsService.createBfsByBoq(item);
		return new ResponseEntity<>(itemsList, HttpStatus.OK);
	}

	@PostMapping("/getBfsById")
	public ResponseEntity<?> getBfsById(@RequestBody BFSItems item) throws AppException {
		BFSItems itemResponse = bfsService.getBfsById(item);
		return new ResponseEntity<>(itemResponse, HttpStatus.OK);
	}

	@PostMapping(value = "/editBfsItems")
	public ResponseEntity<?> editBfs(@RequestBody BFSItems items) throws AppException {
		log.info("BFS Item Edit");
		boolean bfsList = bfsService.editBfs(items);
		String statusCode = bfsList ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = bfsList ? String.format(ApplicationConstants.EDIT_BFS, items.getId())
				: String.format(ApplicationConstants.BFS_EDIT_FAILED, items.getId());
		String code = bfsList ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/editBfsUser")
	public ResponseEntity<?> editBfsUser(@RequestBody BFSUsers bfsUser) throws AppException {
		log.info("BFS Item Edit");
		boolean bfsList = bfsService.editBfsUser(bfsUser);
		String statusCode = bfsList ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = bfsList ? String.format(ApplicationConstants.BID_EDIT_SUCCESS, bfsUser.getId())
				: String.format(ApplicationConstants.BID_EDIT_FAILED, bfsUser.getId());
		String code = bfsList ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/getRequestedItemByBuyer")
	public ResponseEntity<?> getRequestedItemByBuyer(@RequestBody User user) throws AppException {
		List<BFSItems> itemsList = bfsService.getRequestedItemByBuyer(user);
		return new ResponseEntity<>(itemsList, HttpStatus.OK);
	}

	@GetMapping("/getRequestedItemsByCM")
	public ResponseEntity<?> getRequestedItemsByCM() throws AppException {
		List<BFSItems> itemsList = bfsService.getRequestedItemsByCM();
		return new ResponseEntity<>(itemsList, HttpStatus.OK);
	}

	@PostMapping(value = "/editBuyerItemBySeller")
	public ResponseEntity<?> editBuyerItemBySeller(@RequestBody BFSUsers bfsUser) throws AppException {
		log.info("Entered into EditBuyerItemBySeller");
		boolean bfsList = bfsService.editRequestedItemBySeller(bfsUser);
		String statusCode = bfsList ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = bfsList ? String.format(ApplicationConstants.BID_EDIT_SUCCESS, bfsUser.getId())
				: String.format(ApplicationConstants.BID_EDIT_FAILED, bfsUser.getId());
		String code = bfsList ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/createBFSCommentByBuyer")
	public ResponseEntity<?> createBFSCommentByBuyer(@RequestBody BFSUserComments comment) throws AppException {
		log.info("BFS Buyser Comment Creation");
		boolean status = bfsService.createBFSCommentByBuyer(comment);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.BFS_COMMENT_SUCCESS)
				: String.format(ApplicationConstants.BFS_COMMENT_FAILED);
		String code = status ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/getCommentsByItem")
	public ResponseEntity<?> getCommentsByItem(@RequestBody BFSUserComments comment) throws AppException {
		List<BFSUserComments> itemsList = bfsService.getCommentsByItem(comment);
		return new ResponseEntity<>(itemsList, HttpStatus.OK);
	}

	@PostMapping("/getCommentsByItemAndBuyer")
	public ResponseEntity<?> getCommentsByItemAndBuyer(@RequestBody BFSUserComments comment) throws AppException {
		List<BFSUserComments> itemsList = bfsService.getCommentsByItemAndBuyer(comment);
		return new ResponseEntity<>(itemsList, HttpStatus.OK);
	}

	@PostMapping("/getItemByUniqueId")
	public ResponseEntity<?> getItemByUniqueId(@RequestBody BFSUsers user) throws AppException {
		BfsDTO itemsList = bfsService.getItemByUniqueId(user.getUniqueId());
		return new ResponseEntity<>(itemsList, HttpStatus.OK);
	}

	@PostMapping("/getImagesByBfs")
	public ResponseEntity<?> getImagesByBfs(@RequestBody BFSItems item) throws AppException {
		List<BFSImages> documentsList = bfsService.getImagesByBfs(item);
		return new ResponseEntity<>(documentsList, HttpStatus.OK);
	}

	@PostMapping(value = "/getUserInfoById")
	public ResponseEntity<?> getuserInfoById(@RequestBody User user) throws IOException {
		log.info("Entered to get User info By Id");
		VendorInfoBean response = bfsService.getuserInfoById(user);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	@PostMapping(value = "/deactivateCommentsFlag")
	public ResponseEntity<?> deactivateCommentsFlag(@RequestBody BFSItems item) throws IOException {
		log.info("Entered to deactivate Comments Flag");
		boolean response = bfsService.deactivateCommentsFlag(item);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
}
