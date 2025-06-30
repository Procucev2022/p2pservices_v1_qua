package com.portal.procucev.utils;
public interface ApplicationConstants {
	public String MAIL_SENDING_FAILURE = "Unable to send a mail. Kindly check your credentials...";

	public String[] CLIENT_GRANT = { "client-credentials", "password", "refresh_token" };
	String[] CLIENT_AUTHORITY = { "ROLE_CLIENT", "ROLE_ANDROID_CLIENT" };
	String[] CLIENT_SCOPE = { "read", "write", "trust" };
	String CLIENT_RESOURCEID = "oauth2-resource";

	// Vendor registration related constants
	public String VENDOR_REGISTRSTION_FAILED = "Failed to register vendor...";
	public String VENDOR_SUBMIT_SUCCESS = "You Have Successfully Submitted Registration Details.";
	public String VENDOR_SUBMIT_FAILED = "Failed to submit vendor...";
	public String VENDOR_APPROVE_SUCCESS = "Vendor Approved Successfully";
	public String VENDOR_APPROVE_FAILED = "Failed to approve vendor";
	public String VENDOR_REJECTION_SUCCESS = "Vendor Rejected Successfully";
	public String VENDOR_REJECTION_FAILED = "Vendor Rejection Failed";
	public String NO_ORG_FOUND="No Orgaization Found";

	public String DELETE_RFQVENDOR = "Vendor %s deleted successfully";
	public String RFQVENDOR_NOT_EXISTS = "Vendor does not exist";
	public String RFQ_SEND_SUCCESS = "Rfq sent successfully to the selected vendor(s)";
	public String RFQ_SEND_FAILURE = "Unable to send Rfq to the selected vendor(s)";
	public String SUBMIT_PR = "PR %s submitted successfully";
	public String PR_CANCEL_SUCCESS = "PR %s cancelled successfully";
	public String PR_CANCEL_FAILURE = "PR %s cancelled failed";
	public String PR_CLOSED_SUCCESS = "PR %s closed successfully";
	public String PR_CLOSED_FAILURE = "PR %s closed failed";
	public String PR_Approved_SUCCESS = "PR %s approved successfully";
	public String PR_Approved_FAILURE = "PR %s approved failed";
	public String PR_ACCEPT_SUCCESS = "PR %s accepted successfully";
	public String PR_ACCEPT_FAILURE = "PR %s accepted failed";
	public String PR_REJECT_SUCCESS = "PR %s rejected successfully";
	public String PR_REJECTt_FAILURE = "PR %s reject failed";
	public String RFQ_CREATED_SUCCESS = "RFQ %s created successfully";
	public String RFQ_CREATED_FAILURE = "RFQ %s create failed";
	public static final String NO_NEW_QUOTES = "No new quotes found";

	public String QUERY_CREATED_SUCCESS = "Query Created Successfully";
	public String QUERY_CREATED_FAILURE = "Failed to create Query...";

	public String COMMENT_CREATED_SUCCESS = "Comment Created Successfully";
	public String COMMENT_CREATED_FAILURE = "Failed to create comment...";
	public String NO_QUOTATION_FOUND = "No Quotations found for the given rfq";

	public String VENDOR_ALREADY_EXISTS = "Vendor Already Registered with Name ";

	public String USER_ALREADY_ASSOCIATED_TO_ACCOUNT = "User already exist and associated to an Account. Please try with different User";

	public String VENDOR = "VENDOR";

	public String VENDOR_DETAILS_DOESNT_EXIST = "Vendor Details Doesn't Exist";

	public String PASSWORD_CHANGED_UNSUCCESS = "Password failed to updated";

	public String PASSWORD_CHANGED_SUCCESS = "Password updated Successfully";

	public String PR_DATA_NOT_AVAILABLE = "No PR created for your Organization";

	public String PR_ITEMS_NOT_FOUND_FOR_PR = "No Items found for Selected PR";

	public String PR_DATA_NOT_FOUND = "No PR Details Available";

	public String RFQ_DATA_NOT_FOUND = "Selected PR doesn't have any RFQ's";

	public String QUOTATION_NOT_FOUND_VENDOR = "No Quotation Available For Your Organization";

	public String NO_QUOTATION_ITEMS_FOUND_FOR_QUOTATION = "Selected Quotation Doesn't have any Items";

	public String QUOTATION_SUBMITTED_SUCCESS = "Quotation Submitted Successfully";

