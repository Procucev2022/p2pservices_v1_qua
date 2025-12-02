package com.portal.procucev.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.VendorCatalogue;
import com.portal.procucev.model.VendorTermsConditions;
import com.portal.procucev.service.VendorCatalogueService;

@RestController
@RequestMapping("/rest/catalogue")
@CrossOrigin
public class VendorCatalogueController {

	static Logger log = LoggerFactory.getLogger(PartialVendorController.class);
	

	@Autowired
	VendorCatalogueService catalogueService;

	@PostMapping("/saveCatalogue")
	public ResponseEntity<?> saveCatalogue(@RequestBody VendorCatalogue catalogue) {
	    log.info("API Called: /vendor/catalogue/save");

	    try {

	        boolean isUpdate = (catalogue.getId() != null);  // CHECK IF ID EXISTS

	        VendorCatalogue saved = catalogueService.saveCatalogue(catalogue);

	        // Decide message based on ID
	        String message = isUpdate 
	                ? "Catalogue updated successfully"
	                : "Catalogue created successfully";

	        MessageResponse response = new MessageResponse(
	                "200",
	                message,
	                null,
	                new Date(),
	                "Success",
	                null
	        );

	        response.setData(Map.of("catalogueId", saved.getId()));

	        return ResponseEntity.ok(response);

	    } catch (IllegalArgumentException ex) {
	        log.error("Validation failed: {}", ex.getMessage());

	        MessageResponse response = new MessageResponse(
	                "400",
	                "Validation failed",
	                List.of(ex.getMessage()),
	                new Date(),
	                "Failure",
	                "VALIDATION_ERROR"
	        );

	        return ResponseEntity.badRequest().body(response);

	    } catch (Exception ex) {
	        log.error("Unexpected error while saving catalogue", ex);

	        MessageResponse response = new MessageResponse(
	                "500",
	                "Internal server error",
	                List.of(ex.getMessage()),
	                new Date(),
	                "Failure",
	                "SYSTEM_ERROR"
	        );

	        return ResponseEntity.internalServerError().body(response);
	    }
	}


	 @PostMapping("/getCataloguesBySeller")
	   public ResponseEntity<MessageResponse> getCataloguesByVendorId(@RequestBody Organization org) {
	        log.info("API Called: /vendor/catalogue/getByVendor/{}", org.getId());

	        try {
	            List<VendorCatalogue> catalogues = catalogueService.getCataloguesByVendorId(org.getId());

	            if (catalogues.isEmpty()) {
	                MessageResponse response = MessageResponse.error(
	                        "No catalogues found for this vendor",
	                        List.of("CATALOGUE_NOT_FOUND")
	                );
	                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	            }

	            MessageResponse response = MessageResponse.success(
	                    "Catalogues retrieved successfully",
	                    Map.of("catalogues", catalogues)
	            );
	            return ResponseEntity.ok(response);

	        } catch (IllegalArgumentException ex) {
	            log.error("Validation failed: {}", ex.getMessage());

	            MessageResponse response = MessageResponse.error(
	                    "Validation failed",
	                    List.of(ex.getMessage())
	            );
	            return ResponseEntity.badRequest().body(response);

	        } catch (Exception ex) {
	            log.error("Unexpected error while fetching catalogues", ex);

	            MessageResponse response = new MessageResponse(
	                    "500",
	                    "Internal server error",
	                    List.of(ex.getMessage()),
	                    new Date(),
	                    "Failure",
	                    "SYSTEM_ERROR"
	            );
	            return ResponseEntity.internalServerError().body(response);
	        }
	    }
	 
	 
	 @PostMapping("/saveSellerTC")
		public ResponseEntity<?> saveTermsAndConditions(@RequestBody VendorTermsConditions conditions) {
		    log.info("API Called: /vendor/saveSellerTC");

		    try {
		    	VendorTermsConditions saved = catalogueService.saveTermsAndConditions(conditions);

		        MessageResponse response = new MessageResponse(
		                "200",
		                "Terms And Conditions Added successfully",
		                null,
		                new Date(),
		                "Success",
		                null
		        );

		        // Optionally include saved catalogue in response data
		        response.setData(Map.of("catalogueId", saved.getId()));

		        return ResponseEntity.ok(response);

		    } catch (IllegalArgumentException ex) {
		        log.error("Validation failed: {}", ex.getMessage());

		        MessageResponse response = new MessageResponse(
		                "200",
		                "Validation failed",
		                List.of(ex.getMessage()), // errorMsg is List<String>
		                new Date(),
		                "Failure",
		                "VALIDATION_ERROR"
		        );

		        return ResponseEntity.badRequest().body(response);

		    } catch (Exception ex) {
		        log.error("Unexpected error while saving catalogue", ex);

		        MessageResponse response = new MessageResponse(
		                "200",
		                "Internal server error",
		                List.of(ex.getMessage()), // errorMsg as list
		                new Date(),
		                "Failure",
		                "SYSTEM_ERROR"
		        );

		        return ResponseEntity.internalServerError().body(response);
		    }
		}
	 
	 @PostMapping("/getSellerTC")
	   public ResponseEntity<MessageResponse> getSellerTC(@RequestBody Organization org) {
	        log.info("API Called: /vendor/catalogue/getSellerTC/{}", org.getId());

	        try {
	            List<VendorTermsConditions> conditions = catalogueService.getTCByVendorId(org.getId());

	            if (conditions.isEmpty()) {
	                MessageResponse response = MessageResponse.error(
	                        "No Terms and Conditions found for this vendor",
	                        List.of("CATALOGUE_NOT_FOUND")
	                );
	                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	            }

	            MessageResponse response = MessageResponse.success(
	                    "Catalogues retrieved successfully",
	                    Map.of("catalogues", conditions)
	            );
	            return ResponseEntity.ok(response);

	        } catch (IllegalArgumentException ex) {
	            log.error("Validation failed: {}", ex.getMessage());

	            MessageResponse response = MessageResponse.error(
	                    "Validation failed",
	                    List.of(ex.getMessage())
	            );
	            return ResponseEntity.badRequest().body(response);

	        } catch (Exception ex) {
	            log.error("Unexpected error while fetching conditions", ex);

	            MessageResponse response = new MessageResponse(
	                    "500",
	                    "Internal server error",
	                    List.of(ex.getMessage()),
	                    new Date(),
	                    "Failure",
	                    "SYSTEM_ERROR"
	            );
	            return ResponseEntity.internalServerError().body(response);
	        }
	    }
	 
}

