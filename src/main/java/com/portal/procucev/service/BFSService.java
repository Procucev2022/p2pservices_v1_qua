package com.portal.procucev.service;

import java.util.List;

import com.portal.procucev.Dto.BFSItemDto;
import com.portal.procucev.Dto.BFSItemMainDetailsDTO;
import com.portal.procucev.Dto.BfsDTO;
import com.portal.procucev.Dto.VendorInfoBean;
import com.portal.procucev.model.BFSDocuments;
import com.portal.procucev.model.BFSImages;
import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.BFSUserComments;
import com.portal.procucev.model.BFSUsers;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;

public interface BFSService {

	List<Organization> orgSearch(Organization org);

	List<User> getUsersByOrg(Organization org);

	boolean createBfs(List<BFSItems> items);

	List<BFSItems> getAllItems(User user);

	List<BFSItems> createBfsByBoq(BFSItems items);

	List<BFSItems> getItemsByOrgAndUser(User user);

	boolean requestBfsItem(BFSUsers bfsUser);

	List<BFSUsers> getRequestedUserByBFS(BFSItems item);

	List<BfsDTO> getRequestedItems();

	boolean approveBfsItem(BFSUsers bfsUser);

	List<BfsDTO> getApprovedItems(User user);

	List<BfsDTO> getBidsByBuyer(BFSUsers user);

	boolean acceptBfsItemBySeller(BFSUsers bfsUser);

	boolean rejectBfsItemBySeller(BFSUsers bfsUser);

	List<BFSDocuments> getDocumentsByBfs(BFSItems item);

	List<BFSUsers> getRequestedUserByBFSAndStatus(BFSItems item);

	BFSItems getBfsById(BFSItems item);

	boolean editBfs(BFSItems items);

	boolean editBfsUser(BFSUsers bfsUser);

	List<BFSItems> getRequestedItemByBuyer(User user);

	boolean editRequestedItemBySeller(BFSUsers bfsUser);

	boolean createBFSCommentByBuyer(BFSUserComments comment);

	List<BFSUserComments> getCommentsByItem(BFSUserComments comment);

	List<BFSUserComments> getCommentsByItemAndBuyer(BFSUserComments comment);

	List<BFSItems> getRequestedItemsByCM();

	BfsDTO getItemByUniqueId(String uniqueId);

	List<BFSImages> getImagesByBfs(BFSItems item);

	VendorInfoBean getuserInfoById(User user);

	boolean deactivateCommentsFlag(BFSItems item);

	List<BFSItemMainDetailsDTO> getBfsItemsByCategory(List<BFSItemDto> items);

}