	public String QUOTATION_SUBMITTED_FAILED = "Quotation Submitted Failed. Please Try Again.";

	public String QUOTATION_CREATION_FAILED = "Quotation Failed To Create For Selected RFQ. Please Try Again";

	public String QUOTATION_DETAIL_NOT_FOUND = "Quotation Details Not Found";

	public String PAN_ALREADY_EXISTS = "Vendor Already Exists With Same PAN Number";

	public String REQUEST_VENDOR = "Requested Vendor Successfully";

	public String VENDORREQ_DATA_NOT_FOUND = "Requested Vendor Not Found";

	public String VENDOR_REQ_CLOSED_SUCCESS = "Requested Vendor Closed";

	public String VENDOR_REQ__CLOSED_FAILURE = "Failed To Close The Requested Vendor";

	public String CLIENT_CREATION_UNSUCCESS = "Failed To Create Client";

	public String CLIENT_CREATION_SUCCESS = "Client Created Successfully";

	public String CLIENT_ALREADY_EXISTS = "Client Already exists";

	public String CLIENT = "CLIENT";

	public String PRApprover = "PRApprover";

	public static final String FAILURE = "Failure";
	public static final String WARNING = "Warning";
	public static final int orgTypeForVendor = 1;
	public static final String SUCCESS = "Success";
	public static final String CREATE_VENDOR = "Registration Details Updated Successfully";
	public static final String VENDOR_EXISTS = "Vendor %s already exists!!!";
	public static final String VENDOR_FAILED = "Vendor %s Creation Failed!!!";
	public static final String NO_DATA_FOUND = "No details found";
	public static final String BUSSINESS_EXCEPTION = "Bussiness Level Exception";
	public static final String No_RFQ_FOUND = "No RFQ's Found";
	public static final String CREATE_RFQ = "RFQ %s created successfully..";
	public static final String SUBMIT_RFQ = "RFQ %s submitted successfully..";
	public static final String RFQ_EXISTS = "RFQ %s already exists!!!";

	public static final String CREATE_QUOTE = "Quotation %s Created Successfully";

	public static final String QUOTE_EXISTS = "Quotation %s already exists..";

	public static final String DATABASE_EXCEPTION = "Database Level Exception";
	public static final String SERVICE_LEVEL_EXCEPTION = "Service Level Exception";
	public static final String PR_NOT_FOUND = "PR not found with ID %s";
	public static final String DELETE_RFQ = "RFQ(s) deleted successfully..";
	public static final String RFQ_NOT_EXISTS = "RFQ(s) doesn't exists!!!";
	public static final String MAIL_SENT_SUCCESS = "Mail sent successfully..";
	public static final String MAIL_SENT_UNSUCCESS = "Unable to send the mail. Please check the mail credentials..";
	public static final String DELETE_VENDOR = "Deleted vendor(s) successfully..";
	public static final String DELETE_VENDOR_NOT_EXISTS = "Delete vendor(s) unsuccessful..";
	public static final String Query = "query crated successful";
	public static final String PR_Accepted = "Pr accepted successfully";
	public static final String ADD_VENDOR = "Vendor %s is added..";
	public static final String ADD_VENDOR_FAIL = "Vendor %s is not added";
	public static final String CREATE_CLIENT = "Client %s created successfully..";
	public static final String CLIENT_EXISTS = "Client %s already exists!!!";
	public static final String FILE_CANNOT_BE_PROCESSED = "File cannot be processed";
	public static final String CREATE_PR = "PR %s created successfully..";
	public static final String PR_EXISTS = "PR %s already Exist!!!";
	public static final String PR_FAILED = "PR %s Failed to Create";
	public static final String DELETE_CLIENT = "Deleted client(s) successfully..";
	public static final String DELETE_CLIENT_NOT_EXISTS = "Delete client(s) unsuccessful..";
	public static final String FAILED_TO_CREATE = "Failed to create PR";
	public static final String FAILED_TO_CREATE_CLIENT = "Failed to create Client";
	public static final String DELETE_PR_SUCCESSFUL = "Deleted PR(s) successfully..";
	public static final String DELETE_PR_UNSUCCESSFUL = "Delete PR(s) unsuccessful..";
	public static final String PR_NOT_EXISTS = "PR does not exists";
	public static final String PR_STATUS_NULL = "PR status null in database";
	public static final String DATA_SAVING_FAILED = "Data saving failed in to data base";
	public static final String VENDOR_VALIDATION_SUCCESSFUL = "vendor validation success";
	public static final String VEDNOR_VALIDATION_FAILED = "vendor validation failed";

	public static final String VENDOR_ACCEPT_SUCCESS = "Vendor  Registration Accepted Successfully...";
	public static final String VENDOR_ACCEPT_FAILURE = "Vendor  Registration Accept Failed...";
	public static final String VENDOR_REJECT_SUCCESS = "Vendor  Registration Rejected ...";
	public static final String VENDOR_REJECT_FAILURE = "Vendor  Registration Rejected Failed...";

	public static final String USER_DETAILS_NOT_FOUND = "User Doesn't exist";

	public String FILE_HAS_EMPTY_ROWS = "BOQ File has Empty Rows. Please Upload Valid File";

	public String REQUIRED_FORMAT = "S.No, Category, Item Code, Item Description, Material Specification, UOM, Qty";

	public String FILE_FORMAT_NOT_MATCHED_DATA = "File format is not correct. Please have %s columns in that order";

	public String SNO = "S.No";

	public String CATEGORY = "Category";

	public String ITEMCODE = "Item Code";

	public String ITEM_DESCRIPTION = "Item Description";

	public String MATERIAL = "Material Specification";

	public String UNIT = "UOM";

	public String QUANTITY = "Qty";

	public String ITEMID = "ItemId";
	
	public String VENDOR_REQ_PROCESSED_SUCCESS = "Vendor Request Processed successfully";

	public String VENDOR_REQ__PROCESSED_FAILURE = "Failed to Process the Vendor Request";

	public String CLIENT_UPDATED_SUCCESS = "Client details updated successfully";

	public String CLIENT_UPDATION_UNSUCCESS = "Failed to update Client Details";

	public String VENDOR_RFQ_ACCEPTED_SUCCESS = "RFQ Accepted Successfully";

	public String VENDOR_RFQ_ACCEPTED_FAILED = "RFQ Failed to Accept. Please Try Again!!";

	public String VENDOR_RFQ_REJECTED_SUCCESS = "RFQ Rejected Successfully";

	public String VENDOR_RFQ_REJECTION_FAILED = "RFQ Failed to Reject. Please Try Again!!";

	public String NO_ITEMS_FOUND_FOR_PR = "Selected PR Doesn't Have Items For Quotation Comparision";

	public String VENDOR_REQ_COMPLETE_SUCCESS = "Vendor Request Completed";

	public String VENDOR_REQ_COMPLETE_FAILURE = "Failed to complete the Vendor Request";

	public String CREATE_PPO = "PPO Created Successfully";

	public String PPO_FAILED = "Failed To Create PPO";

	public String SUBMIT_PPO = "PPO Submitted Successfully";

	public String SUBMIT_FAILED = "Failed to sumit PPO";

	public String PPO_DATA_NOT_FOUND = "PPO's Not Found";

	public String ACCEPT_PPO = "PPO Accepted Successfully";

	public String ACCEPT_FAILED = "Failed to accept PPO";

	public String REJECT_PPO = "PPO Rejected Successfully";

	public String REJECT_FAILED = "Failed to Reject PPO";

	public String UPDATED_SUCCESS = "Updated Successfully";

	public String UPDATED_FAILED = "Failed to update";

	public String NO_USER_FOUND = "User Not Found";

	public String PASSWORD_RESET_MAIL_SENT_SUCCESS = "Please check your registered email for latest Login Credentials.";

	public String CREATE_AUCTION = "Auction Created Successfully";

	public String AUCTION_FAILED = "Failed to create auction";

	public String AUCTION_DATA_NOT_FOUND = "Auction Not Found";

	public String NO_ITEMS_FOUND_FOR_RFQ = "No Items Found For RFq";

	public String AUCTION_QUOTATION_VENDOR_SUCCESS = "Saved Successfully";

	public String AUCTION_QUOTATION_VENDOR_FAILED = "Failed to Save";

	public String VENDOR_NOT_FOUND = "Vendor Not Found For Requested Rfq";

	public String NO_BIDS_FOUND = "No Bids Found In Database";

	public String SEALED_BID = "sealedBid";

	public String REVERSE_AUCTION = "reversedAuction";

	public String CREATE_BID = "Raised Bid Successfully";

	public String BID_FAILED = "Failed To raise Bid";

	public String EDIT_AUCTION = "Updated Successfully";

	public String AUCTION_EDIT_FAILED = "Failed to Edit";

	public String CANCEL_AUCTION = "Auction Cancelled Successfully";

	public String CANCEL_FAILED = "Failed To Cancel Auction";

	public String RFQ = "rfq total wise";

	public String ItemWise = "item wise";

	public String AUCTION_TIME_ENDED = "Auction Timed Out";

	public String BID_AMOUNT_EXCEDED = "Bid Amount Should Not Be Greater Than StartValue Price";

	public String INVALID_HSN = "Invalid HSN Code";

	public String sealedbid = "sealed bid";

	public String reverseauction = "reverse auction";

	public String BID_SUBMITTED = "Bid Submitted Successfully";

	public String VENDOR_RANK_SUCCESS = "Vendor's rank submitted successfully";

	public String VENOR_RANK_FAILED = "Failed to submit vendor's rank";

	public String CLIENT_USER_CREATION_SUCCESS = "Clientuser created successfully";

	public String CLIENT_USER_CREATION_UNSUCCESS = "Failed to create clientuser";

	public String orgTypeForClient = "3001";

	public String VENDOR_EDIT_SUCCESS = "Updated Successfully";

	public String VENDOR_EDIT_FAILED = "Failed TO Update";

	public String BID_LIMIT_EXCEEDED = "BidLimit Exceeded";

	public String MIN_BID_AMOUNT = "Bid difference should be greater than or equal to min bid";

	public String CURRENT_BID_AMOUNT = "Bid amount should be less than last bid amount";
	
	public String CURRENT_FC_BID_AMOUNT = "Bid amount should be More than last bid amount";

	public String CURRENT_BIDITEMS_AMOUNT = "BidItem amount should be less than last biditem amount";
	
	public String CURRENT_FC_BIDITEMS_AMOUNT = "BidItem amount should be More than last biditem amount";

	public String ACCEPTTC = "Terms And Conditions Accepted Successfully";

	public String ACCEPTTCFAIL = "Failed To Accept Terms And Condition";

	public String UPLOAD_SUCCESS = "File uploaded successfully";

	public String UPLOAD_FAILED = "Failed To upload File";

	public String EDIT_PR = "PR Edited Successfully";

	public String EDIT_FAILED = "Failed to Edit PR";

	public String CREATE_PO = "PO Created Successfully";

	public String PO_FAILED = "Failed to create PO";

	public String EDIT_PO = "PO Edited Successfully";

	public String EDIT_PO_FAILED = "Failed to Edit PO";

	public String PO_DATA_NOT_FOUND = "No PO's Availabe";

	public String EDIT_DELIVERY_DATE = "Delivery Date updated successfully for delivery id: ";

	public String EDIT_DELIVERY_DATE_FAILED = "Failed to Edit Delivery Date for delivery id: ";

	public String REJECT_PO = "PO Rejected Successfully";

	public String REJECT_PO_FAILED = "Failed to Reject PO";

	public String CREATE_ASN = "ASN Created Successfully";

	public String ASN_FAILED = "Failed to create ASN";

	public String DELIVERY_ACCEPTED_SUCCESS = "Delivery Accepted Successfully";

	public String PROCUCEV_ORG_TYPE = "PROCUCEV";

	public String CLIENT_ORG_TYPE = "CLIENT";

	public String VENDOR_ORG_TYPE = "VENDOR";

	public String PO_ALREADY_EXIST = "PO Already exist for selected PPO and Vendor";

	public String DELIVERY = "Delivery Created Successfully";

	public String DELIVERY_FAILED = "Failed to create Delivery";

	public String DELIVERY_UPDATED = "Delivery Updated with delivery id: ";

	public String DELIVERY_UPDATED_FAILED = "Delivery update failed with";

	public String INVOICE_SUCCESS = "Invoice created Successfully ";

	public String INVOICE_FAILED = "Invoice creation failed";
	public String ASN_ACCEPTED_SUCCESS = "ASN Accepted Successfully";

	public String PO_CANCELLED_SUCCESS = "PO Cancelled Successfully";
	
	public String INVOICE_UPDATED_SUCCESS = "Invoice updated Successfully ";

	public String INVOICE_UPDATE_FAILED = "Invoice updation failed";

	public String INVOICE_ACCEPTED_SUCCESS = "Invoice Accepted Successfully";

	public String REQUIRED_FORMAT_VENDORDATA = "S.No, Vendor Name, Location , Mobile Nos, Contact Persons, Mail ID, Category , Sub Category, HSN Code , Website";
	
	public String REQUIRED_FORMAT_CLIENTITEMCATALOGUE = "S.No, ItemDescription, Specification , Uom , CustomerItemCode, Quantity, Category ";

	public String REQUIRED_FORMAT_CLIENTVENDORDATA = "S.No, Category, Vendor Name, Item Name";
	public String SerialNO = "S.No";
	
	public String ItemDescription = "ItemDescription";
	
	public String Specification = "Specification";
	
	public String Uom = "Uom";
	
	public String CustomerItemCode = "CustomerItemCode";
	
	public String Quantity = "Quantity";

	public String VendorName = "Vendor Name";

	public String Location = "Location";

	public String ContactPerson = "Contact Persons";

	public String email = "Mail ID";

	public String Phoneno = "Mobile Nos";

	public String Category = "Category";
	
	public String ItemName = "Item Name";

	public String SubCategory = "Sub Category";

	public String Hsncode = "HSN Code";

	public String Website = "Website";

	public String INACTIVATE_VENDOR = "Inactivated Vendor Successfully";

	public String INACTIVATE_VENDOR_FAILED = "Failed To Inactivate Vendor";

	public String EDIT_VENDOR = "Vendor Details Updated Successfully";

	public String EDIT_VENDOR_FAILED = "Failed To Update Vendor Details";

	public String ADVANCE_SUCCESS = "Advance Payment Request Created Successfully";

	public String ADVANCE_FAILED = "Failed To Create Advance Payment Request";

	public String POADVANCE_ACCEPTED_SUCCESS = "Advance Payment Request Accepted Successfully";

	public String ADVANCE_EDIT_SUCCESS = "Advance Payment Request Edited Successfully";

	public String ADVANCE_EDIT_FAILED = "Failed To Edit Advance Payment";

	public String APPROVE_PO_SUCCESS = "Client Approved PO Successfully";

	public String APPROVE_EDIT_FAILED = "Client Failed to Approve PO";

	public Object HSN = "hsn";

	public Object SAC = "sac";

	public String ITEM_DELETE_SUCCESS = "Item Deleted Successfully";

	public String ITEM_DELETE_FAILED = "Failed To Delete Item";

	public String ITEM_CREATE_SUCCESS = "Item Created Successfully";

	public String ITEM_CREATE_FAILED = "Failed To Create Item";

	public String ITEM_LINK_SUCCESS = "Item Linked to Vendors Successfully";

	public String ITEM_LINK_FAILED = "Failed To Link Item To Vendor";

	public String ITEM_EDIT_SUCCESS = "Item Edited Successfully";

	public String ITEM_EDIT_FAILED = "Failed To Edit Item";

	public String CLIENT_ITEM_LINK_SUCCESS = "Item Linked To Client Successfully ";

	public String CLIENT_ITEM_LINK_FAILED = "Failed To Link Item To Client";

	public String ITEM_CLIENT_VENDOR_LINK_SUCCESS = "Item Client Vendor Linked Successfully";

	public String ITEM_CLIENT_VENDOR_LINK_FAILED = "Failed to Link Item Client Vendor";

	public String ITEM_CATALOGUE_REQ_SUCCESS = "Item Catalogue By Client Requested Successfully";

	public String ITEM_CATALOGUE_REQ_FAILED = "Failed to Request Item Catalogue";

	public String ITEM_REQUEST_CLOSE_SUCCESS = "Requested Item Closed Successfully";

	public String ALL = "ALL";

	public String Linked = "Linked";

	public String NonLinked = "NonLinked";

	public String ITEM_CLOSE_SUCCESS = "Item Closed Successfully";

	public String SUBCATEGORY_APPROVED_SUCCESS = "SubCategory Approved Successfully";

	public String ITEM_APPROVED_SUCCESS = "Item Approved Successfully";

	public String DELINK_VENDORITEM_SUCCESS = "Vendor Item Delinked From Client Successfully";

	public String UOM_CREATED_SUCCESS = "UOM Created Successfully";

	public String UOM_EDITED_SUCCESS = "UOM Edited Successfully";

	public String ITEM_EDIT_ENABLED = "Item Edit Enabled Successfully";

	public CharSequence Sunday = "Sunday";

	public CharSequence Monday = "Monday";

	public CharSequence Tuesday = "Tuesday";

	public CharSequence Wednesday = "Wednesday";

	public CharSequence Thursday = "Thursday";

	public CharSequence Friday = "Friday";

	public CharSequence Saturday = "Saturday";

	public String ITEM_ADD_SUCCESS = "Item Linked Successfully";

	public String VENDOR_EVALUATION_SUCCESS = "Vendor Evaluated Successfully";

	public String VENDOR_EVALUATION_FAILED = "Failed To Evaluate Vendor";

	public String SUBCATEGORY_REJECTED_SUCCESS = "Subcategory Rejected Successfully";

	public String ITEM_REJECTED_SUCCESS = "Item Rejected Successfully";

	public String Refference = "Refference";

	public String VendorClass = "VendorClass";

	public String VendorType = "VendorType";

	public String VENDOR_DLINK_SUCCESS = "Dlinked Vendor Successfully";

	public String CREATE_PERMISSION = "Added Permission to User";

	public String PERMISSION_FAILED = "Failed To Add Permission";
	
	public String All = "ALL";

	public String VENDOR_ENABLED_SUCCESS = "Vendor Enabled Successfully";

	public String VENDOR_ENABLED_FAILED = "Failed To Enable Vendor";

	public String CREATE_SELF_VENDOR = "Thanks for your interest with procucev, our vendor partner will connect with you";

	public String SELF_VENDOR_FAILED = "Failed To Update Vendor Details ";

	public String PartialVendor = "PartialVendor";

	public String FORWARDAUCTION = "Forward Auction";

	public String BID_AMOUNT_LESS = "BidAmount is Less than Start Price Value";

	public String VendorExecutive2 = "VendorExecutive2";

	public String ClientInitiator = "ClientInitiator";

	public String DYNAMIC_PRICING_CREATE_SUCCESS = "Dynamic Pricing Created Sucessfully";

	public String DYNAMIC_PRICING_FAILED = "Failed To Create Dynamic Pricing";

	public String ITEM_DYNAMIC_PRICING_DISABLED = "Disabled Dynamic Pricing For Item Succesfully";

	public String ITEM_DYNAMIC_PRICING_ENABLED = "Enabled Dynamic Pricing For Item Succesfully";

	public String ITEM_DYNAMIC_PRICING_DISABLED_VENDOR = "Disabled Dynamic Pricing For Vendor Succesfully";

	public String ITEM_DYNAMIC_PRICING_ENABLED_VENDOR = "Enabled Dynamic Pricing For Vendor Succesfully";

	public String all = "all";

	public String SUBCATEGORY_EDIT_SUCCESS = "SubCategory edited successfully";

	public String ITEM_CATALOGUE_UPDATE_SUCCESS = "Item Catalogue Updated Successfully";

	public String ITEM_CATALOGUE_UPDATE_FAILED = "Failed To Update Item Catalogue";

	public String REQUIRED_FORMAT_RFQITEMFORMAT = "S.No, ItemDescription,  Specification, Uom, Quantity, Remarks";

	public String RFQ_FORWARD_SUCCESS = "RFQ Forwarded Successfully";

	public String RFQ_FORWARD_FAILURE = "Failed To Forward RFQ";

	public String Spec = " Specification";

	public String Remarks = " Remarks";

	public String AUTHENTICATE_UNSUCCESS = "Failed To Authenticate";

	public String AUTHENTICATE_SUCCESS = "Authenticated Successfully";

	public String RFQ_REQUEST_SUCCESS = "RFQ Requested Successfully";

	public String RFQ_REQUEST_FAILURE = "Failed To Request RFQ";

	public String RFQ_IGNORE_SUCCESS = "RFQ Ignored Successfully";

	public String RFQ_IGNORE_FAILURE = "Failed To Ignore RFQ";

	public String BUSINESS_EXCEPTION = "Business Level Exception";

	public String RAISED_QUERY_SUCCESS = "Query Raised Successfully";

	public String RAISED_QUERY_FAILED = "Failed To Raise Query";

	public String OTP_GENERATE_SUCCESS = "OTP Generated Successfully";

	public String OTP_GENERATE_FAILED = "Failed To Generate OTP";

	public String OTP_VALID_SUCCESS = "OTP is valid. Email validated successfully.";

	public String OTP_VALID_FAILED = "Invalid OTP. Email validation failed.";

	public String CREATE_SELF_CLIENT = "Thanks for yor interest in procucev services.our associate will confirm/contact you for any query";

	public String SELF_CLIENT_FAILED = "Failed to create";

	public String USER_ACCEPT_SUCCESS = "User Accepted Successfully";

	public String USER_ACCEPT_FAILED = "Failed To Accept User";

	public String RFQ_ACCEPT_SUCCESS = "RFQ Accepted Successfully";

	public String RFQ_ACCEPT_FAILURE = "Failed To Accept RFQ";

	public String RFQ_EDIT_SUCCESS = "RFQ Edited Successfully";

	public String RFQ_EDIT_FAILURE = "Failed To Edit RFQ";

	public String USER_IGNORED_SUCCESS = "Ignored User Successfully";

	public String USER_IGNORED_FAILED = "Failed To Ignore User";

	public String CLIENT_EDIT_SUCCESS = "Client Edited Successfully";

	public String CLIENT_EDIT_FAILED = "Failed To Edit Client";

	public String USER_EDIT_SUCCESS = "User Edited Successfully";

	public String USER_EDIT_FAILED = "Failed To Edit User";

	public String NO_VENDORS_FOUND = "No Vendors Associated To Selected RFQ";

	public String VENDOR_UPGRADE_SUCCESS = "Vendor Upgraded Successfully";

	public String VENDOR_UPGRADE_FAILED = "Failed To Upgrade Vendor";

	public String REQUIRED_FORMAT_CLIENT = "S.No, CompanyName, Name, Pan, Email, Phone, Sector, City, State";

	public String COMPANYNAME = "CompanyName";

	public String NAME = "Name";

	public String PAN = "Pan";

	public String EMAIL = "Email";

	public String PHONE = "Phone";

	public String SECTOR = "Sector";

	public String CITY = "City";

	public String STATE = "State";

	public String USER_DELETE_SUCCESS = "User Deleted Successfully";

	public String USER_DELETE_UNSUCCESS = "Failed To Delete User";

	public String BFS_FAILED = "Failed To Create BFS";

	public String CREATE_BFS = "BFS Created BFS Successfully";

	public String BFS_REQUEST_SUCCESS = "Requested BFS Item Successfully";

	public String BFS_REQUEST_FAILED = "Failed To Request BFS Item";

	public String BFS_Approve_SUCCESS = "BFS Approved Successfully";

	public String BFS_Approve_FAILED = "BFS Failed To Approve";

	public String BFS_REJECT_SUCCESS = "BFS Rejected Successfully";

	public String BFS_REJECT_FAILED = "Failed To Reject BFS";

	public String BFS_ACCEPT_SUCCESS = "BFS Accepted Successfully";

	public String BFS_ACCEPT_FAILED = "BFS Failed To Accept";

	public String REQUIRED_FORMAT_BFS_ITEM_FORMAT = " S.No,  ItemDescription,  Specification,  Uom,  Category,  Quantity,  Location,  AgeOfAsset,  BuyPrice,  Discount,  Remarks,  BFSGroup";

	public String AgeOfAsset = " AgeOfAsset";

	public String SellPrice = " SellPrice";

	public String Discount = " Discount";

	public String EDIT_BFS = "BFS Edited Successfully";

	public String BFS_EDIT_FAILED = "BFS Failed To Edit";

	public String BID_EDIT_SUCCESS = "Bid Edited Successfully";

	public String BID_EDIT_FAILED = "Failed To Edit Bid";

	public String BFS_COMMENT_SUCCESS = "Added Comment Successsfully";

	public String BFS_COMMENT_FAILED = "Failed To Add Comment";

	public String BFSGroup = " BFSGroup";

	public String BuyPrice = " BuyPrice";

	public String ADDED_VISITOR = "Visitor Added Successfully";

	public String ADDED_VISITOR_FAILED = "Failed To Add Vistitor";

	public String REQUIRED_CAPEX_FORMAT = "S.no, Project Item Number, Project Category, Project SubCategory, Description, Specification, Uom, Qty, ItemId";

	public String CREATE_REGION_SUCCESS = "Region Created Successfully";

	public String CREATE_REGION_FAILED = "Failed To Create Region";

	public String S_NO = "S.no";

	public String PROJECT_CATEGORY = "Project Category";

	public String PROJECT_SUBCATEGORY = "Project SubCategory";

	public String Project_Item_Number = "Project Item Number";

	public String UPDATE_PPO = "PPO Updated Successfully";
		
	

}
